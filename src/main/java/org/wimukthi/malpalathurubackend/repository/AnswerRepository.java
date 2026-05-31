package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wimukthi.malpalathurubackend.entity.Answer;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Round;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByRound(Round round);

    List<Answer> findByRoundOrderByCategoryAscPlayerJoinedAtAsc(Round round);

    List<Answer> findByRoundAndCategory(Round round, AnswerCategory category);

    List<Answer> findByRoundAndPlayer(Round round, Player player);

    Optional<Answer> findByRoundAndPlayerAndCategory(Round round, Player player, AnswerCategory category);
}
