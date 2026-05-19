package org.wimukthi.malpalathurubackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
}
