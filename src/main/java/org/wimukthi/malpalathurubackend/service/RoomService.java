package org.wimukthi.malpalathurubackend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wimukthi.malpalathurubackend.dto.CreateRoomRequest;
import org.wimukthi.malpalathurubackend.dto.JoinRoomRequest;
import org.wimukthi.malpalathurubackend.dto.PlayerReadyRequest;
import org.wimukthi.malpalathurubackend.dto.RoomResponse;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Room;
import org.wimukthi.malpalathurubackend.entity.Round;
import org.wimukthi.malpalathurubackend.enums.RoomStatus;
import org.wimukthi.malpalathurubackend.enums.RoundStatus;
import org.wimukthi.malpalathurubackend.exception.ResourceNotFoundException;
import org.wimukthi.malpalathurubackend.repository.PlayerRepository;
import org.wimukthi.malpalathurubackend.repository.RoomRepository;
import org.wimukthi.malpalathurubackend.repository.RoundRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final RoomMapper roomMapper;
    private final RoomEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(
            RoomRepository roomRepository,
            PlayerRepository playerRepository,
            RoundRepository roundRepository,
            RoomMapper roomMapper,
            RoomEventPublisher eventPublisher
    ) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
        this.roomMapper = roomMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        Room room = new Room();
        room.setRoomCode(generateUniqueRoomCode());
        room.setLanguage(request.language());
        room.setMaxPlayers(request.maxPlayers());
        room.setCountLimitSeconds(request.countLimitSeconds());
        room.setPrivateRoom(request.privateRoom());
        room.setLocked(false);
        room.setStatus(RoomStatus.LOBBY);
        room.setCreatedAt(LocalDateTime.now());
        room.setTotalRounds(0);
        room.setCurrentRoundNumber(null);

        Room savedRoom = roomRepository.save(room);

        Player host = new Player();
        host.setPlayerName(request.playerName().trim());
        host.setHost(true);
        host.setReady(false);
        host.setConnected(true);
        host.setTotalScore(0);
        host.setJoinedAt(LocalDateTime.now());
        host.setRoom(savedRoom);
        playerRepository.save(host);

        return roomMapper.toRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse joinRoom(JoinRoomRequest request) {
        Room room = getRoomEntityByCode(request.roomCode());
        validateRoomOpen(room);

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalStateException("Game has already started");
        }
        if (Boolean.TRUE.equals(room.getLocked())) {
            throw new IllegalStateException("Room is locked");
        }
        if (playerRepository.countByRoom(room) >= room.getMaxPlayers()) {
            throw new IllegalStateException("Room is full");
        }

        Player player = new Player();
        player.setPlayerName(request.playerName().trim());
        player.setHost(false);
        player.setReady(false);
        player.setConnected(true);
        player.setTotalScore(0);
        player.setJoinedAt(LocalDateTime.now());
        player.setRoom(room);
        playerRepository.save(player);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "PLAYER_JOINED", "A new player joined the room", player.getId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String roomCode) {
        return roomMapper.toRoomResponse(getRoomEntityByCode(roomCode));
    }

    @Transactional
    public RoomResponse lockRoom(String roomCode, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        room.setLocked(true);
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "ROOM_LOCKED", "Room has been locked", null, response);
        return response;
    }

    @Transactional
    public RoomResponse unlockRoom(String roomCode, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        room.setLocked(false);
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "ROOM_UNLOCKED", "Room has been unlocked", null, response);
        return response;
    }

    @Transactional
    public RoomResponse updateReady(String roomCode, Long playerId, PlayerReadyRequest request) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        validateRoomInLobby(room);

        Player player = getPlayerInRoom(room, playerId);
        boolean nextReady = request == null || request.ready() == null
                ? !Boolean.TRUE.equals(player.getReady())
                : request.ready();
        player.setReady(nextReady);
        playerRepository.save(player);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "PLAYER_READY_UPDATED", "Player ready state changed", playerId, response);
        return response;
    }

    @Transactional
    public RoomResponse kickPlayer(String roomCode, Long targetPlayerId, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        Player targetPlayer = getPlayerInRoom(room, targetPlayerId);
        if (Boolean.TRUE.equals(targetPlayer.getHost())) {
            throw new IllegalStateException("Host cannot be kicked");
        }

        playerRepository.delete(targetPlayer);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "PLAYER_KICKED", "Player was removed from the room", targetPlayerId, response);
        return response;
    }

    @Transactional
    public RoomResponse leaveRoom(String roomCode, Long playerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);

        Player player = getPlayerInRoom(room, playerId);
        if (Boolean.TRUE.equals(player.getHost())) {
            return closeRoom(room.getRoomCode(), playerId);
        }

        playerRepository.delete(player);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "PLAYER_LEFT", "Player left the room", playerId, response);
        return response;
    }

    @Transactional
    public RoomResponse startGame(String roomCode, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateRoomOpen(room);
        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        List<Player> players = new ArrayList<>(playerRepository.findByRoom(room));
        if (players.size() < 2) {
            throw new IllegalStateException("At least 2 players are required to start the game");
        }

        Collections.shuffle(players, secureRandom);
        room.setLocked(true);
        room.setStatus(RoomStatus.LETTER_SELECTION);
        room.setTotalRounds(players.size());
        room.setCurrentRoundNumber(1);
        roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < players.size(); i++) {
            Round round = new Round();
            round.setRoom(room);
            round.setRoundNumber(i + 1);
            round.setSuggester(players.get(i));
            round.setStatus(RoundStatus.WAITING_FOR_LETTER);
            round.setCreatedAt(now);
            roundRepository.save(round);
        }

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "GAME_STARTED", "Game has started", null, response);
        return response;
    }

    @Transactional
    public RoomResponse closeRoom(String roomCode, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);
        validateHost(room, hostPlayerId);

        List<Player> players = playerRepository.findByRoom(room);
        players.forEach(player -> {
            player.setConnected(false);
            playerRepository.save(player);
        });

        room.setLocked(true);
        room.setStatus(RoomStatus.CLOSED);
        room.setClosedAt(LocalDateTime.now());
        roomRepository.save(room);

        RoomResponse response = roomMapper.toRoomResponse(room);
        eventPublisher.broadcastState(room.getRoomCode(), "ROOM_CLOSED", "Room has been closed", hostPlayerId, response);
        return response;
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = generateRoomCode();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    private String generateRoomCode() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int index = secureRandom.nextInt(characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }

    private Room getRoomEntityByCode(String roomCode) {
        String normalizedRoomCode = GameTextNormalizer.normalizeRoomCode(roomCode);
        return roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    private Player getPlayerInRoom(Room room, Long playerId) {
        return playerRepository.findByIdAndRoom(playerId, room)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this room"));
    }

    private void validateHost(Room room, Long hostPlayerId) {
        Player hostPlayer = getPlayerInRoom(room, hostPlayerId);
        if (!Boolean.TRUE.equals(hostPlayer.getHost())) {
            throw new IllegalStateException("Only host can perform this action");
        }
    }

    private void validateRoomInLobby(Room room) {
        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalStateException("This action is only allowed before the game starts");
        }
    }

    private void validateRoomOpen(Room room) {
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new IllegalStateException("Room is closed");
        }
    }
}
