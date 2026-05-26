# Development Skills and Practices

## Code Quality Standards

### Backend (Java/Spring Boot)

#### Java Best Practices
- Use Java 17+ features (records, sealed classes, pattern matching)
- Follow Google Java Style Guide or Spring conventions
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Prefer composition over inheritance
- Use Optional for nullable return values
- Leverage Spring's dependency injection

#### Code Style
```java
@Service
@RequiredArgsConstructor
public class ValidationService {
    
    private final KubernetesClient kubernetesClient;
    private final ChallengeRepository challengeRepository;
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    
    /**
     * Validates if cluster state meets challenge success criteria.
     */
    public ValidationResult validateChallenge(String namespace, String challengeId) {
        return challengeRepository.findById(challengeId)
            .map(challenge -> performValidation(namespace, challenge))
            .orElseThrow(() -> new ChallengeNotFoundException(challengeId));
    }
}
```

### Frontend (React/JavaScript)

#### React Best Practices
- Use functional components with hooks
- Follow React naming conventions (PascalCase for components)
- Use TypeScript for type safety (optional but recommended)
- Keep components small and focused (single responsibility)
- Use custom hooks for reusable logic
- Implement proper error boundaries
- Optimize re-renders with useMemo and useCallback

#### Component Structure
```javascript
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';

/**
 * Terminal component for kubectl command execution.
 */
export const Terminal = ({ namespace, onCommandExecute }) => {
    const [output, setOutput] = useState([]);
    const [history, setHistory] = useState([]);
    
    const executeCommand = useCallback(async (command) => {
        // Implementation
    }, [namespace]);
    
    return (
        <div className="terminal">
            {/* Component JSX */}
        </div>
    );
};

Terminal.propTypes = {
    namespace: PropTypes.string.isRequired,
    onCommandExecute: PropTypes.func
};
```

### Error Handling

#### Backend Error Handling
- Use specific exception types with custom exceptions
- Implement global exception handler with @ControllerAdvice
- Provide meaningful error messages that guide users
- Log errors with context for debugging
- Never expose stack traces to players

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(KubernetesClientException.class)
    public ResponseEntity<ErrorResponse> handleKubernetesException(
            KubernetesClientException ex) {
        logger.error("Kubernetes API error: {}", ex.getMessage(), ex);
        
        if (ex.getCode() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "Your game namespace doesn't exist. Try 'game reset'."
                ));
        }
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(
                "Unable to connect to cluster. Check your connection."
            ));
    }
}
```

#### Frontend Error Handling
```javascript
// Error boundary component
class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }
    
    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }
    
    componentDidCatch(error, errorInfo) {
        console.error('Error caught by boundary:', error, errorInfo);
    }
    
    render() {
        if (this.state.hasError) {
            return <ErrorDisplay error={this.state.error} />;
        }
        return this.props.children;
    }
}

// API error handling
const handleApiError = (error) => {
    if (error.response) {
        // Server responded with error status
        return error.response.data.message || 'An error occurred';
    } else if (error.request) {
        // Request made but no response
        return 'Unable to connect to server';
    } else {
        // Something else happened
        return error.message;
    }
};
```

## Game Design Patterns

### Challenge Definition Structure

All challenges should follow this YAML schema:

```yaml
challenge_id: "pod-basics-01"
level: 1
title: "Deploy Your First Pod"
difficulty: beginner
estimated_time: 5

story_context: |
  The station's life support system needs a monitoring pod deployed.
  Your first task is to get it running.

learning_objectives:
  - Understand pod creation
  - Use kubectl run command
  - Verify pod status

initial_state:
  resources: []

success_criteria:
  - type: pod_exists
    name: "life-support-monitor"
    namespace: "player"
  - type: pod_status
    name: "life-support-monitor"
    status: "Running"

hints:
  - level: 1
    text: "Use 'kubectl run' to create a pod"
  - level: 2
    text: "Try: kubectl run life-support-monitor --image=nginx"
  - level: 3
    text: "Check pod status with: kubectl get pods"

validation_timeout: 30
max_hints: 3
```

### Progressive Hint System

Hints should follow the three-level pattern:
1. **Conceptual**: What Kubernetes concept to use
2. **Tactical**: Which kubectl command to use
3. **Explicit**: Exact command with parameters

### Narrative Voice

- Use second-person perspective ("You notice...", "Your mission...")
- Create urgency and stakes without being melodramatic
- Relate challenges to real-world scenarios (system failures, scaling needs, security incidents)
- Keep story segments concise (2-4 sentences per transition)
- Use technical terminology correctly to reinforce learning

```java
@Service
public class NarrativeService {
    
    private static final Map<Integer, String> LEVEL_INTROS = Map.of(
        1, "Welcome aboard the Kubernetes Station. Systems are failing and you're " +
           "the new cluster operator. Your first task: get critical pods running.",
        2, "The station is stable, but traffic is increasing. You need to deploy " +
           "services and manage networking before systems overload."
    );
    
    /**
     * Returns engaging story introduction for level.
     */
    public String getLevelIntro(int level) {
        return LEVEL_INTROS.getOrDefault(level, "");
    }
}
```

## Kubernetes Client Patterns

### Safe Cluster Operations (Backend)

Always wrap Kubernetes operations with safety checks using Fabric8 client:

```java
@Component
public class SafetyGuard {
    
    private static final Set<String> PROTECTED_NAMESPACES = Set.of(
        "kube-system", "kube-public", "default", "kube-node-lease"
    );
    
    private static final Set<String> RESTRICTED_RESOURCES = Set.of(
        "namespace", "persistentvolume", "clusterrole", "clusterrolebinding"
    );
    
    /**
     * Validates if operation is safe to execute.
     *
     * @return ValidationResult with safety status and error message if unsafe
     */
    public ValidationResult validateOperation(
            String operation,
            String namespace,
            String resourceType) {
        
        if (PROTECTED_NAMESPACES.contains(namespace)) {
            return ValidationResult.unsafe(
                String.format("Cannot modify protected namespace: %s", namespace)
            );
        }
        
        if ("delete".equals(operation) && RESTRICTED_RESOURCES.contains(resourceType)) {
            return ValidationResult.unsafe(
                String.format("Deletion of %s is not allowed", resourceType)
            );
        }
        
        return ValidationResult.safe();
    }
}
```

### Namespace Isolation

Every player operation must be scoped to their namespace:

```java
@Service
@RequiredArgsConstructor
public class NamespaceManagerService {
    
    private final KubernetesClient kubernetesClient;
    private static final String NAMESPACE_PREFIX = "k8s-game-";
    
    /**
     * Generates isolated namespace for player.
     */
    public String getPlayerNamespace(String playerId) {
        return NAMESPACE_PREFIX + playerId;
    }
    
    /**
     * Creates namespace with resource quotas for player.
     */
    public boolean setupPlayerNamespace(String playerId) {
        String namespace = getPlayerNamespace(playerId);
        
        try {
            // Create namespace
            Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespace)
                    .addToLabels("app", "k8s-game")
                    .addToLabels("player-id", playerId)
                .endMetadata()
                .build();
            
            kubernetesClient.namespaces().create(ns);
            
            // Apply resource quotas
            ResourceQuota quota = new ResourceQuotaBuilder()
                .withNewMetadata()
                    .withName("player-quota")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .addToHard("pods", new Quantity("10"))
                    .addToHard("services", new Quantity("5"))
                    .addToHard("persistentvolumeclaims", new Quantity("3"))
                    .addToHard("requests.cpu", new Quantity("2"))
                    .addToHard("requests.memory", new Quantity("4Gi"))
                .endSpec()
                .build();
            
            kubernetesClient.resourceQuotas()
                .inNamespace(namespace)
                .create(quota);
            
            return true;
        } catch (KubernetesClientException e) {
            logger.error("Failed to setup namespace for player {}", playerId, e);
            return false;
        }
    }
    
    /**
     * Cleans up player namespace and all resources.
     */
    public void cleanupPlayerNamespace(String playerId) {
        String namespace = getPlayerNamespace(playerId);
        kubernetesClient.namespaces().withName(namespace).delete();
    }
}
```

### Command Execution Service

```java
@Service
@RequiredArgsConstructor
public class CommandExecutorService {
    
    private final KubernetesClient kubernetesClient;
    private final SafetyGuard safetyGuard;
    
    /**
     * Executes kubectl command in player's namespace.
     */
    public CommandResponse executeCommand(String namespace, String command) {
        // Parse command
        KubectlCommand parsed = parseKubectlCommand(command);
        
        // Validate safety
        ValidationResult safety = safetyGuard.validateOperation(
            parsed.getOperation(),
            namespace,
            parsed.getResourceType()
        );
        
        if (!safety.isSafe()) {
            return CommandResponse.error(safety.getMessage());
        }
        
        // Execute command
        try {
            String output = executeKubectlOperation(namespace, parsed);
            return CommandResponse.success(output);
        } catch (KubernetesClientException e) {
            logger.error("Command execution failed: {}", command, e);
            return CommandResponse.error(
                "Command failed: " + e.getMessage()
            );
        }
    }
    
    private String executeKubectlOperation(String namespace, KubectlCommand cmd) {
        // Implementation using Fabric8 client
        // Map kubectl commands to Fabric8 API calls
        return switch (cmd.getOperation()) {
            case "get" -> handleGetOperation(namespace, cmd);
            case "create" -> handleCreateOperation(namespace, cmd);
            case "delete" -> handleDeleteOperation(namespace, cmd);
            case "apply" -> handleApplyOperation(namespace, cmd);
            default -> "Unknown operation: " + cmd.getOperation();
        };
    }
}
```

## WebSocket Communication

### Backend WebSocket Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/terminal")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}

@Controller
@RequiredArgsConstructor
public class TerminalWebSocketController {
    
    private final CommandExecutorService commandExecutor;
    private final ValidationService validationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/terminal/command")
    @SendToUser("/queue/response")
    public CommandResponse handleCommand(
            @Payload CommandRequest request,
            Principal principal) {
        
        String namespace = getPlayerNamespace(principal.getName());
        CommandResponse response = commandExecutor.executeCommand(
            namespace,
            request.getCommand()
        );
        
        // Trigger async validation
        CompletableFuture.runAsync(() -> {
            ValidationResult result = validationService.validateCurrentChallenge(namespace);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/validation",
                result
            );
        });
        
        return response;
    }
}
```

### Frontend WebSocket Integration

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

/**
 * Custom hook for WebSocket connection management.
 */
export const useWebSocket = (namespace) => {
    const [stompClient, setStompClient] = useState(null);
    const [connected, setConnected] = useState(false);
    
    useEffect(() => {
        const socket = new SockJS('http://localhost:8080/ws/terminal');
        const client = Stomp.over(socket);
        
        client.connect({}, () => {
            setConnected(true);
            
            // Subscribe to command responses
            client.subscribe('/user/queue/response', (message) => {
                const response = JSON.parse(message.body);
                handleCommandResponse(response);
            });
            
            // Subscribe to validation updates
            client.subscribe('/user/queue/validation', (message) => {
                const result = JSON.parse(message.body);
                handleValidationResult(result);
            });
            
            // Subscribe to achievements
            client.subscribe('/user/queue/achievements', (message) => {
                const achievement = JSON.parse(message.body);
                showAchievementNotification(achievement);
            });
        });
        
        setStompClient(client);
        
        return () => {
            if (client && client.connected) {
                client.disconnect();
            }
        };
    }, [namespace]);
    
    const sendCommand = useCallback((command) => {
        if (stompClient && connected) {
            stompClient.send('/app/terminal/command', {}, JSON.stringify({
                command,
                namespace
            }));
        }
    }, [stompClient, connected, namespace]);
    
    return { connected, sendCommand };
};
```

## Browser Terminal UI (xterm.js)

### Terminal Component Implementation

```javascript
import { Terminal as XTerm } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm/css/xterm.css';

/**
 * Terminal component using xterm.js for browser-based terminal emulation.
 */
export const Terminal = ({ namespace, onCommandExecute }) => {
    const terminalRef = useRef(null);
    const xtermRef = useRef(null);
    const { connected, sendCommand } = useWebSocket(namespace);
    const [commandHistory, setCommandHistory] = useState([]);
    const [historyIndex, setHistoryIndex] = useState(-1);
    
    useEffect(() => {
        // Initialize xterm.js
        const term = new XTerm({
            cursorBlink: true,
            fontSize: 14,
            fontFamily: 'Menlo, Monaco, "Courier New", monospace',
            theme: {
                background: '#1e1e1e',
                foreground: '#d4d4d4',
                cursor: '#00ff00',
                selection: '#264f78',
                black: '#000000',
                red: '#cd3131',
                green: '#0dbc79',
                yellow: '#e5e510',
                blue: '#2472c8',
                magenta: '#bc3fbc',
                cyan: '#11a8cd',
                white: '#e5e5e5'
            }
        });
        
        const fitAddon = new FitAddon();
        term.loadAddon(fitAddon);
        term.open(terminalRef.current);
        fitAddon.fit();
        
        xtermRef.current = term;
        
        // Display welcome message
        term.writeln('\x1b[1;36m╔════════════════════════════════════════╗\x1b[0m');
        term.writeln('\x1b[1;36m║   Kubernetes Learning Game Terminal   ║\x1b[0m');
        term.writeln('\x1b[1;36m╚════════════════════════════════════════╝\x1b[0m');
        term.writeln('');
        term.write(getPrompt());
        
        // Handle input
        let currentLine = '';
        term.onData((data) => {
            const code = data.charCodeAt(0);
            
            if (code === 13) { // Enter
                term.writeln('');
                if (currentLine.trim()) {
                    executeCommand(currentLine);
                    setCommandHistory(prev => [...prev, currentLine]);
                    currentLine = '';
                }
                term.write(getPrompt());
            } else if (code === 127) { // Backspace
                if (currentLine.length > 0) {
                    currentLine = currentLine.slice(0, -1);
                    term.write('\b \b');
                }
            } else if (code === 27) { // Arrow keys
                // Handle command history navigation
            } else {
                currentLine += data;
                term.write(data);
            }
        });
        
        return () => {
            term.dispose();
        };
    }, []);
    
    const executeCommand = (command) => {
        const term = xtermRef.current;
        
        if (command.startsWith('game ')) {
            handleGameCommand(command);
        } else if (command.startsWith('kubectl ')) {
            sendCommand(command);
        } else {
            term.writeln(`\x1b[1;31mUnknown command: ${command}\x1b[0m`);
            term.writeln('Use "kubectl" for Kubernetes commands or "game" for game commands');
        }
        
        onCommandExecute?.(command);
    };
    
    const writeOutput = (text, color = 'white') => {
        const term = xtermRef.current;
        const colorCodes = {
            green: '\x1b[1;32m',
            red: '\x1b[1;31m',
            yellow: '\x1b[1;33m',
            cyan: '\x1b[1;36m',
            magenta: '\x1b[1;35m',
            white: '\x1b[0m'
        };
        term.writeln(`${colorCodes[color]}${text}\x1b[0m`);
    };
    
    const getPrompt = () => {
        return `\x1b[1;36m[${namespace}]\x1b[0m k8s-game> `;
    };
    
    return (
        <div className="terminal-container">
            <div ref={terminalRef} className="terminal" />
            {!connected && (
                <div className="connection-status">
                    Connecting to server...
                </div>
            )}
        </div>
    );
};
```

### Color Scheme Constants

```javascript
// utils/colorScheme.js
export const TerminalColors = {
    SUCCESS: '\x1b[1;32m',    // Green
    ERROR: '\x1b[1;31m',      // Red
    WARNING: '\x1b[1;33m',    // Yellow
    INFO: '\x1b[1;36m',       // Cyan
    STORY: '\x1b[1;35m',      // Magenta
    HINT: '\x1b[1;34m',       // Blue
    COMMAND: '\x1b[0m',       // White
    RESET: '\x1b[0m'
};

export const formatSuccess = (message) => 
    `${TerminalColors.SUCCESS}✓ ${message}${TerminalColors.RESET}`;

export const formatError = (message) => 
    `${TerminalColors.ERROR}✗ ${message}${TerminalColors.RESET}`;

export const formatStory = (message) => {
    const border = '═'.repeat(60);
    return `\n${TerminalColors.STORY}╔${border}╗\n${message}\n╚${border}╝${TerminalColors.RESET}\n`;
};
```

## State Management

### Backend Entity (JPA)

```java
@Entity
@Table(name = "game_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    
    @Column(nullable = false)
    private Integer currentLevel;
    
    @Column(nullable = false)
    private Integer currentChallenge;
    
    @Column(nullable = false)
    private String namespace;
    
    @Column
    private LocalDateTime sessionStart;
    
    @Column
    private Integer totalPlaytime = 0; // seconds
    
    @Column
    private LocalDateTime updatedAt;
    
    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Frontend State (React Context)

```javascript
import React, { createContext, useContext, useState, useEffect } from 'react';

const GameStateContext = createContext();

export const GameStateProvider = ({ children }) => {
    const [gameState, setGameState] = useState({
        playerId: null,
        currentLevel: 1,
        currentChallenge: 1,
        completedChallenges: [],
        achievements: [],
        hintsUsed: 0,
        commandsExecuted: 0,
        sessionStart: null,
        totalPlaytime: 0
    });
    
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
        // Load game state from API
        loadGameState();
    }, []);
    
    const loadGameState = async () => {
        try {
            const response = await fetch('/api/game/state', {
                headers: {
                    'Authorization': `Bearer ${getAuthToken()}`
                }
            });
            const data = await response.json();
            setGameState(data);
        } catch (error) {
            console.error('Failed to load game state:', error);
        } finally {
            setLoading(false);
        }
    };
    
    const saveGameState = async (updates) => {
        const newState = { ...gameState, ...updates };
        setGameState(newState);
        
        try {
            await fetch('/api/game/state', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getAuthToken()}`
                },
                body: JSON.stringify(newState)
            });
        } catch (error) {
            console.error('Failed to save game state:', error);
        }
    };
    
    return (
        <GameStateContext.Provider value={{ gameState, saveGameState, loading }}>
            {children}
        </GameStateContext.Provider>
    );
};

export const useGameState = () => {
    const context = useContext(GameStateContext);
    if (!context) {
        throw new Error('useGameState must be used within GameStateProvider');
    }
    return context;
};
```

## Testing Patterns

### Backend Tests (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {
    
    @Mock
    private KubernetesClient kubernetesClient;
    
    @Mock
    private ChallengeRepository challengeRepository;
    
    @InjectMocks
    private ValidationService validationService;
    
    @Test
    void testPodExistsValidation() {
        // Given
        String namespace = "test-namespace";
        String challengeId = "pod-basics-01";
        
        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setSuccessCriteria(List.of(
            Map.of("type", "pod_exists", "name", "test-pod")
        ));
        
        Pod mockPod = new PodBuilder()
            .withNewMetadata()
                .withName("test-pod")
            .endMetadata()
            .withNewStatus()
                .withPhase("Running")
            .endStatus()
            .build();
        
        when(challengeRepository.findById(challengeId))
            .thenReturn(Optional.of(challenge));
        when(kubernetesClient.pods().inNamespace(namespace).withName("test-pod").get())
            .thenReturn(mockPod);
        
        // When
        ValidationResult result = validationService.validateChallenge(namespace, challengeId);
        
        // Then
        assertTrue(result.isSuccess());
        verify(kubernetesClient).pods();
    }
}
```

### Frontend Tests (Jest + React Testing Library)

```javascript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Terminal } from './Terminal';
import { useWebSocket } from '../../hooks/useWebSocket';

jest.mock('../../hooks/useWebSocket');

describe('Terminal Component', () => {
    beforeEach(() => {
        useWebSocket.mockReturnValue({
            connected: true,
            sendCommand: jest.fn()
        });
    });
    
    test('renders terminal with welcome message', () => {
        render(<Terminal namespace="test-namespace" />);
        expect(screen.getByText(/Kubernetes Learning Game Terminal/i)).toBeInTheDocument();
    });
    
    test('executes kubectl command when entered', async () => {
        const mockSendCommand = jest.fn();
        useWebSocket.mockReturnValue({
            connected: true,
            sendCommand: mockSendCommand
        });
        
        render(<Terminal namespace="test-namespace" />);
        
        const input = screen.getByRole('textbox');
        fireEvent.change(input, { target: { value: 'kubectl get pods' } });
        fireEvent.keyPress(input, { key: 'Enter', code: 13 });
        
        await waitFor(() => {
            expect(mockSendCommand).toHaveBeenCalledWith('kubectl get pods');
        });
    });
});
```

## Performance Considerations

### Cluster State Caching (Backend)

Avoid excessive API calls by caching cluster state with Spring Cache:

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("clusterState");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(100));
        return cacheManager;
    }
}

@Service
@RequiredArgsConstructor
public class KubernetesService {
    
    private final KubernetesClient kubernetesClient;
    
    /**
     * Gets pods with caching (5 second TTL).
     */
    @Cacheable(value = "clusterState", key = "'pods_' + #namespace")
    public List<Pod> getPods(String namespace) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .list()
            .getItems();
    }
    
    /**
     * Invalidates cache for namespace.
     */
    @CacheEvict(value = "clusterState", key = "'pods_' + #namespace")
    public void invalidatePodsCache(String namespace) {
        // Cache automatically cleared
    }
}
```

### Frontend Performance Optimization

```javascript
import { memo, useMemo, useCallback } from 'react';

/**
 * Memoized terminal output component to prevent unnecessary re-renders.
 */
export const OutputDisplay = memo(({ output }) => {
    return (
        <div className="output">
            {output.map((line, index) => (
                <div key={index} className="output-line">
                    {line}
                </div>
            ))}
        </div>
    );
});

/**
 * Custom hook with memoized command history.
 */
export const useCommandHistory = () => {
    const [history, setHistory] = useState([]);
    const [index, setIndex] = useState(-1);
    
    const addCommand = useCallback((command) => {
        setHistory(prev => [...prev, command]);
        setIndex(-1);
    }, []);
    
    const getPrevious = useCallback(() => {
        if (index < history.length - 1) {
            const newIndex = index + 1;
            setIndex(newIndex);
            return history[history.length - 1 - newIndex];
        }
        return null;
    }, [history, index]);
    
    const getNext = useCallback(() => {
        if (index > 0) {
            const newIndex = index - 1;
            setIndex(newIndex);
            return history[history.length - 1 - newIndex];
        }
        setIndex(-1);
        return '';
    }, [history, index]);
    
    return { addCommand, getPrevious, getNext };
};
```

## Logging Strategy

### Backend Logging (Logback)

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/k8s-game.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/k8s-game-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.k8sgame" level="DEBUG"/>
    <logger name="io.fabric8.kubernetes" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="FILE"/>
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

```java
@Service
@Slf4j
public class GameEngineService {
    
    public void startGame(String playerId) {
        log.info("Starting game for player: {}", playerId);
        try {
            // Game logic
            log.debug("Game state initialized for player: {}", playerId);
        } catch (Exception e) {
            log.error("Failed to start game for player: {}", playerId, e);
            throw new GameInitializationException("Could not start game", e);
        }
    }
}
```

### Frontend Logging

```javascript
// utils/logger.js
const LOG_LEVELS = {
    DEBUG: 0,
    INFO: 1,
    WARN: 2,
    ERROR: 3
};

const currentLevel = process.env.NODE_ENV === 'production' 
    ? LOG_LEVELS.WARN 
    : LOG_LEVELS.DEBUG;

export const logger = {
    debug: (...args) => {
        if (currentLevel <= LOG_LEVELS.DEBUG) {
            console.log('[DEBUG]', new Date().toISOString(), ...args);
        }
    },
    
    info: (...args) => {
        if (currentLevel <= LOG_LEVELS.INFO) {
            console.info('[INFO]', new Date().toISOString(), ...args);
        }
    },
    
    warn: (...args) => {
        if (currentLevel <= LOG_LEVELS.WARN) {
            console.warn('[WARN]', new Date().toISOString(), ...args);
        }
    },
    
    error: (...args) => {
        if (currentLevel <= LOG_LEVELS.ERROR) {
            console.error('[ERROR]', new Date().toISOString(), ...args);
        }
    }
};
```

## Configuration Management

### Backend Configuration (Spring Boot)

```yaml
# application.yml
spring:
  application:
    name: k8s-game-backend
  datasource:
    url: jdbc:postgresql://localhost:5432/k8sgame
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

kubernetes:
  config-path: ${KUBECONFIG:~/.kube/config}
  namespace-prefix: k8s-game-
  resource-quotas:
    pods: 10
    services: 5
    pvc: 3
    cpu: "2"
    memory: "4Gi"

game:
  session-timeout: 3600
  max-hints-per-challenge: 3
  validation-timeout: 30

logging:
  level:
    com.k8sgame: DEBUG
    io.fabric8: INFO
```

```java
@Configuration
@ConfigurationProperties(prefix = "game")
@Data
public class GameConfig {
    private Integer sessionTimeout;
    private Integer maxHintsPerChallenge;
    private Integer validationTimeout;
}
```

### Frontend Configuration

```javascript
// config/environment.js
const config = {
    development: {
        apiBaseUrl: 'http://localhost:8080',
        wsEndpoint: 'http://localhost:8080/ws/terminal',
        logLevel: 'DEBUG'
    },
    production: {
        apiBaseUrl: process.env.REACT_APP_API_URL,
        wsEndpoint: process.env.REACT_APP_WS_URL,
        logLevel: 'WARN'
    }
};

export const getConfig = () => {
    const env = process.env.NODE_ENV || 'development';
    return config[env];
};
```
