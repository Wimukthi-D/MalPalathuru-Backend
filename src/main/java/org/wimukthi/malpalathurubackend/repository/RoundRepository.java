package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.wimukthi.malpalathurubackend.entity.Room;
import org.wimukthi.malpalathurubackend.entity.Round;

import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByRoomOrderByRoundNumberAsc(Room room);

    Optional<Round> findByIdAndRoom(Long id, Room room);

    Optional<Round> findByRoomAndRoundNumber(Room room, Integer roundNumber);

    @EntityGraph(attributePaths = "room")
    List<Round> findByCountdownStartedAtIsNotNullAndLockedAtIsNull();
}
