package org.wimukthi.malpalathurubackend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.wimukthi.malpalathurubackend.dto.CreateRoomRequest;
import org.wimukthi.malpalathurubackend.dto.JoinRoomRequest;
import org.wimukthi.malpalathurubackend.dto.PlayerResponse;
import org.wimukthi.malpalathurubackend.dto.RoomResponse;
import org.wimukthi.malpalathurubackend.entity.Player;
import org.wimukthi.malpalathurubackend.entity.Room;
import org.wimukthi.malpalathurubackend.enums.RoomStatus;
import org.wimukthi.malpalathurubackend.exception.ResourceNotFoundException;
import org.wimukthi.malpalathurubackend.repository.PlayerRepository;
import org.wimukthi.malpalathurubackend.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wimukthi.malpalathurubackend.dto.RoomEventResponse;
import org.wimukthi.malpalathurubackend.dto.UpdateReadyRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(RoomRepository roomRepository, PlayerRepository playerRepository , SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.messagingTemplate = messagingTemplate;
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

        return getRoomByCode(savedRoom.getRoomCode());
    }

    @Transactional
    public RoomResponse joinRoom(JoinRoomRequest request) {
        String normalizedRoomCode = request.roomCode().trim().toUpperCase();

        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalStateException("Game has already started");
        }

        if (Boolean.TRUE.equals(room.getLocked())) {
            throw new IllegalStateException("Room is locked");
        }

        long currentPlayers = playerRepository.countByRoom(room);

        if (currentPlayers >= room.getMaxPlayers()) {
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

        RoomResponse response = getRoomByCode(room.getRoomCode());

        broadcastRoomState(
                room.getRoomCode(),
                "PLAYER_JOINED",
                "A new player joined the room",
                null
        );

        return response;
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Player> players = playerRepository.findByRoom(room);

        List<PlayerResponse> playerResponses = players.stream()
                .map(this::mapPlayerToResponse)
                .toList();

        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getLanguage(),
                room.getMaxPlayers(),
                room.getCountLimitSeconds(),
                room.getPrivateRoom(),
                room.getLocked(),
                room.getStatus(),
                playerResponses
        );
    }

    @Transactional
    public RoomResponse lockRoom(String roomCode,Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);

        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        room.setLocked(true);
        roomRepository.save(room);

        broadcastRoomState(
                room.getRoomCode(),
                "ROOM_LOCKED",
                "Room has been locked",
                null
        );

        return getRoomByCode(room.getRoomCode());
    }

    @Transactional
    public RoomResponse unlockRoom(String roomCode,Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);

        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        room.setLocked(false);
        roomRepository.save(room);

        broadcastRoomState(
                room.getRoomCode(),
                "ROOM_UNLOCKED",
                "Room has been unlocked",
                null
        );
        return getRoomByCode(room.getRoomCode());
    }

    @Transactional
    public RoomResponse updateReadyStatus(
            String roomCode,
            Long playerId,
            UpdateReadyRequest request
    ) {
        Room room = getRoomEntityByCode(roomCode);

        validateRoomInLobby(room);

        Player player = getPlayerInRoom(room, playerId);

        if (Boolean.TRUE.equals(player.getHost())) {
            throw new IllegalStateException("Host does not need to mark ready");
        }

        player.setReady(request.ready());
        playerRepository.save(player);

        broadcastRoomState(
                room.getRoomCode(),
                "PLAYER_READY_UPDATED",
                "Player ready status updated",
                player.getId()
        );

        return getRoomByCode(room.getRoomCode());
    }

    @Transactional
    public RoomResponse kickPlayer(
            String roomCode,
            Long targetPlayerId,
            Long hostPlayerId
    ) {
        Room room = getRoomEntityByCode(roomCode);

        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        Player targetPlayer = getPlayerInRoom(room, targetPlayerId);

        if (Boolean.TRUE.equals(targetPlayer.getHost())) {
            throw new IllegalStateException("Host cannot be kicked");
        }

        playerRepository.delete(targetPlayer);

        RoomResponse response = getRoomByCode(room.getRoomCode());

        broadcastRoomState(
                room.getRoomCode(),
                "PLAYER_KICKED",
                "Player was removed from the room",
                targetPlayerId
        );

        return response;
    }

    @Transactional
    public RoomResponse startGame(String roomCode, Long hostPlayerId) {
        Room room = getRoomEntityByCode(roomCode);

        validateRoomInLobby(room);
        validateHost(room, hostPlayerId);

        long playerCount = playerRepository.countByRoom(room);

        if (playerCount < 2) {
            throw new IllegalStateException("At least 2 players are required to start the game");
        }

        room.setLocked(true);
        room.setStatus(RoomStatus.LETTER_SELECTION);
        roomRepository.save(room);

        broadcastRoomState(
                room.getRoomCode(),
                "GAME_STARTED",
                "Game has started",
                null
        );

        return getRoomByCode(room.getRoomCode());
    }



    private PlayerResponse mapPlayerToResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getPlayerName(),
                player.getHost(),
                player.getReady(),
                player.getConnected(),
                player.getTotalScore()
        );
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
        return roomRepository.findByRoomCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    private Player getPlayerInRoom(Room room, Long playerId) {
        return playerRepository.findByIdAndRoom(playerId, room)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this room"));
    }

    private void validateHost(Room room, Long hostPlayerId) {
        Player hostPlayer = getPlayerInRoom(room, hostPlayerId);

        if(!Boolean.TRUE.equals(hostPlayer.getHost())) {
            throw new IllegalStateException("Only host can perform this action");
        }
    }

    private void validateRoomInLobby(Room room) {
        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new IllegalStateException("This action is only allowed before the game starts");
        }
    }

    private void broadcastRoomState(String roomCode, String eventType, String message, Long affectedPlayerId) {
        RoomResponse roomResponse = getRoomByCode(roomCode);

        RoomEventResponse event =  new RoomEventResponse(
                eventType,
                message,
                affectedPlayerId,
                roomResponse
        );

        messagingTemplate.convertAndSend(
                "/topic/rooms" + roomCode + "state",
                event
        );
    }

}