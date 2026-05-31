package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;
import org.wimukthi.malpalathurubackend.enums.ReviewDecision;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_reviews")
public class AnswerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReviewDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_host_player_id")
    private Player decidedByHost;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public AnswerReview() {
    }

    public Long getId() {
        return id;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public Player getDecidedByHost() {
        return decidedByHost;
    }

    public void setDecidedByHost(Player decidedByHost) {
        this.decidedByHost = decidedByHost;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
