package com.k8sgame.service;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private NamespaceService namespaceService;

    @Mock
    private ChallengeLoader challengeLoader;

    @InjectMocks
    private GameService gameService;

    @Test
    void startGame_createsSessionAndNamespace() {
        when(namespaceService.getNamespaceName(anyString())).thenAnswer(inv -> "game-session-" + inv.getArgument(0));
        when(namespaceService.setupNamespace(anyString())).thenReturn(true);

        GameState state = gameService.startGame();

        assertThat(state.getSessionId()).isNotBlank();
        assertThat(state.getNamespace()).startsWith("game-session-");
        verify(namespaceService).setupNamespace(state.getSessionId());
    }

    @Test
    void getCurrentChallenge_returnsFirstChallenge() {
        when(namespaceService.getNamespaceName(anyString())).thenAnswer(inv -> "game-session-" + inv.getArgument(0));
        when(namespaceService.setupNamespace(anyString())).thenReturn(true);

        Challenge first = new Challenge();
        first.setId("c1");
        first.setLevel(1);
        when(challengeLoader.getChallengeAtIndex(0)).thenReturn(Optional.of(first));

        GameState state = gameService.startGame();
        Optional<Challenge> challenge = gameService.getCurrentChallenge(state.getSessionId());

        assertThat(challenge).isPresent();
        assertThat(challenge.get().getId()).isEqualTo("c1");
    }

    @Test
    void advanceChallenge_movesToNextChallenge() {
        when(namespaceService.getNamespaceName(anyString())).thenAnswer(inv -> "game-session-" + inv.getArgument(0));
        when(namespaceService.setupNamespace(anyString())).thenReturn(true);

        Challenge first = new Challenge();
        first.setId("c1");
        first.setLevel(1);
        Challenge second = new Challenge();
        second.setId("c2");
        second.setLevel(1);

        when(challengeLoader.getChallengeAtIndex(0)).thenReturn(Optional.of(first));
        when(challengeLoader.getChallengeAtIndex(1)).thenReturn(Optional.of(second));

        GameState state = gameService.startGame();
        Optional<Challenge> next = gameService.advanceChallenge(state.getSessionId());

        assertThat(next).isPresent();
        assertThat(next.get().getId()).isEqualTo("c2");
        assertThat(state.getCompletedChallengeIds()).contains("c1");
    }

    @Test
    void getGameState_returnsEmptyForUnknownSession() {
        assertThat(gameService.getGameState("unknown")).isEmpty();
    }
}
