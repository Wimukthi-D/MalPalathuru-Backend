package org.wimukthi.malpalathurubackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wimukthi.malpalathurubackend.dto.LeaderboardResponse;
import org.wimukthi.malpalathurubackend.entity.*;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;
import org.wimukthi.malpalathurubackend.enums.Language;
import org.wimukthi.malpalathurubackend.enums.ReviewDecision;
import org.wimukthi.malpalathurubackend.repository.AnswerRepository;
import org.wimukthi.malpalathurubackend.repository.AnswerReviewRepository;
import org.wimukthi.malpalathurubackend.repository.PlayerRepository;
import org.wimukthi.malpalathurubackend.repository.ScoreRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerReviewRepository answerReviewRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ScoreRepository scoreRepository;

    @Mock
    private RoomMapper roomMapper;

    @Test
    void englishAcceptedDuplicateAnswersScoreFiveAndUniqueScoresTen() {
        Room room = room();
        Round round = round(room);
        Player first = player(1L, "First", room);
        Player second = player(2L, "Second", room);
        Player third = player(3L, "Third", room);

        Answer firstAnswer = answer(round, first, "Apple");
        Answer secondAnswer = answer(round, second, " apple ");
        Answer thirdAnswer = answer(round, third, "Pear");

        when(playerRepository.findByRoom(room)).thenReturn(List.of(first, second, third));
        when(answerRepository.findByRound(round)).thenReturn(List.of(firstAnswer, secondAnswer, thirdAnswer));
        when(answerReviewRepository.findByAnswer(any(Answer.class))).thenAnswer(invocation -> {
            AnswerReview review = new AnswerReview();
            review.setAnswer(invocation.getArgument(0));
            review.setDecision(ReviewDecision.ACCEPTED);
            return Optional.of(review);
        });
        when(scoreRepository.sumPointsByPlayer(any(Player.class))).thenReturn(0L);
        when(roomMapper.toLeaderboard(room, round, false)).thenReturn(new LeaderboardResponse("ABC123", round.getId(), 1, false, List.of()));

        ScoreService scoreService = new ScoreService(answerRepository, answerReviewRepository, playerRepository, scoreRepository, roomMapper);
        scoreService.calculateRoundScores(room, round, false);

        ArgumentCaptor<Score> scoreCaptor = ArgumentCaptor.forClass(Score.class);
        org.mockito.Mockito.verify(scoreRepository, org.mockito.Mockito.times(18)).save(scoreCaptor.capture());

        Map<Long, Integer> fruitScores = scoreCaptor.getAllValues().stream()
                .filter(score -> score.getCategory() == AnswerCategory.FRUIT)
                .collect(Collectors.toMap(score -> score.getPlayer().getId(), Score::getPoints));

        assertThat(fruitScores).containsEntry(1L, 5);
        assertThat(fruitScores).containsEntry(2L, 5);
        assertThat(fruitScores).containsEntry(3L, 10);
    }

    private Room room() {
        Room room = new Room();
        room.setRoomCode("ABC123");
        room.setLanguage(Language.ENGLISH);
        room.setMaxPlayers(3);
        room.setCountLimitSeconds(30);
        room.setPrivateRoom(false);
        room.setLocked(true);
        return room;
    }

    private Round round(Room room) {
        Round round = new Round();
        setId(round, 10L);
        round.setRoom(room);
        round.setRoundNumber(1);
        round.setCreatedAt(LocalDateTime.now());
        return round;
    }

    private Player player(Long id, String name, Room room) {
        Player player = new Player();
        setId(player, id);
        player.setPlayerName(name);
        player.setHost(false);
        player.setReady(false);
        player.setConnected(true);
        player.setTotalScore(0);
        player.setJoinedAt(LocalDateTime.now());
        player.setRoom(room);
        return player;
    }

    private Answer answer(Round round, Player player, String text) {
        Answer answer = new Answer();
        answer.setRound(round);
        answer.setPlayer(player);
        answer.setCategory(AnswerCategory.FRUIT);
        answer.setAnswerText(text);
        answer.setAutoSubmitted(false);
        answer.setSubmittedAt(LocalDateTime.now());
        return answer;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not set id for test", ex);
        }
    }
}
