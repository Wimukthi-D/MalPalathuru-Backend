package org.wimukthi.malpalathurubackend.service;

import org.springframework.stereotype.Service;
import org.wimukthi.malpalathurubackend.dto.*;
import org.wimukthi.malpalathurubackend.entity.*;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;
import org.wimukthi.malpalathurubackend.enums.VoteValue;
import org.wimukthi.malpalathurubackend.repository.*;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomMapper {

    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final UsedLetterRepository usedLetterRepository;
    private final AnswerRepository answerRepository;
    private final AnswerReviewRepository answerReviewRepository;
    private final VoteRepository voteRepository;
    private final ScoreRepository scoreRepository;

    public RoomMapper(
            PlayerRepository playerRepository,
            RoundRepository roundRepository,
            UsedLetterRepository usedLetterRepository,
            AnswerRepository answerRepository,
            AnswerReviewRepository answerReviewRepository,
            VoteRepository voteRepository,
            ScoreRepository scoreRepository
    ) {
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
        this.usedLetterRepository = usedLetterRepository;
        this.answerRepository = answerRepository;
        this.answerReviewRepository = answerReviewRepository;
        this.voteRepository = voteRepository;
        this.scoreRepository = scoreRepository;
    }

    public RoomResponse toRoomResponse(Room room) {
        List<Player> players = sortedPlayers(playerRepository.findByRoom(room));
        List<PlayerResponse> playerResponses = players.stream()
                .map(this::toPlayerResponse)
                .toList();

        List<Round> rounds = roundRepository.findByRoomOrderByRoundNumberAsc(room);
        List<RoundSummaryResponse> roundResponses = rounds.stream()
                .map(this::toRoundSummaryResponse)
                .toList();

        RoundResponse currentRound = room.getCurrentRoundNumber() == null
                ? null
                : rounds.stream()
                .filter(round -> room.getCurrentRoundNumber().equals(round.getRoundNumber()))
                .findFirst()
                .map(this::toRoundResponse)
                .orElse(null);

        List<String> usedLetters = usedLetterRepository.findByRoomOrderByCreatedAtAsc(room).stream()
                .map(UsedLetter::getDisplayLetter)
                .toList();

        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getLanguage(),
                room.getMaxPlayers(),
                room.getCountLimitSeconds(),
                room.getPrivateRoom(),
                room.getLocked(),
                room.getStatus(),
                playerResponses,
                room.getTotalRounds(),
                room.getCurrentRoundNumber(),
                currentRound,
                roundResponses,
                usedLetters
        );
    }

    public PlayerResponse toPlayerResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getPlayerName(),
                player.getHost(),
                player.getReady(),
                player.getConnected(),
                player.getTotalScore()
        );
    }

    public RoundSummaryResponse toRoundSummaryResponse(Round round) {
        return new RoundSummaryResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                round.getSelectedLetter(),
                toPlayerResponse(round.getSuggester())
        );
    }

    public RoundResponse toRoundResponse(Round round) {
        Map<Long, Score> scoreByAnswerKey = scoreRepository.findByRound(round).stream()
                .collect(Collectors.toMap(
                        score -> answerKey(score.getPlayer().getId(), score.getCategory()),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<AnswerResponse> answers = sortedAnswers(answerRepository.findByRound(round)).stream()
                .map(answer -> toAnswerResponse(answer, scoreByAnswerKey))
                .toList();

        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                round.getSelectedLetter(),
                toPlayerResponse(round.getSuggester()),
                round.getCountdownStartedAt(),
                round.getLockedAt(),
                round.getReviewStartedAt(),
                round.getScoreCalculatedAt(),
                answers
        );
    }

    public AnswerResponse toAnswerResponse(Answer answer, Map<Long, Score> scoreByAnswerKey) {
        Score score = scoreByAnswerKey.get(answerKey(answer.getPlayer().getId(), answer.getCategory()));
        return new AnswerResponse(
                answer.getId(),
                toPlayerResponse(answer.getPlayer()),
                answer.getCategory(),
                answer.getAnswerText(),
                answer.getAutoSubmitted(),
                answer.getSubmittedAt(),
                toReviewResponse(answer),
                score == null ? null : score.getPoints()
        );
    }

    public ReviewResponse toReviewResponse(Answer answer) {
        AnswerReview review = answerReviewRepository.findByAnswer(answer).orElse(null);
        List<VoteResponse> votes = voteRepository.findByAnswer(answer).stream()
                .sorted(Comparator.comparing(vote -> vote.getVoter().getJoinedAt()))
                .map(this::toVoteResponse)
                .toList();

        return new ReviewResponse(
                review == null ? null : review.getId(),
                review == null ? null : review.getDecision(),
                review == null || review.getDecidedByHost() == null ? null : review.getDecidedByHost().getId(),
                review == null ? null : review.getDecidedAt(),
                voteRepository.countByAnswerAndValue(answer, VoteValue.ACCEPT),
                voteRepository.countByAnswerAndValue(answer, VoteValue.REJECT),
                votes
        );
    }

    public VoteResponse toVoteResponse(Vote vote) {
        return new VoteResponse(
                vote.getVoter().getId(),
                vote.getVoter().getPlayerName(),
                vote.getValue(),
                vote.getVotedAt()
        );
    }

    public ReviewStateResponse toReviewState(Room room, Round round) {
        List<ReviewItemResponse> items = sortedAnswers(answerRepository.findByRound(round)).stream()
                .map(this::toReviewItem)
                .toList();

        ReviewItemResponse currentItem = items.stream()
                .filter(item -> item.decision() == null)
                .findFirst()
                .orElse(null);

        return new ReviewStateResponse(
                room.getRoomCode(),
                round.getId(),
                round.getRoundNumber(),
                currentItem,
                items
        );
    }

    public ReviewItemResponse toReviewItem(Answer answer) {
        ReviewResponse review = toReviewResponse(answer);
        return new ReviewItemResponse(
                answer.getId(),
                answer.getCategory(),
                toPlayerResponse(answer.getPlayer()),
                answer.getAnswerText(),
                review.decision(),
                review.acceptVotes(),
                review.rejectVotes(),
                review.votes()
        );
    }

    public LeaderboardResponse toLeaderboard(Room room, Round round, boolean finalLeaderboard) {
        List<Player> players = sortedPlayers(playerRepository.findByRoom(room));
        List<Score> scores = round == null ? List.of() : scoreRepository.findByRound(round);

        List<LeaderboardEntryResponse> entries = players.stream()
                .map(player -> toLeaderboardEntry(player, scores))
                .sorted(Comparator
                        .comparing(LeaderboardEntryResponse::totalScore, Comparator.reverseOrder())
                        .thenComparing(entry -> entry.player().playerName()))
                .toList();

        return new LeaderboardResponse(
                room.getRoomCode(),
                round == null ? null : round.getId(),
                round == null ? null : round.getRoundNumber(),
                finalLeaderboard,
                entries
        );
    }

    private LeaderboardEntryResponse toLeaderboardEntry(Player player, List<Score> roundScores) {
        Map<AnswerCategory, Integer> categoryScores = new EnumMap<>(AnswerCategory.class);
        for (AnswerCategory category : AnswerCategory.values()) {
            categoryScores.put(category, 0);
        }

        int roundScore = 0;
        for (Score score : roundScores) {
            if (score.getPlayer().getId().equals(player.getId())) {
                categoryScores.put(score.getCategory(), score.getPoints());
                roundScore += score.getPoints();
            }
        }

        return new LeaderboardEntryResponse(
                toPlayerResponse(player),
                roundScore,
                player.getTotalScore(),
                categoryScores
        );
    }

    private List<Player> sortedPlayers(List<Player> players) {
        return players.stream()
                .sorted(Comparator.comparing(Player::getJoinedAt))
                .toList();
    }

    private List<Answer> sortedAnswers(List<Answer> answers) {
        return answers.stream()
                .sorted(Comparator
                        .comparingInt((Answer answer) -> answer.getCategory().ordinal())
                        .thenComparing(answer -> answer.getPlayer().getJoinedAt()))
                .toList();
    }

    private Long answerKey(Long playerId, AnswerCategory category) {
        return playerId * 100 + category.ordinal();
    }
}
