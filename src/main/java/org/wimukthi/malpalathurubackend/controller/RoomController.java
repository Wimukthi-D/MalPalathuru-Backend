package org.wimukthi.malpalathurubackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wimukthi.malpalathurubackend.dto.*;
import org.wimukthi.malpalathurubackend.service.GameService;
import org.wimukthi.malpalathurubackend.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final GameService gameService;

    public RoomController(RoomService roomService, GameService gameService) {
        this.roomService = roomService;
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    @PostMapping("/join")
    public RoomResponse joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(request);
    }

    @GetMapping("/{roomCode}")
    public RoomResponse getRoom(@PathVariable String roomCode) {
        return roomService.getRoomByCode(roomCode);
    }

    @PatchMapping("/{roomCode}/lock")
    public RoomResponse lockRoom(@PathVariable String roomCode, @RequestParam Long hostPlayerId) {
        return roomService.lockRoom(roomCode, hostPlayerId);
    }

    @PatchMapping("/{roomCode}/unlock")
    public RoomResponse unlockRoom(@PathVariable String roomCode, @RequestParam Long hostPlayerId) {
        return roomService.unlockRoom(roomCode, hostPlayerId);
    }

    @PatchMapping("/{roomCode}/players/{playerId}/ready")
    public RoomResponse updateReady(
            @PathVariable String roomCode,
            @PathVariable Long playerId,
            @RequestBody(required = false) PlayerReadyRequest request
    ) {
        return roomService.updateReady(roomCode, playerId, request);
    }

    @DeleteMapping("/{roomCode}/players/{playerId}")
    public ResponseEntity<RoomResponse> removePlayer(
            @PathVariable String roomCode,
            @PathVariable Long playerId,
            @RequestParam(required = false) Long hostPlayerId
    ) {
        RoomResponse response = hostPlayerId == null
                ? roomService.leaveRoom(roomCode, playerId)
                : roomService.kickPlayer(roomCode, playerId, hostPlayerId);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{roomCode}/start", method = {RequestMethod.POST, RequestMethod.PATCH})
    public RoomResponse startGame(@PathVariable String roomCode, @RequestParam Long hostPlayerId) {
        return roomService.startGame(roomCode, hostPlayerId);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/select-letter")
    public RoomResponse selectLetter(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @Valid @RequestBody SelectLetterRequest request
    ) {
        return gameService.selectLetter(roomCode, roundId, request);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/answers")
    public RoomResponse submitAnswers(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @Valid @RequestBody SubmitAnswersRequest request
    ) {
        return gameService.submitAnswers(roomCode, roundId, request);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/lock")
    public RoomResponse lockRound(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @RequestParam(required = false) Long hostPlayerId
    ) {
        return gameService.lockRound(roomCode, roundId, hostPlayerId);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/review/start")
    public ReviewStateResponse startReview(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @RequestParam Long hostPlayerId
    ) {
        return gameService.startReview(roomCode, roundId, hostPlayerId);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/review/vote")
    public ReviewStateResponse vote(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @Valid @RequestBody ReviewVoteRequest request
    ) {
        return gameService.vote(roomCode, roundId, request);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/review/decision")
    public ReviewStateResponse decideAnswer(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @RequestParam Long hostPlayerId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        return gameService.decideAnswer(roomCode, roundId, hostPlayerId, request);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/review/complete")
    public LeaderboardResponse completeReview(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @RequestParam Long hostPlayerId
    ) {
        return gameService.completeReview(roomCode, roundId, hostPlayerId);
    }

    @PostMapping("/{roomCode}/rounds/{roundId}/next")
    public GameAdvanceResponse nextRound(
            @PathVariable String roomCode,
            @PathVariable Long roundId,
            @RequestParam Long hostPlayerId
    ) {
        return gameService.nextRound(roomCode, roundId, hostPlayerId);
    }

    @PostMapping("/{roomCode}/close")
    public RoomResponse closeRoom(@PathVariable String roomCode, @RequestParam Long hostPlayerId) {
        return roomService.closeRoom(roomCode, hostPlayerId);
    }
}
