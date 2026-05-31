package org.wimukthi.malpalathurubackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wimukthi.malpalathurubackend.entity.Round;
import org.wimukthi.malpalathurubackend.repository.RoundRepository;

import java.time.LocalDateTime;

@Service
public class RoundTimerService {

    private static final Logger log = LoggerFactory.getLogger(RoundTimerService.class);

    private final RoundRepository roundRepository;
    private final GameService gameService;

    public RoundTimerService(RoundRepository roundRepository, GameService gameService) {
        this.roundRepository = roundRepository;
        this.gameService = gameService;
    }

    @Scheduled(fixedDelay = 1000)
    public void lockExpiredCountdownRounds() {
        // The database stores the countdown start time; this small poller is the server-side
        // source of truth and locks expired rounds even if no frontend sends another request.
        for (Round round : roundRepository.findByCountdownStartedAtIsNotNullAndLockedAtIsNull()) {
            LocalDateTime expiresAt = round.getCountdownStartedAt().plusSeconds(round.getRoom().getCountLimitSeconds());
            if (LocalDateTime.now().isBefore(expiresAt)) {
                continue;
            }

            try {
                boolean locked = gameService.lockExpiredRound(round.getId());
                if (locked) {
                    log.info("Locked expired round {} in room {}", round.getId(), round.getRoom().getRoomCode());
                }
            } catch (RuntimeException ex) {
                log.warn("Could not lock expired round {}", round.getId(), ex);
            }
        }
    }
}
