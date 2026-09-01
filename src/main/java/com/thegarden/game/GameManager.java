package com.thegarden.game;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession createGame(int totalRounds) {
        String gameId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        GameSession session = new GameSession(gameId, totalRounds);
        sessions.put(gameId, session);
        return session;
    }

    public GameSession getGame(String gameId) { return sessions.get(gameId); }
}
