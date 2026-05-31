package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Round;
import org.wimukthi.malpalathurubackend.entity.Score;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByRound(Round round);

    List<Score> findByRoundAndPlayer(Round round, Player player);

    void deleteByRound(Round round);

    @Query("select coalesce(sum(s.points), 0) from Score s where s.player = :player")
    Long sumPointsByPlayer(@Param("player") Player player);
}
