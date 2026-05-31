package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wimukthi.malpalathurubackend.entity.Answer;
import org.wimukthi.malpalathurubackend.entity.AnswerReview;

import java.util.Optional;

public interface AnswerReviewRepository extends JpaRepository<AnswerReview, Long> {

    Optional<AnswerReview> findByAnswer(Answer answer);
}
