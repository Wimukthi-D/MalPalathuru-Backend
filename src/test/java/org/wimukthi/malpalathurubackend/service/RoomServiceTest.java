package org.wimukthi.malpalathurubackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wimukthi.malpalathurubackend.dto.PlayerReadyRequest;
import org.wimukthi.malpalathurubackend.dto.RoomResponse;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Room;
import org.wimukthi.malpalathurubackend.entity.Round;
import org.wimukthi.malpalathurubackend.enums.Language;
import org.wimukthi.malpalathurubackend.enums.RoomStatus;
import org.wimukthi.malpalathurubackend.enums.RoundStatus;
import org.wimukthi.malpalathurubackend.repository.PlayerRepository;
import org.wimukthi.malpalathurubackend.repository.RoomRepository;
import org.wimukthi.malpalathurubackend.repository.RoundRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomEventPublisher eventPublisher;

    @Test
    void startGameRequiresHostPlayer() {
        Room room = room("ABC123");
        Player player = player(2L, "Player Two", false, room);
        RoomService roomService = roomService();

        when(roomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerRepository.findByIdAndRoom(2L, room)).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> roomService.startGame("abc123", 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only host can perform this action");

        verify(playerRepository, never()).findByRoom(room);
    }

    @Test
    void updateReadyTogglesAndBroadcasts() {
        Room room = room("ABC123");
        Player player = player(2L, "Guest", false, room);
        RoomResponse response = response(room);
        RoomService roomService = roomService();

        when(roomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerRepository.findByIdAndRoom(2L, room)).thenReturn(Optional.of(player));
        when(roomMapper.toRoomResponse(room)).thenReturn(response);

        RoomResponse actual = roomService.updateReady("abc123", 2L, new PlayerReadyRequest(true));

        assertThat(actual).isSameAs(response);
        assertThat(player.getReady()).isTrue();
        verify(playerRepository).save(player);
        verify(eventPublisher).broadcastState("ABC123", "PLAYER_READY_UPDATED", "Player ready state changed", 2L, response);
    }

    @Test
    void kickPlayerRemovesGuestOnlyBeforeGameStarts() {
        Room room = room("ABC123");
        Player host = player(1L, "Host", true, room);
        Player guest = player(2L, "Guest", false, room);
        RoomResponse response = response(room);
        RoomService roomService = roomService();

        when(roomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerRepository.findByIdAndRoom(1L, room)).thenReturn(Optional.of(host));
        when(playerRepository.findByIdAndRoom(2L, room)).thenReturn(Optional.of(guest));
        when(roomMapper.toRoomResponse(room)).thenReturn(response);

        RoomResponse actual = roomService.kickPlayer("abc123", 2L, 1L);

        assertThat(actual).isSameAs(response);
        verify(playerRepository).delete(guest);
        verify(eventPublisher).broadcastState("ABC123", "PLAYER_KICKED", "Player was removed from the room", 2L, response);
    }

    @Test
    void startGameCreatesOneRoundPerPlayer() {
        Room room = room("ABC123");
        Player host = player(1L, "Host", true, room);
        Player guest = player(2L, "Guest", false, room);
        RoomResponse response = response(room);
        RoomService roomService = roomService();

        when(roomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerRepository.findByIdAndRoom(1L, room)).thenReturn(Optional.of(host));
        when(playerRepository.findByRoom(room)).thenReturn(List.of(host, guest));
        when(roomMapper.toRoomResponse(room)).thenReturn(response);

        RoomResponse actual = roomService.startGame("abc123", 1L);

        assertThat(actual).isSameAs(response);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.LETTER_SELECTION);
        assertThat(room.getLocked()).isTrue();
        assertThat(room.getTotalRounds()).isEqualTo(2);
        assertThat(room.getCurrentRoundNumber()).isEqualTo(1);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository, org.mockito.Mockito.times(2)).save(roundCaptor.capture());
        assertThat(roundCaptor.getAllValues())
                .extracting(Round::getStatus)
                .containsOnly(RoundStatus.WAITING_FOR_LETTER);
        assertThat(roundCaptor.getAllValues())
                .extracting(Round::getRoundNumber)
                .containsExactly(1, 2);
        verify(eventPublisher).broadcastState("ABC123", "GAME_STARTED", "Game has started", null, response);
    }

    private RoomService roomService() {
        return new RoomService(roomRepository, playerRepository, roundRepository, roomMapper, eventPublisher);
    }

    private Room room(String roomCode) {
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setLanguage(Language.ENGLISH);
        room.setMaxPlayers(4);
        room.setCountLimitSeconds(30);
        room.setPrivateRoom(false);
        room.setLocked(false);
        room.setStatus(RoomStatus.LOBBY);
        room.setCreatedAt(LocalDateTime.now());
        return room;
    }

    private Player player(Long id, String name, boolean host, Room room) {
        Player player = new Player();
        setId(player, id);
        player.setPlayerName(name);
        player.setHost(host);
        player.setReady(false);
        player.setConnected(true);
        player.setTotalScore(0);
        player.setJoinedAt(LocalDateTime.now());
        player.setRoom(room);
        return player;
    }

    private RoomResponse response(Room room) {
        return new RoomResponse(
                null,
                room.getRoomCode(),
                room.getLanguage(),
                room.getMaxPlayers(),
                room.getCountLimitSeconds(),
                room.getPrivateRoom(),
                room.getLocked(),
                room.getStatus(),
                List.of()
        );
    }

    private void setId(Player player, Long id) {
        try {
            Field field = Player.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(player, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not set player id for test", ex);
        }
    }
}
