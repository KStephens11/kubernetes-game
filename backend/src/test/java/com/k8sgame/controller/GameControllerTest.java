package com.k8sgame.controller;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import com.k8sgame.service.GameService;
import com.k8sgame.service.HintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({GameController.class, HintController.class, GlobalExceptionHandler.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private HintService hintService;

    @Test
    void startGame_returns200WithSessionId() throws Exception {
        GameState state = new GameState("abc123", "game-session-abc123");
        when(gameService.startGame()).thenReturn(state);
        when(gameService.getCurrentChallenge("abc123")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/game/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("abc123"));
    }

    @Test
    void getGameState_returns404ForUnknownSession() throws Exception {
        when(gameService.getGameState("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/game/state/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentChallenge_returnsChallenge() throws Exception {
        Challenge c = new Challenge();
        c.setId("c1");
        c.setLevel(1);
        c.setTitle("Test Challenge");
        c.setDescription("desc");
        c.setStoryContext("story");
        c.setSuccessCriteria(List.of());
        c.setHints(List.of());

        when(gameService.getCurrentChallenge("abc123")).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/challenges/current/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.title").value("Test Challenge"));
    }

    @Test
    void getHint_returnsHint() throws Exception {
        GameState state = new GameState("abc123", "game-session-abc123");
        Challenge c = new Challenge();
        c.setId("c1");
        c.setHints(List.of(Map.of("level", 1, "text", "Try kubectl run")));

        when(gameService.getGameState("abc123")).thenReturn(Optional.of(state));
        when(gameService.getCurrentChallenge("abc123")).thenReturn(Optional.of(c));
        when(hintService.getNextHint(state, c)).thenReturn(Optional.of(
                Map.of("hint", "Try kubectl run", "hintsRemaining", 2, "hintLevel", 1)
        ));

        mockMvc.perform(get("/api/hints/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hint").value("Try kubectl run"));
    }
}
