package com.k8sgame.controller;

import com.k8sgame.model.CommandRequest;
import com.k8sgame.model.CommandResponse;
import com.k8sgame.model.GameState;
import com.k8sgame.model.ValidationResult;
import com.k8sgame.service.CommandService;
import com.k8sgame.service.GameService;
import com.k8sgame.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminalWebSocketControllerTest {

    @Mock private CommandService commandService;
    @Mock private GameService gameService;
    @Mock private ValidationService validationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TerminalWebSocketController controller;

    @Test
    void handleCommand_sendsErrorWhenSessionNotFound() {
        when(gameService.getGameState("unknown")).thenReturn(Optional.empty());

        controller.handleCommand(new CommandRequest("unknown", "kubectl get pods"));

        ArgumentCaptor<CommandResponse> captor = ArgumentCaptor.forClass(CommandResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq("unknown"), eq("/queue/output"), captor.capture());
        assertThat(captor.getValue().success()).isFalse();
        assertThat(captor.getValue().output()).contains("Session not found");
    }

    @Test
    void handleCommand_executesKubectlAndSendsOutput() {
        GameState state = new GameState("abc", "game-session-abc");
        when(gameService.getGameState("abc")).thenReturn(Optional.of(state));
        when(commandService.execute("game-session-abc", "kubectl get pods"))
                .thenReturn(CommandResponse.success("No resources found"));
        when(gameService.getCurrentChallenge("abc")).thenReturn(Optional.empty());

        controller.handleCommand(new CommandRequest("abc", "kubectl get pods"));

        verify(commandService).execute("game-session-abc", "kubectl get pods");
        verify(messagingTemplate).convertAndSendToUser(eq("abc"), eq("/queue/output"), any(CommandResponse.class));
    }

    @Test
    void handleCommand_gameStatusReturnsState() {
        GameState state = new GameState("abc", "game-session-abc");
        when(gameService.getGameState("abc")).thenReturn(Optional.of(state));

        controller.handleCommand(new CommandRequest("abc", "game status"));

        ArgumentCaptor<CommandResponse> captor = ArgumentCaptor.forClass(CommandResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq("abc"), eq("/queue/output"), captor.capture());
        assertThat(captor.getValue().success()).isTrue();
        assertThat(captor.getValue().output()).contains("Level:");
    }
}
