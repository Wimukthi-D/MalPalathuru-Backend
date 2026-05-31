package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;
import org.wimukthi.malpalathurubackend.enums.AnswerCategory;

@Entity
@Table(
        name = "scores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_score_round_player_category", columnNames = {"round_id", "player_id", "category"})
        }
)
public class Score {

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

    @Column(nullable = false)
    private Integer points;

    public Score() {
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

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}
