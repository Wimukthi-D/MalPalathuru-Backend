package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Room;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByRoom(Room room);

    Long countByRoom(Room room);

    Optional<Player> findByIdAndRoom(Long id, Room room);
}
