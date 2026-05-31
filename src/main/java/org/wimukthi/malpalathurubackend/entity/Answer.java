package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_answer_round_player_category", columnNames = {"round_id", "player_id", "category"})
        }
)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnswerCategory category;

    @Column(name = "answer_text", nullable = false, length = 255)
    private String answerText;

    @Column(name = "auto_submitted", nullable = false)
    private Boolean autoSubmitted;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @OneToOne(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnswerReview review;

    public Answer() {
    }

    public Long getId() {
        return id;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public AnswerCategory getCategory() {
        return category;
    }

    public void setCategory(AnswerCategory category) {
        this.category = category;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Boolean getAutoSubmitted() {
        return autoSubmitted;
    }

    public void setAutoSubmitted(Boolean autoSubmitted) {
        this.autoSubmitted = autoSubmitted;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public AnswerReview getReview() {
        return review;
    }

    public void setReview(AnswerReview review) {
        this.review = review;
    }
}
