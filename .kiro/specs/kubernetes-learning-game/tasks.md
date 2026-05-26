# Implementation Plan: Kubernetes Learning Game MVP

## Overview

This implementation plan breaks down the Kubernetes Learning Game MVP into discrete, actionable coding tasks. The approach follows an incremental development strategy: establish core infrastructure first, build backend services, implement frontend components, create challenge content, and finally integrate and test the complete system.

The MVP focuses on delivering the core gameplay loop (Challenge → Command → Validation → Progression) with 15 challenges across 5 levels, using real kubectl command execution against a Kubernetes cluster.

## Tasks

- [x] 1. Project setup and infrastructure
  - [x] 1.1 Initialize Spring Boot backend project
    - Create Spring Boot 3.x project with Maven
    - Add dependencies: Spring Web, Spring WebSocket, Fabric8 Kubernetes Client, Lombok
    - Configure application.yml with basic settings
    - Create main application class with @SpringBootApplication
    - _Requirements: 1.1, 12.1_
  
  - [x] 1.2 Initialize React frontend project
    - Create React 18+ project with Vite
    - Add dependencies: xterm.js, SockJS, STOMP, Axios
    - Configure Vite for development and production builds
    - Set up basic project structure (components, services, hooks directories)
    - _Requirements: 6.1, 6.2_
  
  - [x] 1.3 Configure Kubernetes client connection
    - Create KubernetesConfig class to initialize Fabric8 client
    - Load kubeconfig from file or environment variable
    - Implement cluster connectivity verification
    - Add error handling for cluster connection failures
    - _Requirements: 1.1, 1.3_
  
  - [x] 1.4 Set up WebSocket configuration
    - Create WebSocketConfig class with STOMP message broker
    - Configure endpoints: /ws/terminal for connection
    - Set up message destinations: /app for client-to-server, /topic and /queue for server-to-client
    - Enable SockJS fallback for browser compatibility
    - _Requirements: 6.1_

- [x] 2. Backend core services - Namespace and safety
  - [x] 2.1 Implement NamespaceService for player isolation
    - Create service to generate unique namespace names (game-session-{sessionId})
    - Implement namespace creation with labels (app=k8s-game, session-id)
    - Apply resource quotas (10 pods, 5 services, 2 CPU, 4Gi memory)
    - Implement namespace cleanup method
    - _Requirements: 1.5, 9.1, 9.3, 12.1_
  
  - [x] 2.2 Write unit tests for NamespaceService
    - Test namespace name generation
    - Test resource quota application
    - Mock Kubernetes client for isolated testing
    - _Requirements: 9.1, 9.3_
  
  - [x] 2.3 Implement SafetyGuard for command validation
    - Create SafetyGuard component with operation validation logic
    - Block operations on protected namespaces (kube-system, default, etc.)
    - Block dangerous resource types (namespace, clusterrole, persistentvolume)
    - Validate namespace scope for all operations
    - Return validation results with helpful error messages
    - _Requirements: 9.1, 9.2, 9.4_
  
  - [x] 2.4 Write unit tests for SafetyGuard
    - Test blocking of protected namespace operations
    - Test blocking of dangerous resource types
    - Test allowing safe operations
    - Verify error message clarity
    - _Requirements: 9.4_

- [x] 3. Backend core services - Command execution
  - [x] 3.1 Implement CommandService for kubectl execution
    - Create service to parse kubectl command strings
    - Map kubectl operations to Fabric8 client API calls (get, create, delete, apply)
    - Integrate SafetyGuard validation before execution
    - Execute commands in player's namespace using Fabric8 client
    - Return CommandResult with success status, output, and errors
    - _Requirements: 1.2, 9.1_
  
  - [x] 3.2 Implement command parsing logic
    - Parse kubectl command string to extract operation, resource type, and parameters
    - Handle common kubectl command patterns (run, get, describe, delete, apply, expose, scale)
    - Extract namespace from command or use default player namespace
    - Handle command flags and options
    - _Requirements: 1.2_
  
  - [x] 3.3 Write unit tests for command parsing
    - Test parsing of various kubectl command formats
    - Test extraction of operation, resource type, and parameters
    - Test handling of invalid command formats
    - _Requirements: 1.2_
  
  - [x] 3.4 Implement Fabric8 operation handlers
    - Create handler methods for each kubectl operation type
    - Map kubectl get to Fabric8 list/get operations
    - Map kubectl create/run to Fabric8 create operations
    - Map kubectl delete to Fabric8 delete operations
    - Map kubectl apply to Fabric8 createOrReplace operations
    - Format Fabric8 responses to match kubectl output style
    - _Requirements: 1.2_

- [x] 4. Backend core services - Validation
  - [x] 4.1 Implement ValidationService for challenge completion
    - Create service to check cluster state against challenge criteria
    - Implement criterion checkers: pod_exists, pod_running, service_exists, deployment_ready
    - Query cluster state using Fabric8 client
    - Compare actual state with expected success criteria
    - Return ValidationResult with completion status and feedback
    - _Requirements: 2.1, 2.2, 2.3, 2.5_
  
  - [x] 4.2 Implement individual criterion validators
    - Create checkPodExists: verify pod exists in namespace
    - Create checkPodRunning: verify pod status is "Running"
    - Create checkServiceExists: verify service exists in namespace
    - Create checkDeploymentReady: verify deployment has desired replicas ready
    - Handle edge cases (resource not found, partial readiness)
    - _Requirements: 2.1, 2.2_
  
  - [x] 4.3 Write unit tests for ValidationService
    - Test each criterion validator with mocked Kubernetes client
    - Test validation with all criteria met
    - Test validation with some criteria unmet
    - Test feedback message generation
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [x] 4.4 Implement asynchronous validation trigger
    - Trigger validation automatically after command execution
    - Use CompletableFuture for non-blocking validation
    - Send validation results via WebSocket to client
    - Handle validation errors gracefully
    - _Requirements: 2.1_

- [x] 5. Backend core services - Game state and challenges
  - [x] 5.1 Implement in-memory GameState management
    - Create GameState model class (sessionId, namespace, currentLevel, currentChallenge, completedChallenges, hintsUsed)
    - Create GameService with ConcurrentHashMap for session storage
    - Implement startGame: create session, initialize namespace, load first challenge
    - Implement getCurrentChallenge: retrieve challenge for session
    - Implement advanceChallenge: move to next challenge, update state
    - Implement getGameState: return current state for session
    - _Requirements: 11.1, 11.2, 12.1_
  
  - [x] 5.2 Implement ChallengeLoader for content management
    - Create Challenge model class (id, level, order, title, description, storyContext, successCriteria, hints, initialResources)
    - Load challenge YAML files from resources/challenges/ at startup
    - Parse YAML into Challenge objects using Jackson or SnakeYAML
    - Store challenges in memory indexed by ID
    - Implement getChallengeById method
    - _Requirements: 4.1, 4.5, 10.1_
  
  - [x] 5.3 Write unit tests for GameService
    - Test session creation and initialization
    - Test challenge progression logic
    - Test state retrieval
    - Mock ChallengeLoader for isolated testing
    - _Requirements: 11.1, 11.2_
  
  - [x] 5.4 Implement HintService for progressive hints
    - Create service to retrieve hints for current challenge
    - Track hints used per session in GameState
    - Return next available hint (progressive: general → specific)
    - Limit hints per challenge (max 3)
    - Return hint response with remaining hints count
    - _Requirements: 7.1, 7.2, 7.5_

- [x] 6. Backend REST controllers
  - [x] 6.1 Implement GameController REST endpoints
    - POST /api/game/start: create session, initialize namespace, return sessionId and first challenge
    - GET /api/game/state/{sessionId}: return current game state
    - GET /api/challenges/current/{sessionId}: return current challenge details
    - POST /api/game/reset/{sessionId}: reset namespace to challenge start state
    - Add error handling with @ControllerAdvice
    - _Requirements: 11.1, 11.2, 12.1, 15.2_
  
  - [x] 6.2 Implement HintController REST endpoint
    - GET /api/hints/{sessionId}: return next hint for current challenge
    - Update hints used count in GameState
    - Return error if max hints reached
    - _Requirements: 7.1, 7.2_
  
  - [x] 6.3 Write integration tests for REST endpoints
    - Test game start flow
    - Test state retrieval
    - Test hint retrieval
    - Test error responses
    - Use MockMvc for controller testing
    - _Requirements: 11.1, 7.1_

- [x] 7. Backend WebSocket controller
  - [x] 7.1 Implement TerminalWebSocketController
    - Create @MessageMapping for /app/command to receive commands from client
    - Extract sessionId from command request
    - Route command to CommandService for execution
    - Send command response to client via @SendToUser /queue/output
    - Trigger async validation after command execution
    - Send validation results to client via /queue/validation
    - Handle WebSocket errors and disconnections
    - _Requirements: 1.2, 2.1, 6.1_
  
  - [x] 7.2 Write integration tests for WebSocket controller
    - Test command message handling
    - Test response delivery to client
    - Test validation result delivery
    - Mock CommandService and ValidationService
    - _Requirements: 1.2, 2.1_

- [x] 8. Checkpoint - Backend core functionality complete
  - Ensure all backend services compile without errors
  - Verify Kubernetes client connects to cluster
  - Test namespace creation and cleanup manually
  - Test command execution with simple kubectl commands
  - Ensure all tests pass, ask the user if questions arise

- [x] 9. Frontend terminal interface
  - [x] 9.1 Implement Terminal component with xterm.js
    - Create Terminal component using xterm.js library
    - Initialize xterm instance with theme and configuration
    - Implement FitAddon for responsive terminal sizing
    - Display welcome message on mount
    - Handle terminal input (command entry, backspace, enter)
    - Maintain command history for up/down arrow navigation
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 9.2 Implement command execution in Terminal
    - Distinguish between game commands (game hint, game status) and kubectl commands
    - Send kubectl commands via WebSocket using useWebSocket hook
    - Display command output with color coding (success=green, error=red, info=cyan)
    - Show command prompt with namespace context
    - Handle special commands (hint, status, reset)
    - _Requirements: 6.1, 6.2, 6.5_
  
  - [x] 9.3 Implement useWebSocket custom hook
    - Create hook to manage WebSocket connection with SockJS and STOMP
    - Connect to /ws/terminal endpoint on mount
    - Subscribe to /user/queue/output for command responses
    - Subscribe to /user/queue/validation for validation results
    - Implement sendCommand method to send commands to backend
    - Handle connection status (connected, disconnected, reconnecting)
    - Clean up connection on unmount
    - _Requirements: 6.1_
  
  - [x] 9.4 Write component tests for Terminal
    - Test terminal rendering
    - Test command input handling
    - Test command history navigation
    - Mock useWebSocket hook
    - _Requirements: 6.1_

- [x] 10. Frontend game UI components
  - [x] 10.1 Implement ChallengePanel component
    - Create component to display current challenge information
    - Show challenge title and description
    - Display success criteria as checklist with completion status
    - Show progress indicator (Challenge X of Y)
    - Update criteria status based on validation results
    - _Requirements: 4.1, 4.2, 8.4_
  
  - [x] 10.2 Implement GameLayout component
    - Create layout with terminal on left, challenge panel on right
    - Make layout responsive for different screen sizes
    - Add header with game title and session info
    - Style with consistent theme (dark terminal aesthetic)
    - _Requirements: 6.2, 6.3_
  
  - [x] 10.3 Implement game state management with React Context
    - Create GameContext to store session state
    - Store sessionId, currentChallenge, completedChallenges, hintsUsed
    - Implement loadGameState to fetch state from API
    - Implement updateGameState to sync with backend
    - Provide context to all components via GameProvider
    - _Requirements: 11.1, 11.2_
  
  - [x] 10.4 Write component tests for ChallengePanel
    - Test challenge information display
    - Test criteria checklist rendering
    - Test progress indicator
    - _Requirements: 4.1_

- [x] 11. Frontend API integration
  - [x] 11.1 Implement API service layer
    - Create api.js with Axios instance configured with base URL
    - Implement startGame: POST /api/game/start
    - Implement getGameState: GET /api/game/state/{sessionId}
    - Implement getCurrentChallenge: GET /api/challenges/current/{sessionId}
    - Implement getHint: GET /api/hints/{sessionId}
    - Implement resetGame: POST /api/game/reset/{sessionId}
    - Add error handling and response transformation
    - _Requirements: 11.1, 7.1_
  
  - [x] 11.2 Implement game initialization flow
    - Create App.jsx with game initialization logic
    - Call startGame API on component mount
    - Store sessionId in GameContext
    - Load first challenge and display in ChallengePanel
    - Connect Terminal to WebSocket with sessionId
    - Handle initialization errors with user-friendly messages
    - _Requirements: 11.1, 12.1_
  
  - [x] 11.3 Write integration tests for API service
    - Test API calls with mocked Axios
    - Test error handling
    - Test response transformation
    - _Requirements: 11.1_

- [x] 12. Challenge content creation - Level 1 (Pod Basics)
  - [x] 12.1 Create Level 1 Challenge 1: Create Your First Pod
    - Write YAML file: level-1-challenge-1.yaml
    - Define challenge: create pod named "web-server" with nginx image
    - Success criteria: pod_exists (web-server), pod_running (web-server)
    - Add 3 progressive hints
    - Add story context about deploying first pod
    - _Requirements: 3.1, 4.1, 10.1_
  
  - [x] 12.2 Create Level 1 Challenge 2: Inspect a Pod
    - Write YAML file: level-1-challenge-2.yaml
    - Define challenge: use kubectl describe and kubectl logs on existing pod
    - Success criteria: custom validation for command execution
    - Add hints about inspection commands
    - Add story context about troubleshooting
    - _Requirements: 3.1, 4.1, 10.1_
  
  - [x] 12.3 Create Level 1 Challenge 3: Delete and Recreate Pod
    - Write YAML file: level-1-challenge-3.yaml
    - Define challenge: delete existing pod and create new one
    - Success criteria: pod_exists with new pod name, pod_running
    - Add hints about pod lifecycle
    - Add story context about pod replacement
    - _Requirements: 3.1, 4.1, 10.1_

- [x] 13. Challenge content creation - Level 2 (Deployments)
  - [x] 13.1 Create Level 2 Challenge 1: Create Deployment
    - Write YAML file: level-2-challenge-1.yaml
    - Define challenge: create deployment with 3 replicas
    - Success criteria: deployment_ready with 3 replicas
    - Add hints about kubectl create deployment
    - Add story context about scalable applications
    - _Requirements: 3.2, 4.1, 10.1_
  
  - [x] 13.2 Create Level 2 Challenge 2: Scale Deployment
    - Write YAML file: level-2-challenge-2.yaml
    - Define challenge: scale existing deployment to 5 replicas
    - Success criteria: deployment_ready with 5 replicas
    - Add hints about kubectl scale
    - Add story context about handling increased load
    - _Requirements: 3.2, 4.1, 10.3_
  
  - [x] 13.3 Create Level 2 Challenge 3: Update Deployment
    - Write YAML file: level-2-challenge-3.yaml
    - Define challenge: update deployment image version
    - Success criteria: deployment_ready with new image
    - Add hints about kubectl set image
    - Add story context about rolling updates
    - _Requirements: 3.2, 4.1, 10.3_

- [x] 14. Challenge content creation - Level 3 (Services)
  - [x] 14.1 Create Level 3 Challenge 1: Expose Deployment
    - Write YAML file: level-3-challenge-1.yaml
    - Define challenge: create service to expose deployment
    - Success criteria: service_exists, service has correct selector
    - Add hints about kubectl expose
    - Add story context about networking
    - _Requirements: 3.2, 4.1, 10.4_
  
  - [x] 14.2 Create Level 3 Challenge 2: Service Discovery
    - Write YAML file: level-3-challenge-2.yaml
    - Define challenge: verify service endpoints
    - Success criteria: service has endpoints matching pod IPs
    - Add hints about kubectl get endpoints
    - Add story context about service discovery
    - _Requirements: 3.2, 4.1, 10.4_
  
  - [x] 14.3 Create Level 3 Challenge 3: Port Forwarding
    - Write YAML file: level-3-challenge-3.yaml
    - Define challenge: use kubectl port-forward to access service
    - Success criteria: custom validation for port-forward command
    - Add hints about port-forward syntax
    - Add story context about local testing
    - _Requirements: 3.2, 4.1, 10.4_

- [x] 15. Challenge content creation - Level 4 (ConfigMaps & Secrets)
  - [x] 15.1 Create Level 4 Challenge 1: Create ConfigMap
    - Write YAML file: level-4-challenge-1.yaml
    - Define challenge: create ConfigMap with configuration data
    - Success criteria: configmap_exists
    - Add hints about kubectl create configmap
    - Add story context about configuration management
    - _Requirements: 4.1, 10.1_
  
  - [x] 15.2 Create Level 4 Challenge 2: Use ConfigMap in Pod
    - Write YAML file: level-4-challenge-2.yaml
    - Define challenge: create pod that uses ConfigMap as environment variable
    - Success criteria: pod_running, pod has env var from ConfigMap
    - Add hints about env valueFrom configMapKeyRef
    - Add story context about dynamic configuration
    - _Requirements: 4.1, 10.1_
  
  - [x] 15.3 Create Level 4 Challenge 3: Create Secret
    - Write YAML file: level-4-challenge-3.yaml
    - Define challenge: create Secret with sensitive data
    - Success criteria: secret_exists
    - Add hints about kubectl create secret
    - Add story context about security
    - _Requirements: 4.1, 10.1_

- [x] 16. Challenge content creation - Level 5 (Debugging)
  - [x] 16.1 Create Level 5 Challenge 1: Fix Broken Pod
    - Write YAML file: level-5-challenge-1.yaml
    - Define challenge: fix pod with wrong image (ImagePullBackOff)
    - Initial resources: broken pod with invalid image
    - Success criteria: pod_running with correct image
    - Add hints about kubectl describe, kubectl edit
    - Add story context about troubleshooting
    - _Requirements: 4.1, 10.2, 13.1_
  
  - [x] 16.2 Create Level 5 Challenge 2: Fix Broken Service
    - Write YAML file: level-5-challenge-2.yaml
    - Define challenge: fix service with wrong selector
    - Initial resources: service with mismatched selector, deployment
    - Success criteria: service has endpoints
    - Add hints about checking selectors
    - Add story context about networking issues
    - _Requirements: 4.1, 10.2, 13.1_
  
  - [x] 16.3 Create Level 5 Challenge 3: Resource Limits
    - Write YAML file: level-5-challenge-3.yaml
    - Define challenge: fix pod failing due to insufficient resources
    - Initial resources: pod with resource requests exceeding quota
    - Success criteria: pod_running with adjusted resources
    - Add hints about resource requests and limits
    - Add story context about resource management
    - _Requirements: 4.1, 10.2, 10.5, 13.1_

- [x] 17. Checkpoint - Challenge content complete
  - Verify all 15 challenge YAML files are created
  - Ensure ChallengeLoader successfully loads all challenges
  - Test each challenge manually for completeness
  - Verify success criteria are achievable
  - Ensure all tests pass, ask the user if questions arise

- [x] 18. Integration and polish
  - [x] 18.1 Implement challenge progression flow
    - When validation detects challenge complete, trigger advanceChallenge in GameService
    - Load next challenge and send to client via WebSocket
    - Display completion message in terminal with color
    - Update ChallengePanel with new challenge
    - Handle level transitions with story context
    - _Requirements: 2.1, 4.2, 14.1, 14.2_
  
  - [x] 18.2 Implement level transition narrative
    - Create NarrativeService to deliver story content
    - Display level intro story when starting new level
    - Display level completion summary when finishing level
    - Format narrative with visual borders in terminal
    - _Requirements: 4.1, 4.2, 4.5, 14.2, 14.3_
  
  - [x] 18.3 Implement error handling and user feedback
    - Add global error handler in backend with @ControllerAdvice
    - Handle KubernetesClientException with user-friendly messages
    - Handle cluster connectivity failures with troubleshooting guidance
    - Display errors in terminal with red color and helpful context
    - Implement reconnection logic for WebSocket disconnections
    - _Requirements: 1.4, 13.1, 13.2, 15.1, 15.5_
  
  - [x] 18.4 Implement namespace reset functionality
    - Add reset endpoint to delete all resources in namespace
    - Recreate initial resources for current challenge
    - Notify user of successful reset in terminal
    - Update challenge panel to reflect reset state
    - _Requirements: 9.5, 15.2_
  
  - [x] 18.5 Write end-to-end integration tests
    - Test complete gameplay flow: start → command → validation → progression
    - Test hint system integration
    - Test reset functionality
    - Test error scenarios (cluster down, invalid commands)
    - _Requirements: 1.2, 2.1, 7.1_

- [x] 19. Final polish and documentation
  - [x] 19.1 Add terminal color scheme and formatting
    - Implement color constants for success, error, warning, info, story
    - Format command output with appropriate colors
    - Add visual borders for story segments
    - Implement command prompt with namespace indicator
    - _Requirements: 6.3, 6.4_
  
  - [x] 19.2 Implement command history navigation
    - Store command history in Terminal component state
    - Handle up/down arrow keys to navigate history
    - Display previous commands when navigating
    - _Requirements: 6.1_
  
  - [x] 19.3 Add loading states and connection indicators
    - Show "Connecting..." message while WebSocket connects
    - Display connection status indicator in UI
    - Show loading spinner during API calls
    - Handle loading states gracefully
    - _Requirements: 6.4, 15.5_
  
  - [x] 19.4 Create README with setup instructions
    - Document prerequisites (Java 17+, Node 18+, Minikube/Kind)
    - Provide step-by-step setup instructions
    - Document how to start backend and frontend
    - Include troubleshooting section
    - _Requirements: 12.1, 12.3_

- [ ] 20. Final checkpoint - MVP complete
  - Run complete playthrough of all 15 challenges
  - Verify all success criteria work correctly
  - Test error handling and edge cases
  - Verify namespace isolation with multiple sessions
  - Ensure all tests pass, ask the user if questions arise

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for faster MVP delivery
- Each task references specific requirements from requirements.md for traceability
- The implementation follows an incremental approach: infrastructure → backend → frontend → content → integration
- Checkpoints (tasks 8, 17, 20) ensure validation at key milestones
- Backend tasks use Java/Spring Boot with Fabric8 Kubernetes Client
- Frontend tasks use React with xterm.js for terminal emulation
- All kubectl operations are executed server-side for security
- Namespace isolation ensures safe multi-player support
- In-memory state management simplifies MVP (no database required)
- Challenge content is data-driven (YAML files) for easy modification
- WebSocket provides real-time command execution and validation feedback
- Progressive hints guide players without giving away solutions
- The MVP delivers 15 challenges across 5 levels (60+ minutes of gameplay)
- **Current status**: Backend (tasks 1-8), frontend (tasks 9-11), and all challenge content (tasks 12-17) are fully implemented. All 15 challenge YAML files have been created. Remaining work is integration polish (task 18), final polish (task 19), and end-to-end validation (task 20).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["18.1", "18.2"] },
    { "id": 1, "tasks": ["18.3", "18.4"] },
    { "id": 2, "tasks": ["18.5", "19.1", "19.2", "19.3"] },
    { "id": 3, "tasks": ["19.4"] }
  ]
}
```
