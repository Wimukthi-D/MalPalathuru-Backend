package org.wimukthi.malpalathurubackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wimukthi.malpalathurubackend.dto.CreateRoomRequest;
import org.wimukthi.malpalathurubackend.dto.JoinRoomRequest;
import org.wimukthi.malpalathurubackend.dto.RoomResponse;
import org.wimukthi.malpalathurubackend.service.RoomService;

@RestController
@RequestMapping("api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request){
        return roomService.createRoom(request);
    }

    @PostMapping("/join")
    public RoomResponse joinRoom(@Valid @RequestBody JoinRoomRequest request){
        return roomService.joinRoom(request);
    }

    @GetMapping("/{roomCode}")
    public RoomResponse getRoom(@PathVariable String roomCode){
        return roomService.getRoomByCode(roomCode);
    }

    @PatchMapping("/{roomCode}/lock")
    public RoomResponse lockRoom(@PathVariable String roomCode, @RequestParam Long hostPlayerId){
        return roomService.lockRoom(roomCode, hostPlayerId);
    }

    @PatchMapping("/{roomCode}/unlock")
    public RoomResponse unlockRoom(@PathVariable String roomCode, @RequestParam Long hostPlayerId){
        return roomService.unlockRoom(roomCode, hostPlayerId);
    }

    @DeleteMapping("/{roomCode}/players/{playerId}")
    public ResponseEntity<RoomResponse> removePlayer(
            @PathVariable String roomCode,
            @PathVariable Long playerId,
            @RequestParam(required = false) Long hostPlayerId
    ){
        RoomResponse response = hostPlayerId == null
                ? roomService.leaveRoom(roomCode, playerId)
                : roomService.kickPlayer(roomCode, playerId, hostPlayerId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{roomCode}/start")
    public RoomResponse startGame(@PathVariable String roomCode, @RequestParam Long hostPlayerId){
        return roomService.startGame(roomCode, hostPlayerId);
    }
}
