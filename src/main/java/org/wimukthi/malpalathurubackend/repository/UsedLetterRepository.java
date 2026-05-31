package org.wimukthi.malpalathurubackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wimukthi.malpalathurubackend.entity.Room;
import org.wimukthi.malpalathurubackend.entity.UsedLetter;

import java.util.List;

public interface UsedLetterRepository extends JpaRepository<UsedLetter, Long> {

    boolean existsByRoomAndNormalizedLetter(Room room, String normalizedLetter);

    List<UsedLetter> findByRoomOrderByCreatedAtAsc(Room room);
}
