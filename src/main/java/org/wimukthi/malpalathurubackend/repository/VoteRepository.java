package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wimukthi.malpalathurubackend.entity.Answer;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Vote;
import org.wimukthi.malpalathurubackend.enums.VoteValue;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByAnswerAndVoter(Answer answer, Player voter);

    List<Vote> findByAnswer(Answer answer);

    long countByAnswerAndValue(Answer answer, VoteValue value);
}
