package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;
import org.wimukthi.malpalathurubackend.enums.RoundStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "rounds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_round_room_number", columnNames = {"room_id", "round_number"})
        }
)
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggester_player_id", nullable = false)
    private Player suggester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoundStatus status;

    @Column(name = "selected_letter", length = 16)
    private String selectedLetter;

    @Column(name = "selected_letter_normalized", length = 64)
    private String selectedLetterNormalized;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "letter_selected_at")
    private LocalDateTime letterSelectedAt;

    @Column(name = "countdown_started_at")
    private LocalDateTime countdownStartedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "review_started_at")
    private LocalDateTime reviewStartedAt;

    @Column(name = "score_calculated_at")
    private LocalDateTime scoreCalculatedAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    public Round() {
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Player getSuggester() {
        return suggester;
    }

    public void setSuggester(Player suggester) {
        this.suggester = suggester;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public String getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(String selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    public String getSelectedLetterNormalized() {
        return selectedLetterNormalized;
    }

    public void setSelectedLetterNormalized(String selectedLetterNormalized) {
        this.selectedLetterNormalized = selectedLetterNormalized;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLetterSelectedAt() {
        return letterSelectedAt;
    }

    public void setLetterSelectedAt(LocalDateTime letterSelectedAt) {
        this.letterSelectedAt = letterSelectedAt;
    }

    public LocalDateTime getCountdownStartedAt() {
        return countdownStartedAt;
    }

    public void setCountdownStartedAt(LocalDateTime countdownStartedAt) {
        this.countdownStartedAt = countdownStartedAt;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getReviewStartedAt() {
        return reviewStartedAt;
    }

    public void setReviewStartedAt(LocalDateTime reviewStartedAt) {
        this.reviewStartedAt = reviewStartedAt;
    }

    public LocalDateTime getScoreCalculatedAt() {
        return scoreCalculatedAt;
    }

    public void setScoreCalculatedAt(LocalDateTime scoreCalculatedAt) {
        this.scoreCalculatedAt = scoreCalculatedAt;
    }

    public List<Answer> getAnswers() {
        return answers;
    }
}
