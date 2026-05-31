package org.wimukthi.malpalathurubackend;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wimukthi.malpalathurubackend.repository.*;
import org.wimukthi.malpalathurubackend.service.RoundTimerService;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class MalPalathuruBackendApplicationTests {

    @MockitoBean
    private RoomRepository roomRepository;

    @MockitoBean
    private PlayerRepository playerRepository;

    @MockitoBean
    private RoundRepository roundRepository;

    @MockitoBean
    private UsedLetterRepository usedLetterRepository;

    @MockitoBean
    private AnswerRepository answerRepository;

    @MockitoBean
    private AnswerReviewRepository answerReviewRepository;

    @MockitoBean
    private VoteRepository voteRepository;

    @MockitoBean
    private ScoreRepository scoreRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private RoundTimerService roundTimerService;

    @Test
    void contextLoads() {
    }

}
