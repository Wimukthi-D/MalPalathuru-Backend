package org.wimukthi.malpalathurubackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wimukthi.malpalathurubackend.dto.*;
import org.wimukthi.malpalathurubackend.entity.*;
import org.wimukthi.malpalathurubackend.enums.*;
import org.wimukthi.malpalathurubackend.exception.ResourceNotFoundException;
import org.wimukthi.malpalathurubackend.repository.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GameService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final UsedLetterRepository usedLetterRepository;
    private final AnswerRepository answerRepository;
    private final AnswerReviewRepository answerReviewRepository;
    private final VoteRepository voteRepository;
    private final ScoreService scoreService;
    private final RoomMapper roomMapper;
    private final RoomEventPublisher eventPublisher;

    public GameService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            RoundRepository roundRepository,
            UsedLetterRepository usedLetterRepository,
            AnswerRepository answerRepository,
            AnswerReviewRepository answerReviewRepository,
            VoteRepository voteRepository,
            ScoreService scoreService,
            RoomMapper roomMapper,
            RoomEventPublisher eventPublisher
    ) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
        this.usedLetterRepository = usedLetterRepository;
        this.answerRepository = answerRepository;
        this.answerReviewRepository = answerReviewRepository;
        this.voteRepository = voteRepository;
        this.scoreService = scoreService;
        this.roomMapper = roomMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RoomResponse selectLetter(String roomCode, Long roundId, SelectLetterRequest request) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);

        if (room.getStatus() != RoomStatus.LETTER_SELECTION || round.getStatus() != RoundStatus.WAITING_FOR_LETTER) {
            throw new IllegalStateException("Round is not waiting for a letter");
        }

        Player player = getPlayerInRoom(room, request.playerId());
        if (!Objects.equals(round.getSuggester().getId(), player.getId())) {
            throw new IllegalStateException("Only the selected suggester can choose the letter");
        }

        String displayLetter = GameTextNormalizer.displayLetter(room.getLanguage(), request.letter());
        String normalizedLetter = GameTextNormalizer.normalizeLetter(room.getLanguage(), request.letter());
        if (displayLetter.isBlank()) {
            throw new IllegalStateException("Letter is required");
        }
        if (displayLetter.length() > 16) {
            throw new IllegalStateException("Letter is too long");
        }
        if (round.getSelectedLetter() != null || usedLetterRepository.existsByRoomAndNormalizedLetter(room, normalizedLetter)) {
            throw new IllegalStateException("Letter has already been used");
        }

        LocalDateTime now = LocalDateTime.now();
        round.setSelectedLetter(displayLetter);
        round.setSelectedLetterNormalized(normalizedLetter);
        round.setLetterSelectedAt(now);
        round.setStatus(RoundStatus.PLAYING);
        room.setStatus(RoomStatus.PLAYING);

        UsedLetter usedLetter = new UsedLetter();
        usedLetter.setRoom(room);
        usedLetter.setRound(round);
        usedLetter.setDisplayLetter(displayLetter);
        usedLetter.setNormalizedLetter(normalizedLetter);
        usedLetter.setCreatedAt(now);

        usedLetterRepository.save(usedLetter);
        roundRepository.save(round);
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "LETTER_SELECTED", "Letter has been selected", player.getId(), response);
        eventPublisher.broadcastState(room.getRoomCode(), "ROUND_STARTED", "Round has started", null, response);
        return response;
    }

    @Transactional
    public RoomResponse submitAnswers(String roomCode, Long roundId, SubmitAnswersRequest request) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        Player player = getPlayerInRoom(room, request.playerId());

        if (round.getLockedAt() != null) {
            throw new IllegalStateException("Round is locked");
        }
        if (round.getSelectedLetter() == null) {
            throw new IllegalStateException("Letter has not been selected");
        }
        if (round.getStatus() != RoundStatus.PLAYING && round.getStatus() != RoundStatus.COUNTDOWN_STARTED) {
            throw new IllegalStateException("Round is not accepting answers");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<AnswerCategory, String> submittedAnswers = answerMap(request);
        for (Map.Entry<AnswerCategory, String> entry : submittedAnswers.entrySet()) {
            Answer answer = answerRepository.findByRoundAndPlayerAndCategory(round, player, entry.getKey())
                    .orElseGet(() -> newAnswer(round, player, entry.getKey(), now, false));
            answer.setAnswerText(GameTextNormalizer.sanitizeAnswer(entry.getValue()));
            answer.setAutoSubmitted(false);
            answer.setSubmittedAt(now);
            answerRepository.save(answer);
        }

        boolean countdownStarted = false;
        if (round.getCountdownStartedAt() == null) {
            countdownStarted = true;
            round.setCountdownStartedAt(now);
            round.setStatus(RoundStatus.COUNTDOWN_STARTED);
            room.setStatus(RoomStatus.COUNTDOWN_STARTED);
            roundRepository.save(round);
            roomRepository.save(room);
        }

        RoomResponse response = roomMapper.toRoomResponse(room);
        if (countdownStarted) {
            TimerResponse timer = timerResponse(room, round);
            eventPublisher.broadcastTimer(room.getRoomCode(), "COUNTDOWN_STARTED", "Countdown has started", response, timer);
            eventPublisher.broadcastState(room.getRoomCode(), "COUNTDOWN_STARTED", "Countdown has started", player.getId(), response, timer);
        }

        eventPublisher.broadcastState(room.getRoomCode(), "ANSWER_SUBMITTED", "Player submitted answers", player.getId(), response);
        return response;
    }

    @Transactional
    public RoomResponse lockRound(String roomCode, Long roundId, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);

        if (hostPlayerId == null && !isCountdownExpired(room, round)) {
            throw new IllegalStateException("Countdown has not expired");
        }
        if (hostPlayerId != null) {
            validateHost(room, hostPlayerId);
        }

        return lockRoundInternal(room, round);
    }

    @Transactional
    public boolean lockExpiredRound(Long roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null || round.getLockedAt() != null || round.getCountdownStartedAt() == null) {
            return false;
        }

        Room room = round.getRoom();
        if (room.getStatus() == RoomStatus.CLOSED || !isCountdownExpired(room, round)) {
            return false;
        }

        lockRoundInternal(room, round);
        return true;
    }

    @Transactional
    public ReviewStateResponse startReview(String roomCode, Long roundId, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        Player host = validateHost(room, hostPlayerId);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        if (round.getLockedAt() == null) {
            throw new IllegalStateException("Round must be locked before review starts");
        }

        ensureAllAnswersExist(room, round);
        ensureReviewsExist(round);

        round.setStatus(RoundStatus.REVIEWING);
        round.setReviewStartedAt(LocalDateTime.now());
        room.setStatus(RoomStatus.REVIEWING);
        roundRepository.save(round);
        roomRepository.save(room);

        RoomResponse roomResponse = roomMapper.toRoomResponse(room);
        ReviewStateResponse reviewState = roomMapper.toReviewState(room, round);
        eventPublisher.broadcastState(room.getRoomCode(), "REVIEW_STARTED", "Review has started", host.getId(), roomResponse);
        eventPublisher.broadcastReview(room.getRoomCode(), "REVIEW_STARTED", "Review has started", host.getId(), roomResponse, reviewState);
        if (reviewState.currentItem() != null) {
            eventPublisher.broadcastReview(room.getRoomCode(), "REVIEW_ITEM_CHANGED", "Current review item changed", host.getId(), roomResponse, reviewState.currentItem());
        }
        return reviewState;
    }

    @Transactional
    public ReviewStateResponse vote(String roomCode, Long roundId, ReviewVoteRequest request) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        validateReviewing(round);

        Player voter = getPlayerInRoom(room, request.playerId());
        Answer answer = getAnswerInRound(round, request.answerId());

        Vote vote = voteRepository.findByAnswerAndVoter(answer, voter).orElseGet(Vote::new);
        vote.setAnswer(answer);
        vote.setVoter(voter);
        vote.setValue(request.vote());
        vote.setVotedAt(LocalDateTime.now());
        voteRepository.save(vote);

        ReviewStateResponse reviewState = roomMapper.toReviewState(room, round);
        RoomResponse roomResponse = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastReview(room.getRoomCode(), "VOTE_UPDATED", "Vote has been updated", voter.getId(), roomResponse, reviewState);
        return reviewState;
    }

    @Transactional
    public ReviewStateResponse decideAnswer(String roomCode, Long roundId, Long hostPlayerId, ReviewDecisionRequest request) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        Player host = validateHost(room, hostPlayerId);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        validateReviewing(round);

        Answer answer = getAnswerInRound(round, request.answerId());
        AnswerReview review = getOrCreateReview(answer);
        review.setDecision(request.decision());
        review.setDecidedByHost(host);
        review.setDecidedAt(LocalDateTime.now());
        answerReviewRepository.save(review);

        ReviewStateResponse reviewState = roomMapper.toReviewState(room, round);
        RoomResponse roomResponse = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastReview(room.getRoomCode(), "REVIEW_DECISION_UPDATED", "Review decision has been updated", host.getId(), roomResponse, reviewState);
        eventPublisher.broadcastReview(room.getRoomCode(), "REVIEW_ITEM_CHANGED", "Current review item changed", host.getId(), roomResponse, reviewState.currentItem());
        return reviewState;
    }

    @Transactional
    public LeaderboardResponse completeReview(String roomCode, Long roundId, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        Player host = validateHost(room, hostPlayerId);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        if (round.getStatus() != RoundStatus.REVIEWING) {
            throw new IllegalStateException("Round is not in review");
        }

        finalizePendingReviewDecisions(round, host);

        round.setStatus(RoundStatus.SCORE_READY);
        round.setScoreCalculatedAt(LocalDateTime.now());
        room.setStatus(RoomStatus.ROUND_LEADERBOARD);
        roundRepository.save(round);
        roomRepository.save(room);

        LeaderboardResponse leaderboard = scoreService.calculateRoundScores(room, round, false);
        RoomResponse roomResponse = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "ROUND_SCORE_READY", "Round score is ready", host.getId(), roomResponse, leaderboard);
        eventPublisher.broadcastLeaderboard(room.getRoomCode(), "ROUND_SCORE_READY", "Round leaderboard is ready", roomResponse, leaderboard);
        return leaderboard;
    }

    @Transactional
    public GameAdvanceResponse nextRound(String roomCode, Long roundId, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        Player host = validateHost(room, hostPlayerId);

        Round round = getRoundInRoom(room, roundId);
        validateCurrentRound(room, round);
        if (round.getStatus() != RoundStatus.SCORE_READY && round.getStatus() != RoundStatus.COMPLETED) {
            throw new IllegalStateException("Round score must be ready before moving on");
        }

        round.setStatus(RoundStatus.COMPLETED);
        roundRepository.save(round);

        int nextRoundNumber = round.getRoundNumber() + 1;
        Round nextRound = roundRepository.findByRoomAndRoundNumber(room, nextRoundNumber).orElse(null);
        if (nextRound == null) {
            room.setStatus(RoomStatus.FINISHED);
            roomRepository.save(room);

            LeaderboardResponse leaderboard = roomMapper.toLeaderboard(room, null, true);
            RoomResponse roomResponse = roomMapper.toRoomResponse(room);
            eventPublisher.broadcastState(room.getRoomCode(), "FINAL_LEADERBOARD_READY", "Final leaderboard is ready", host.getId(), roomResponse, leaderboard);
            eventPublisher.broadcastLeaderboard(room.getRoomCode(), "FINAL_LEADERBOARD_READY", "Final leaderboard is ready", roomResponse, leaderboard);
            return new GameAdvanceResponse(true, roomResponse, leaderboard);
        }

        room.setCurrentRoundNumber(nextRoundNumber);
        room.setStatus(RoomStatus.LETTER_SELECTION);
        nextRound.setStatus(RoundStatus.WAITING_FOR_LETTER);
        roundRepository.save(nextRound);
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "NEXT_ROUND_STARTED", "Next round has started", host.getId(), response);
        return new GameAdvanceResponse(false, response, null);
    }

    private RoomResponse lockRoundInternal(Room room, Round round) {
        if (round.getLockedAt() != null) {
            return roomMapper.toRoomResponse(room);
        }
        if (round.getSelectedLetter() == null) {
            throw new IllegalStateException("Letter has not been selected");
        }

        ensureAllAnswersExist(room, round);

        round.setLockedAt(LocalDateTime.now());
        round.setStatus(RoundStatus.ANSWER_LOCKED);
        room.setStatus(RoomStatus.ANSWER_LOCKED);
        roundRepository.save(round);
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "ROUND_LOCKED", "Round has been locked", null, response);
        eventPublisher.broadcastTimer(room.getRoomCode(), "ROUND_LOCKED", "Round has been locked", response, timerResponse(room, round));
        return response;
    }

    private void ensureAllAnswersExist(Room room, Round round) {
        LocalDateTime now = LocalDateTime.now();
        List<Player> players = playerRepository.findByRoom(room);
        for (Player player : players) {
            for (AnswerCategory category : AnswerCategory.values()) {
                answerRepository.findByRoundAndPlayerAndCategory(round, player, category)
                        .orElseGet(() -> answerRepository.save(newAnswer(round, player, category, now, true)));
            }
        }
    }

    private void ensureReviewsExist(Round round) {
        answerRepository.findByRound(round).forEach(this::getOrCreateReview);
    }

    private void finalizePendingReviewDecisions(Round round, Player host) {
        for (Answer answer : answerRepository.findByRound(round)) {
            AnswerReview review = getOrCreateReview(answer);
            if (review.getDecision() != null) {
                continue;
            }

            boolean empty = GameTextNormalizer.sanitizeAnswer(answer.getAnswerText()).isBlank();
            long acceptVotes = voteRepository.countByAnswerAndValue(answer, VoteValue.ACCEPT);
            long rejectVotes = voteRepository.countByAnswerAndValue(answer, VoteValue.REJECT);

            review.setDecision(!empty && acceptVotes >= rejectVotes ? ReviewDecision.ACCEPTED : ReviewDecision.REJECTED);
            review.setDecidedByHost(host);
            review.setDecidedAt(LocalDateTime.now());
            answerReviewRepository.save(review);
        }
    }

    private AnswerReview getOrCreateReview(Answer answer) {
        return answerReviewRepository.findByAnswer(answer).orElseGet(() -> {
            AnswerReview review = new AnswerReview();
            review.setAnswer(answer);
            return answerReviewRepository.save(review);
        });
    }

    private Answer newAnswer(Round round, Player player, AnswerCategory category, LocalDateTime submittedAt, boolean autoSubmitted) {
        Answer answer = new Answer();
        answer.setRound(round);
        answer.setPlayer(player);
        answer.setCategory(category);
        answer.setAnswerText("");
        answer.setAutoSubmitted(autoSubmitted);
        answer.setSubmittedAt(submittedAt);
        return answer;
    }

    private Map<AnswerCategory, String> answerMap(SubmitAnswersRequest request) {
        Map<AnswerCategory, String> answers = new EnumMap<>(AnswerCategory.class);
        answers.put(AnswerCategory.FEMALE, request.female());
        answers.put(AnswerCategory.MALE, request.male());
        answers.put(AnswerCategory.FLOWER, request.flower());
        answers.put(AnswerCategory.FRUIT, request.fruit());
        answers.put(AnswerCategory.ANIMAL, request.animal());
        answers.put(AnswerCategory.CITY, request.city());
        return answers;
    }

    private TimerResponse timerResponse(Room room, Round round) {
        LocalDateTime startedAt = round.getCountdownStartedAt();
        return new TimerResponse(
                room.getRoomCode(),
                round.getId(),
                startedAt,
                startedAt == null ? null : startedAt.plusSeconds(room.getCountLimitSeconds()),
                room.getCountLimitSeconds()
        );
    }

    private boolean isCountdownExpired(Room room, Round round) {
        return round.getCountdownStartedAt() != null
                && !LocalDateTime.now().isBefore(round.getCountdownStartedAt().plusSeconds(room.getCountLimitSeconds()));
    }

    private Room getRoomEntityByCode(String roomCode) {
        return roomRepository.findByRoomCode(GameTextNormalizer.normalizeRoomCode(roomCode))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    private Round getRoundInRoom(Room room, Long roundId) {
        return roundRepository.findByIdAndRoom(roundId, room)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found in this room"));
    }

    private Answer getAnswerInRound(Round round, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));
        if (!Objects.equals(answer.getRound().getId(), round.getId())) {
            throw new ResourceNotFoundException("Answer not found in this round");
        }
        return answer;
    }

    private Player getPlayerInRoom(Room room, Long playerId) {
        return playerRepository.findByIdAndRoom(playerId, room)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this room"));
    }

    private Player validateHost(Room room, Long hostPlayerId) {
        Player hostPlayer = getPlayerInRoom(room, hostPlayerId);
        if (!Boolean.TRUE.equals(hostPlayer.getHost())) {
            throw new IllegalStateException("Only host can perform this action");
        }
        return hostPlayer;
    }

    private void validateCurrentRound(Room room, Round round) {
        if (!Objects.equals(room.getCurrentRoundNumber(), round.getRoundNumber())) {
            throw new IllegalStateException("This is not the current round");
        }
    }

    private void validateReviewing(Round round) {
        if (round.getStatus() != RoundStatus.REVIEWING) {
            throw new IllegalStateException("Round is not in review");
        }
    }

    private void validateRoomOpen(Room room) {
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new IllegalStateException("Room is closed");
        }
    }
}
