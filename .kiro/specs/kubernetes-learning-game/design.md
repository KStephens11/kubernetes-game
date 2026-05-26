# Design Document: Kubernetes Learning Game (MVP)

## Overview

The Kubernetes Learning Game MVP is a simplified, browser-based educational game that teaches Kubernetes through real kubectl command execution. This design focuses on delivering core value quickly: an interactive terminal where players solve challenges by running actual kubectl commands against a real cluster.

### MVP Philosophy

This design prioritizes:
- **Speed to working prototype**: Simple implementations over complex abstractions
- **Core gameplay loop**: Challenge → Command → Validation → Progression
- **Minimal infrastructure**: Local cluster (Minikube/Kind) sufficient
- **Essential features only**: No elaborate achievement systems or complex narratives initially
- **In-memory state where possible**: Reduce database complexity for MVP

### What's Included in MVP

- 3-5 levels with 2-3 challenges each (~15 total challenges)
- Basic terminal interface with xterm.js
- Real kubectl command execution via WebSocket
- Simple validation system checking cluster state
- Basic progress tracking (current level/challenge)
- Simple hint system (1-2 hints per challenge)
- Namespace isolation for safety

### What's Deferred Post-MVP

- Complex achievement system
- Elaborate narrative engine
- Multi-player support
- Advanced analytics and statistics
- Sophisticated difficulty scaling
- Comprehensive authentication (use simple session-based auth)
- Database persistence (use in-memory for MVP)

## Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  React Frontend                                       │  │
│  │  ├─ Terminal Component (xterm.js)                    │  │
│  │  ├─ Challenge Display                                │  │
│  │  └─ Simple Progress Indicator                        │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│                    WebSocket + REST                          │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                   Spring Boot Backend                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Controllers                                            │ │
│  │  ├─ TerminalWebSocketController (command execution)    │ │
│  │  ├─ GameController (REST: state, challenges)           │ │
│  │  └─ HintController (REST: hints)                       │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Services (Core Logic)                                  │ │
│  │  ├─ GameService (state management, progression)        │ │
│  │  ├─ CommandService (kubectl execution)                 │ │
│  │  ├─ ValidationService (challenge completion check)     │ │
│  │  └─ ChallengeLoader (load from YAML files)             │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  In-Memory State                                        │ │
│  │  ├─ ConcurrentHashMap<sessionId, GameState>            │ │
│  │  └─ Challenge definitions (loaded at startup)          │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                   │
│                   Fabric8 Client                             │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│              Kubernetes Cluster (Minikube/Kind)              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Player Namespaces (isolated per session)              │ │
│  │  ├─ game-session-abc123                                │ │
│  │  ├─ game-session-def456                                │ │
│  │  └─ ...                                                 │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Simplified Component Interactions

```
Player enters command in browser terminal
           ↓
WebSocket sends command to backend
           ↓
CommandService validates safety (namespace check)
           ↓
CommandService executes kubectl via Fabric8
           ↓
Result streamed back to browser via WebSocket
           ↓
ValidationService checks if challenge complete
           ↓
If complete: GameService advances to next challenge
           ↓
Update sent to browser (new challenge or completion message)
```

## Components and Interfaces

### Backend Components

#### 1. GameService (Core State Management)

**Responsibility**: Manage game state, progression, and session lifecycle

**Key Methods**:
```java
public class GameService {
    // In-memory state storage
    private final ConcurrentHashMap<String, GameState> sessions;
    
    // Initialize new game session
    GameState startGame(String sessionId);
    
    // Get current challenge for session
    Challenge getCurrentChallenge(String sessionId);
    
    // Advance to next challenge
    void advanceChallenge(String sessionId);
    
    // Get current game state
    GameState getGameState(String sessionId);
}
```

**Implementation Notes**:
- Use `ConcurrentHashMap` for thread-safe in-memory storage
- Session ID generated on first connection (UUID)
- No database persistence in MVP - state lost on restart
- Simple progression: linear challenge sequence

#### 2. CommandService (kubectl Execution)

**Responsibility**: Execute kubectl commands safely against cluster

**Key Methods**:
```java
public class CommandService {
    private final KubernetesClient k8sClient;
    
    // Execute command in player's namespace
    CommandResult executeCommand(String sessionId, String command);
    
    // Parse kubectl command string
    private KubectlCommand parseCommand(String command);
    
    // Safety check before execution
    private boolean isSafeCommand(String namespace, KubectlCommand cmd);
}
```

**Implementation Notes**:
- Use Fabric8 KubernetesClient for cluster operations
- Parse command string to extract operation and resource type
- Block dangerous operations (delete namespace, cluster-wide operations)
- All operations scoped to session-specific namespace

#### 3. ValidationService (Challenge Completion)

**Responsibility**: Check if cluster state meets challenge success criteria

**Key Methods**:
```java
public class ValidationService {
    private final KubernetesClient k8sClient;
    
    // Check if challenge is complete
    ValidationResult validateChallenge(String namespace, Challenge challenge);
    
    // Check specific criterion
    private boolean checkCriterion(String namespace, SuccessCriterion criterion);
}
```

**Implementation Notes**:
- Query cluster state after each command
- Compare against challenge success criteria
- Simple criteria types: pod_exists, pod_running, service_exists, deployment_ready
- Return boolean + feedback message

#### 4. ChallengeLoader (Content Management)

**Responsibility**: Load challenge definitions from YAML files

**Key Methods**:
```java
public class ChallengeLoader {
    // Load all challenges at startup
    List<Challenge> loadChallenges();
    
    // Get challenge by ID
    Challenge getChallengeById(String id);
}
```

**Implementation Notes**:
- Load from `src/main/resources/challenges/` at application startup
- Parse YAML files into Challenge objects
- Store in memory (no database)
- Simple structure: level number + challenge number

#### 5. NamespaceService (Isolation)

**Responsibility**: Create and manage player-specific namespaces

**Key Methods**:
```java
public class NamespaceService {
    // Create namespace for session
    String createSessionNamespace(String sessionId);
    
    // Setup initial resources for challenge
    void setupChallengeResources(String namespace, Challenge challenge);
    
    // Clean up namespace
    void cleanupNamespace(String namespace);
}
```

**Implementation Notes**:
- Namespace naming: `game-session-{sessionId}`
- Apply resource quotas (10 pods, 2 CPU, 4Gi memory)
- Create initial resources defined in challenge YAML
- Cleanup on session end or timeout

### Frontend Components

#### 1. Terminal Component

**Responsibility**: Browser-based terminal using xterm.js

**Key Features**:
- Command input with history (up/down arrows)
- Colored output (success=green, error=red, info=cyan)
- WebSocket connection for real-time command execution
- Display kubectl output

**Implementation**:
```javascript
export const Terminal = ({ sessionId }) => {
    const [output, setOutput] = useState([]);
    const { sendCommand, connected } = useWebSocket(sessionId);
    
    const handleCommand = (cmd) => {
        if (cmd.startsWith('kubectl ')) {
            sendCommand(cmd);
        } else if (cmd === 'hint') {
            fetchHint();
        } else if (cmd === 'status') {
            fetchStatus();
        }
    };
    
    return <div ref={terminalRef} />;
};
```

#### 2. ChallengePanel Component

**Responsibility**: Display current challenge information

**Key Features**:
- Challenge title and description
- Success criteria checklist
- Simple progress indicator (Challenge X of Y)

**Implementation**:
```javascript
export const ChallengePanel = ({ challenge, progress }) => {
    return (
        <div className="challenge-panel">
            <h2>{challenge.title}</h2>
            <p>{challenge.description}</p>
            <ul>
                {challenge.criteria.map(c => (
                    <li key={c.id}>
                        {c.complete ? '✓' : '○'} {c.description}
                    </li>
                ))}
            </ul>
            <div>Challenge {progress.current} of {progress.total}</div>
        </div>
    );
};
```

#### 3. useWebSocket Hook

**Responsibility**: Manage WebSocket connection for command execution

**Implementation**:
```javascript
export const useWebSocket = (sessionId) => {
    const [stompClient, setStompClient] = useState(null);
    const [connected, setConnected] = useState(false);
    
    useEffect(() => {
        const socket = new SockJS('/ws/terminal');
        const client = Stomp.over(socket);
        
        client.connect({}, () => {
            setConnected(true);
            client.subscribe(`/user/queue/output`, handleOutput);
            client.subscribe(`/user/queue/validation`, handleValidation);
        });
        
        setStompClient(client);
        return () => client.disconnect();
    }, [sessionId]);
    
    const sendCommand = (command) => {
        stompClient.send('/app/command', {}, JSON.stringify({
            sessionId,
            command
        }));
    };
    
    return { sendCommand, connected };
};
```

## Data Models

### Core Data Structures (Simplified)

#### GameState (In-Memory)

```java
public class GameState {
    private String sessionId;
    private String namespace;
    private int currentLevel;
    private int currentChallenge;
    private List<String> completedChallenges;
    private int hintsUsed;
    private LocalDateTime sessionStart;
    
    // Simple getters/setters
}
```

#### Challenge (Loaded from YAML)

```java
public class Challenge {
    private String id;              // "level-1-challenge-1"
    private int level;
    private int order;
    private String title;
    private String description;
    private String storyContext;    // Simple 2-3 sentence context
    private List<SuccessCriterion> successCriteria;
    private List<String> hints;
    private List<InitialResource> initialResources;
}
```

#### SuccessCriterion

```java
public class SuccessCriterion {
    private String type;            // pod_exists, pod_running, service_exists
    private String resourceName;
    private Map<String, String> additionalParams;
}
```

#### CommandResult

```java
public class CommandResult {
    private boolean success;
    private String output;
    private String error;
    private boolean challengeComplete;
}
```

### Challenge Definition Format (YAML)

```yaml
id: level-1-challenge-1
level: 1
order: 1
title: "Create Your First Pod"
description: "Deploy a simple nginx pod to get familiar with kubectl"

story_context: |
  Welcome to the Kubernetes cluster. Your first task is to deploy
  a web server pod. Use kubectl to create a pod named 'web-server'
  running nginx.

success_criteria:
  - type: pod_exists
    resource_name: web-server
  - type: pod_running
    resource_name: web-server

hints:
  - "Use the 'kubectl run' command to create a pod"
  - "Try: kubectl run web-server --image=nginx"

initial_resources: []
```

## API Design

### REST Endpoints (Simplified)

```
GET  /api/game/start
     → Returns: { sessionId, namespace, firstChallenge }
     
GET  /api/game/state/{sessionId}
     → Returns: { currentLevel, currentChallenge, progress }
     
GET  /api/challenges/current/{sessionId}
     → Returns: Challenge object
     
GET  /api/hints/{sessionId}
     → Returns: { hint, hintsRemaining }
     
POST /api/game/reset/{sessionId}
     → Resets namespace to challenge start state
```

### WebSocket Endpoints

```
/ws/terminal                          - Connection endpoint

Client → Server:
/app/command
  { sessionId, command }

Server → Client:
/user/queue/output
  { output, error, timestamp }
  
/user/queue/validation
  { challengeComplete, nextChallenge, message }
```

## Validation Logic

### Validation Flow

```
1. Player executes kubectl command
2. Command executed against cluster
3. ValidationService triggered automatically
4. Check each success criterion:
   - pod_exists: Check if pod with name exists in namespace
   - pod_running: Check if pod status is "Running"
   - service_exists: Check if service exists
   - deployment_ready: Check if deployment has desired replicas ready
5. If all criteria met:
   - Mark challenge complete
   - Load next challenge
   - Send update to client
6. If not complete:
   - Send feedback on what's still needed
```

### Validation Implementation

```java
public ValidationResult validateChallenge(String namespace, Challenge challenge) {
    List<String> incomplete = new ArrayList<>();
    
    for (SuccessCriterion criterion : challenge.getSuccessCriteria()) {
        boolean met = switch (criterion.getType()) {
            case "pod_exists" -> checkPodExists(namespace, criterion.getResourceName());
            case "pod_running" -> checkPodRunning(namespace, criterion.getResourceName());
            case "service_exists" -> checkServiceExists(namespace, criterion.getResourceName());
            case "deployment_ready" -> checkDeploymentReady(namespace, criterion.getResourceName());
            default -> false;
        };
        
        if (!met) {
            incomplete.add(criterion.getDescription());
        }
    }
    
    boolean complete = incomplete.isEmpty();
    String message = complete 
        ? "Challenge complete! Moving to next challenge..."
        : "Still needed: " + String.join(", ", incomplete);
    
    return new ValidationResult(complete, message);
}
```

### Safety Validation

```java
private boolean isSafeCommand(String namespace, KubectlCommand cmd) {
    // Block cluster-wide operations
    if (cmd.hasClusterScope()) {
        return false;
    }
    
    // Block operations outside player namespace
    if (!cmd.getNamespace().equals(namespace)) {
        return false;
    }
    
    // Block dangerous resource types
    Set<String> blocked = Set.of("namespace", "persistentvolume", 
                                  "clusterrole", "clusterrolebinding");
    if (blocked.contains(cmd.getResourceType())) {
        return false;
    }
    
    return true;
}
```

## Technology Choices (MVP Justification)

### Backend: Spring Boot + Fabric8

**Why Spring Boot**:
- Fast setup with Spring Initializr
- Built-in WebSocket support
- Minimal configuration needed
- Good for MVP iteration speed

**Why Fabric8 Kubernetes Client**:
- Java-native Kubernetes client
- Simpler than shelling out to kubectl binary
- Type-safe API
- Good documentation

**Alternative Considered**: Go + client-go
- Rejected for MVP: Requires more setup, team may be more familiar with Java/Spring

### Frontend: React + xterm.js

**Why React**:
- Fast component development
- Large ecosystem
- Team familiarity likely

**Why xterm.js**:
- De facto standard for browser terminals
- Good documentation
- Handles ANSI colors and formatting

**Simplified State**: React Context API instead of Redux
- Redux overkill for MVP
- Context API sufficient for simple state

### No Database for MVP

**Why In-Memory**:
- Faster development (no schema migrations)
- Simpler deployment (no DB to configure)
- Acceptable for MVP (state loss on restart is okay)
- Easy to add persistence later

**Post-MVP**: Add H2 or PostgreSQL when persistence needed

### Local Cluster (Minikube/Kind)

**Why Local**:
- No cloud costs
- Faster iteration
- Simpler setup for developers
- Good enough for MVP testing

**Post-MVP**: Support remote clusters, cloud providers

## Challenge Content Strategy (MVP)

### Level 1: Pod Basics (3 challenges)

1. **Create a Pod**: Use `kubectl run` to create a simple pod
2. **Inspect a Pod**: Use `kubectl describe` and `kubectl logs`
3. **Delete and Recreate**: Practice pod lifecycle

### Level 2: Deployments (3 challenges)

1. **Create Deployment**: Use `kubectl create deployment`
2. **Scale Deployment**: Use `kubectl scale`
3. **Update Deployment**: Change image version

### Level 3: Services (3 challenges)

1. **Expose Deployment**: Create a service with `kubectl expose`
2. **Service Discovery**: Verify service endpoints
3. **Port Forwarding**: Use `kubectl port-forward`

### Level 4: ConfigMaps & Secrets (3 challenges)

1. **Create ConfigMap**: Store configuration data
2. **Use ConfigMap in Pod**: Mount as environment variable
3. **Create Secret**: Store sensitive data

### Level 5: Debugging (3 challenges)

1. **Fix Broken Pod**: Pod with wrong image
2. **Fix Broken Service**: Service with wrong selector
3. **Resource Limits**: Pod failing due to resource constraints

**Total**: 15 challenges, estimated 45-60 minutes gameplay

## Error Handling

### Backend Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(KubernetesClientException.class)
    public ResponseEntity<ErrorResponse> handleK8sError(KubernetesClientException ex) {
        return ResponseEntity.status(503)
            .body(new ErrorResponse("Cluster connection failed. Is Minikube running?"));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadCommand(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Invalid command: " + ex.getMessage()));
    }
}
```

### Frontend Error Handling

```javascript
// Simple error display in terminal
const displayError = (error) => {
    terminal.writeln(`\x1b[1;31m✗ Error: ${error.message}\x1b[0m`);
    
    if (error.type === 'CONNECTION_LOST') {
        terminal.writeln('\x1b[1;33mReconnecting...\x1b[0m');
        reconnectWebSocket();
    }
};
```

## Testing Strategy

### Why Property-Based Testing Is Not Applicable

Property-based testing (PBT) is **not appropriate** for this feature because:

1. **Infrastructure Operations**: The core functionality involves Kubernetes cluster operations (creating pods, services, deployments), which are external service interactions, not pure functions with testable properties.

2. **Side-Effect Heavy**: Most operations have side effects (creating/deleting cluster resources) rather than pure input/output transformations.

3. **External Dependencies**: Behavior depends on Kubernetes API responses, cluster state, and network communication—not deterministic based solely on inputs.

4. **UI Rendering**: Terminal interface rendering and WebSocket communication don't have universal properties that hold across all inputs.

5. **Integration-Focused**: The value is in integration between components (browser ↔ backend ↔ cluster), not in algorithmic correctness of pure functions.

**Appropriate Testing Approaches**:
- **Unit tests** for pure logic (command parsing, safety validation)
- **Integration tests** with mocked Kubernetes client
- **E2E tests** for full gameplay flow
- **Manual testing** with real cluster

### Backend Testing (Simplified for MVP)

**Unit Tests** (JUnit 5 + Mockito):

```java
// Test command parsing logic
@Test
void testParseKubectlCommand() {
    KubectlCommand cmd = CommandService.parse("kubectl get pods");
    assertEquals("get", cmd.getOperation());
    assertEquals("pods", cmd.getResourceType());
}

// Test safety validation logic
@Test
void testBlockDangerousCommands() {
    assertFalse(safetyValidator.isSafe("kubectl delete namespace kube-system"));
    assertTrue(safetyValidator.isSafe("kubectl get pods -n game-session-123"));
}

// Test validation criterion checking
@Test
void testPodExistsCriterion() {
    when(k8sClient.pods().inNamespace("test").withName("web").get())
        .thenReturn(mockPod);
    
    boolean met = validationService.checkPodExists("test", "web");
    assertTrue(met);
}
```

**Integration Tests** (with mocked Kubernetes client):

```java
@SpringBootTest
@AutoConfigureMockMvc
class GameIntegrationTest {
    
    @Test
    void testCommandExecutionFlow() {
        // Start game session
        String sessionId = gameService.startGame();
        
        // Execute command
        CommandResult result = commandService.execute(sessionId, "kubectl get pods");
        
        // Verify result
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
    }
}
```

**Manual Testing**:
- End-to-end gameplay with real Minikube cluster
- Test all 15 challenges sequentially
- Verify namespace isolation
- Test error scenarios (cluster down, invalid commands)

### Frontend Testing (Simplified for MVP)

**Component Tests** (React Testing Library + Jest):

```javascript
describe('Terminal Component', () => {
    test('renders terminal interface', () => {
        render(<Terminal sessionId="test-123" />);
        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });
    
    test('sends command via WebSocket', async () => {
        const mockSend = jest.fn();
        useWebSocket.mockReturnValue({ sendCommand: mockSend, connected: true });
        
        render(<Terminal sessionId="test-123" />);
        fireEvent.change(input, { target: { value: 'kubectl get pods' } });
        fireEvent.submit(form);
        
        expect(mockSend).toHaveBeenCalledWith('kubectl get pods');
    });
});
```

**Integration Tests**:
- WebSocket connection and reconnection
- Command/response flow
- Challenge progression updates

**Manual Testing**:
- Browser compatibility (Chrome, Firefox, Safari)
- Terminal input/output formatting
- WebSocket stability over long sessions
- Error message display

### MVP Testing Priorities

1. **Critical** (Must work for MVP):
   - Command execution against real cluster
   - Validation correctly detects challenge completion
   - Namespace isolation prevents cross-session interference
   - WebSocket connection stable

2. **Important** (Should work for MVP):
   - Error handling for common failures
   - Hint system functional
   - Progress tracking accurate

3. **Nice to Have** (Can defer post-MVP):
   - Comprehensive edge case coverage
   - Performance testing under load
   - Security penetration testing

### Test Coverage Goals (MVP)

- **Backend unit tests**: 70%+ coverage of service layer
- **Backend integration tests**: All REST endpoints, WebSocket handlers
- **Frontend component tests**: All major components render correctly
- **Manual E2E tests**: Complete playthrough of all 15 challenges

**Post-MVP**: Add automated E2E tests with Cypress, load testing, security audits

## Deployment (MVP)

### Local Development Setup

```bash
# Start Minikube
minikube start

# Build backend
cd backend
mvn clean package

# Run backend
java -jar target/k8s-game-0.0.1-SNAPSHOT.jar

# Run frontend (separate terminal)
cd frontend
npm install
npm run dev
```

### Simple Docker Deployment (Optional)

```dockerfile
# Backend Dockerfile
FROM openjdk:17-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```dockerfile
# Frontend Dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
CMD ["npm", "run", "preview"]
```

### MVP Deployment Strategy

- **Development**: Run locally with Minikube
- **Demo**: Docker Compose with both services
- **Post-MVP**: Kubernetes deployment, cloud hosting

## Security Considerations (MVP)

### Namespace Isolation

- Each session gets unique namespace
- All kubectl commands scoped to session namespace
- Resource quotas prevent resource exhaustion

### Command Safety

- Whitelist allowed kubectl operations
- Block cluster-wide operations
- Block dangerous resource types
- Validate namespace in every command

### Session Management

- Simple session ID (UUID)
- No authentication in MVP (single-user assumption)
- Session timeout after 1 hour of inactivity

**Post-MVP**: Add proper authentication, multi-user support

## Performance Considerations (MVP)

### Backend

- In-memory state (fast access)
- Connection pooling for Kubernetes client
- Simple caching for challenge definitions

### Frontend

- Lazy load challenges (fetch on demand)
- Debounce validation checks
- Limit terminal output history (last 1000 lines)

### Cluster

- Resource quotas per namespace
- Limit concurrent sessions (start with 10)

**Post-MVP**: Add metrics, monitoring, horizontal scaling

## MVP Success Criteria

### Functional Requirements

- ✓ Player can execute kubectl commands in browser terminal
- ✓ Commands execute against real Kubernetes cluster
- ✓ Validation detects challenge completion
- ✓ Player progresses through 15 challenges
- ✓ Hints available for each challenge
- ✓ Namespace isolation prevents interference

### Non-Functional Requirements

- ✓ Setup time < 10 minutes (Minikube + run app)
- ✓ Command execution latency < 2 seconds
- ✓ WebSocket connection stable for 1 hour session
- ✓ Works in Chrome and Firefox

### MVP Completion Definition

MVP is complete when:
1. All 15 challenges playable end-to-end
2. Command execution and validation working
3. Basic error handling in place
4. Can demo to stakeholders without crashes

## Post-MVP Roadmap

### Phase 2: Persistence & Polish

- Add database for progress persistence
- Improve narrative/story elements
- Add more challenges (30+ total)
- Better error messages and hints

### Phase 3: Multi-User & Auth

- User authentication
- Multiple concurrent players
- Leaderboards
- Achievement system

### Phase 4: Advanced Features

- Challenge editor
- Custom scenarios
- Team/classroom mode
- Analytics dashboard

## Appendix: Simplified File Structure

```
backend/
├── src/main/java/com/k8sgame/
│   ├── K8sGameApplication.java
│   ├── config/
│   │   ├── WebSocketConfig.java
│   │   └── KubernetesConfig.java
│   ├── controller/
│   │   ├── GameController.java
│   │   ├── HintController.java
│   │   └── TerminalWebSocketController.java
│   ├── service/
│   │   ├── GameService.java
│   │   ├── CommandService.java
│   │   ├── ValidationService.java
│   │   ├── ChallengeLoader.java
│   │   └── NamespaceService.java
│   └── model/
│       ├── GameState.java
│       ├── Challenge.java
│       ├── SuccessCriterion.java
│       └── CommandResult.java
├── src/main/resources/
│   ├── application.yml
│   └── challenges/
│       ├── level-1-challenge-1.yaml
│       ├── level-1-challenge-2.yaml
│       └── ...
└── pom.xml

frontend/
├── src/
│   ├── components/
│   │   ├── Terminal.jsx
│   │   ├── ChallengePanel.jsx
│   │   └── GameLayout.jsx
│   ├── hooks/
│   │   └── useWebSocket.js
│   ├── services/
│   │   └── api.js
│   └── App.jsx
├── package.json
└── vite.config.js
```

**Key Simplifications**:
- No separate entity/dto/repository layers (in-memory only)
- No complex service hierarchy
- Minimal configuration files
- Flat component structure
- No elaborate state management

This MVP design delivers the core value proposition—learning Kubernetes through real command execution—while avoiding over-engineering and enabling rapid iteration.
