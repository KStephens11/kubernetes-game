# Technical Stack

## Core Technologies

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Maven or Gradle
- **Kubernetes Client**: Fabric8 Kubernetes Client
- **WebSocket**: Spring WebSocket for real-time command execution
- **Database**: PostgreSQL or H2 (for development) - player progress and state
- **Security**: Spring Security for authentication/authorization

### Frontend
- **Framework**: React 18+
- **Language**: JavaScript/TypeScript
- **Build Tool**: Vite or Create React App
- **UI Library**: Material-UI or Tailwind CSS
- **Terminal Emulator**: xterm.js for browser-based terminal
- **State Management**: React Context API or Redux
- **HTTP Client**: Axios or Fetch API
- **WebSocket Client**: SockJS + STOMP for real-time communication

### Runtime Environment
- **Platform**: Browser-based application (Chrome, Firefox, Safari, Edge)
- **Kubernetes Integration**: Real cluster connectivity via kubeconfig (server-side)
- **kubectl**: Commands executed server-side, results streamed to browser

### Key Technical Requirements

- Kubernetes cluster access (local or remote) on server
- Valid kubeconfig credentials on server
- Namespace isolation for safe multi-player support
- Resource quota management
- State persistence across sessions (database)
- WebSocket connection for real-time command execution
- Session management and authentication

## Architecture Components

### Backend Services (Spring Boot)

#### Game Engine Service
- Game state management and progression logic
- Session handling and save/load functionality
- Command routing (game commands vs kubectl commands)
- RESTful API endpoints for game operations

#### Validation Service
- Cluster state verification against challenge criteria
- Success/failure detection
- Alternative solution recognition
- Asynchronous validation with WebSocket notifications

#### Narrative Service
- Story progression and context delivery
- Level transitions and plot advancement
- Scenario-based storytelling
- Challenge content management

#### Hint Service
- Progressive hint delivery (general → specific)
- Contextual kubectl examples
- Kubernetes concept explanations

#### Progress Service
- Challenge and level completion tracking
- Achievement system
- Player statistics and session metrics
- Database persistence

#### Kubernetes Service
- Fabric8 client wrapper
- Namespace management and isolation
- Command execution (kubectl operations)
- Safety guard for dangerous operations
- Resource quota enforcement

### Frontend Components (React)

#### Terminal Component
- xterm.js integration for terminal emulation
- Command input and output display
- WebSocket connection for real-time updates
- Command history and autocomplete

#### Game UI Components
- Story/narrative display
- Challenge objectives panel
- Hint system interface
- Progress tracker dashboard
- Achievement notifications

#### State Management
- Player session state
- Current challenge state
- WebSocket connection state
- UI state (modals, panels, themes)

## Development Guidelines

### Kubernetes Concepts Coverage

Challenges must cover:
- Pods, Deployments, Services
- ConfigMaps and Secrets
- Persistent Volumes
- Debugging failing resources
- Scaling and updating applications
- Networking and service discovery
- Resource management and optimization

### Safety Constraints

- **Namespace Isolation**: All player operations restricted to dedicated namespaces
- **Resource Quotas**: Prevent cluster resource exhaustion
- **Dangerous Operation Blocking**: Prevent deletion of critical resources
- **Clean Reset Mechanism**: Restore namespace to known state

### User Experience Standards

- **Terminal Interface**: Polished CLI with clear visual hierarchy
- **Color Coding**: Highlight important information and game states
- **Error Messages**: Helpful, educational feedback on failures
- **Command Prompts**: Clear indication of current state and available actions

## Common Commands

### Game Commands (Browser Terminal)
```bash
# Initialize new game (creates namespace, sets up initial resources)
game init

# Resume existing game (loads saved state)
game resume

# Reset current level (restores namespace to level start state)
game reset

# View current challenge
game status

# Request hint
game hint

# View progress and achievements
game progress

# Save and exit
game save
game exit
```

### kubectl Operations (Browser Terminal)
All standard kubectl commands are executed server-side within player namespace:
```bash
kubectl get pods
kubectl describe deployment <name>
kubectl apply -f <file>
kubectl logs <pod>
kubectl exec -it <pod> -- /bin/sh
kubectl create deployment <name> --image=<image>
kubectl scale deployment <name> --replicas=3
kubectl expose deployment <name> --port=80
```

## API Endpoints

### REST API
```
POST   /api/auth/login          - Authenticate player
POST   /api/auth/register       - Register new player
GET    /api/game/state          - Get current game state
POST   /api/game/init           - Initialize new game
POST   /api/game/reset          - Reset current level
GET    /api/challenges/current  - Get current challenge
GET    /api/hints/next          - Get next hint
GET    /api/progress            - Get player progress
POST   /api/achievements        - Award achievement
```

### WebSocket Endpoints
```
/ws/terminal                     - Terminal command execution
/topic/validation                - Challenge validation updates
/topic/narrative                 - Story progression events
/topic/achievements              - Achievement notifications
```

## Build and Run Commands

### Backend (Spring Boot)
```bash
# Build with Maven
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Package as JAR
mvn package

# Run packaged application
java -jar target/k8s-game-backend-0.0.1-SNAPSHOT.jar
```

### Frontend (React)
```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Run tests
npm test

# Lint code
npm run lint
```

### Docker (Optional)
```bash
# Build backend image
docker build -t k8s-game-backend ./backend

# Build frontend image
docker build -t k8s-game-frontend ./frontend

# Run with docker-compose
docker-compose up
```

## Testing Approach

### Backend Tests (JUnit 5 + Mockito)
- **Unit Tests**: Service layer logic, validation rules
- **Integration Tests**: REST API endpoints, WebSocket connections
- **Kubernetes Client Tests**: Mock Fabric8 client operations
- **Security Tests**: Authentication and authorization flows

### Frontend Tests (Jest + React Testing Library)
- **Component Tests**: UI component rendering and interactions
- **Integration Tests**: WebSocket communication, API calls
- **E2E Tests**: Full user flows with Cypress or Playwright

### Test Coverage Areas
- **Cluster State Validation**: Verify challenge success criteria detection
- **Namespace Isolation**: Ensure player operations don't affect other namespaces
- **Error Handling**: Test graceful recovery from cluster connectivity issues
- **Progress Persistence**: Verify save/load functionality across sessions
- **Command Parsing**: Test game command vs kubectl command routing
- **WebSocket Communication**: Test real-time command execution and updates
- **Concurrent Users**: Test multiple players with isolated namespaces
