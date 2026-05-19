package org.wimukthi.malpalathurubackend.service;

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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository, PlayerRepository playerRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
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

        return getRoomByCode(room.getRoomCode());
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
}