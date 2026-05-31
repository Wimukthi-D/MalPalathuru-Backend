package org.wimukthi.malpalathurubackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wimukthi.malpalathurubackend.dto.LeaderboardResponse;
import org.wimukthi.malpalathurubackend.entity.*;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;
import org.wimukthi.malpalathurubackend.enums.ReviewDecision;
import org.wimukthi.malpalathurubackend.repository.AnswerRepository;
import org.wimukthi.malpalathurubackend.repository.AnswerReviewRepository;
import org.wimukthi.malpalathurubackend.repository.PlayerRepository;
import org.wimukthi.malpalathurubackend.repository.ScoreRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final AnswerRepository answerRepository;
    private final AnswerReviewRepository answerReviewRepository;
    private final PlayerRepository playerRepository;
    private final ScoreRepository scoreRepository;
    private final RoomMapper roomMapper;

    public ScoreService(
            AnswerRepository answerRepository,
            AnswerReviewRepository answerReviewRepository,
            PlayerRepository playerRepository,
            ScoreRepository scoreRepository,
            RoomMapper roomMapper
    ) {
        this.answerRepository = answerRepository;
        this.answerReviewRepository = answerReviewRepository;
        this.playerRepository = playerRepository;
        this.scoreRepository = scoreRepository;
        this.roomMapper = roomMapper;
    }

    @Transactional
    public LeaderboardResponse calculateRoundScores(Room room, Round round, boolean finalLeaderboard) {
        scoreRepository.deleteByRound(round);

        List<Player> players = playerRepository.findByRoom(room);
        List<Answer> answers = answerRepository.findByRound(round);

        for (AnswerCategory category : AnswerCategory.values()) {
            Map<String, List<Answer>> acceptedAnswersByNormalizedValue = answers.stream()
                    .filter(answer -> answer.getCategory() == category)
                    .filter(this::isAccepted)
                    .filter(answer -> !GameTextNormalizer.sanitizeAnswer(answer.getAnswerText()).isBlank())
                    .collect(Collectors.groupingBy(answer ->
                            GameTextNormalizer.normalizeAnswer(room.getLanguage(), answer.getAnswerText())
                    ));

            for (Player player : players) {
                Answer answer = answers.stream()
                        .filter(candidate -> candidate.getCategory() == category)
                        .filter(candidate -> Objects.equals(candidate.getPlayer().getId(), player.getId()))
                        .findFirst()
                        .orElse(null);

                int points = 0;
                if (answer != null && isAccepted(answer) && !GameTextNormalizer.sanitizeAnswer(answer.getAnswerText()).isBlank()) {
                    String normalized = GameTextNormalizer.normalizeAnswer(room.getLanguage(), answer.getAnswerText());
                    int duplicateCount = acceptedAnswersByNormalizedValue.getOrDefault(normalized, List.of()).size();
                    points = duplicateCount > 1 ? 5 : 10;
                }

                Score score = new Score();
                score.setRound(round);
                score.setPlayer(player);
                score.setCategory(category);
                score.setPoints(points);
                scoreRepository.save(score);
            }
        }

        for (Player player : players) {
            Long totalScore = scoreRepository.sumPointsByPlayer(player);
            player.setTotalScore(totalScore == null ? 0 : totalScore.intValue());
            playerRepository.save(player);
        }

        return roomMapper.toLeaderboard(room, round, finalLeaderboard);
    }

    private boolean isAccepted(Answer answer) {
        return answerReviewRepository.findByAnswer(answer)
                .map(review -> review.getDecision() == ReviewDecision.ACCEPTED)
                .orElse(false);
    }
}
