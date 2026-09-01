package com.thegarden.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired private GameManager gameManager;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/api/create-game")
    public Map<String, String> createGame(@RequestBody Map<String, Object> body) {
        int totalRounds = body.get("totalRounds") != null ? (Integer) body.get("totalRounds") : 8;
        GameSession session = gameManager.createGame(totalRounds);
        return Map.of("gameId", session.gameId);
    }

    @PostMapping("/api/join-game/{gameId}")
    public Map<String, Object> joinGame(@PathVariable String gameId, @RequestBody Map<String, String> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        Player p = session.addPlayer(body.get("name"));
        broadcastState(gameId);
        Map<String, Object> response = new HashMap<>();
        response.put("playerId", p.id);
        response.put("seed", p.seed); // Can be null until seeds are assigned
        return response;
    }

    @PostMapping("/api/{gameId}/start-round")
    public Map<String, Object> startRound(@PathVariable String gameId) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.startRound();
        broadcastState(gameId);
        return Map.of("round", session.currentRound);
    }

    @PostMapping("/api/{gameId}/submit-bid")
    public Map<String, Object> submitBid(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.submitBid((String) body.get("playerId"), (Integer) body.get("amount"), (String) body.get("eventChoice"));
        broadcastState(gameId);
        if (session.allBidsSubmitted()) { session.resolveBidding(); broadcastState(gameId); }
        return Map.of("status", "ok");
    }

    @PostMapping("/api/{gameId}/buy-action-card")
    public Map<String, Object> buyActionCard(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.buyActionCard((String) body.get("playerId"), (String) body.get("target"), (String) body.get("waterChoice"));
        broadcastState(gameId);
        return Map.of("status", "ok");
    }

    @PostMapping("/api/{gameId}/submit-action")
    public Map<String, Object> submitAction(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.submitAction((String) body.get("playerId"), (String) body.get("action"), (String) body.get("waterChoice"));
        broadcastState(gameId);
        if (session.allActionsSubmitted()) { session.resolveRound(); broadcastState(gameId); }
        return Map.of("status", "ok");
    }

    @PostMapping("/api/{gameId}/sacrifice")
    public Map<String, Object> sacrifice(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.sacrifice((String) body.get("playerId"), (String) body.get("stat"), (Integer) body.get("amount"));
        broadcastState(gameId);
        return Map.of("status", "ok");
    }

    @PostMapping("/api/{gameId}/gift-coins")
    public Map<String, Object> giftCoins(@PathVariable String gameId, @RequestBody Map<String, Object> body) {
        GameSession session = gameManager.getGame(gameId);
        if (session == null) return Map.of("error", "Game not found");
        session.giftCoins((String) body.get("fromId"), (String) body.get("toId"), (Integer) body.get("amount"));
        broadcastState(gameId);
        return Map.of("status", "ok");
    }

    @GetMapping("/api/{gameId}/state")
    public GameSession getState(@PathVariable String gameId) { return gameManager.getGame(gameId); }

    private void broadcastState(String gameId) {
        GameSession session = gameManager.getGame(gameId);
        if (session != null) messagingTemplate.convertAndSend("/topic/game/" + gameId, session);
    }
}
