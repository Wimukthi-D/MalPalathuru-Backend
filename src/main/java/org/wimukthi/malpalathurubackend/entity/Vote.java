package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;
import org.wimukthi.malpalathurubackend.enums.VoteValue;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vote_answer_voter", columnNames = {"answer_id", "voter_player_id"})
        }
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_player_id", nullable = false)
    private Player voter;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_value", nullable = false, length = 20)
    private VoteValue value;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    public Vote() {
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

    public Player getVoter() {
        return voter;
    }

    public void setVoter(Player voter) {
        this.voter = voter;
    }

    public VoteValue getValue() {
        return value;
    }

    public void setValue(VoteValue value) {
        this.value = value;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
