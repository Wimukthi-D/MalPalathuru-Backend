package org.wimukthi.malpalathurubackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "used_letters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_used_letter_room_normalized", columnNames = {"room_id", "normalized_letter"})
        }
)
public class UsedLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false, unique = true)
    private Round round;

    @Column(name = "display_letter", nullable = false, length = 16)
    private String displayLetter;

    @Column(name = "normalized_letter", nullable = false, length = 64)
    private String normalizedLetter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UsedLetter() {
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

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public String getDisplayLetter() {
        return displayLetter;
    }

    public void setDisplayLetter(String displayLetter) {
        this.displayLetter = displayLetter;
    }

    public String getNormalizedLetter() {
        return normalizedLetter;
    }

    public void setNormalizedLetter(String normalizedLetter) {
        this.normalizedLetter = normalizedLetter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
