# Project Structure

## Directory Organization

```
.
├── .kiro/
│   ├── specs/
│   │   └── kubernetes-learning-game/
│   │       ├── .config.kiro
│   │       └── requirements.md
│   └── steering/
│       ├── product.md
│       ├── tech.md
│       └── structure.md
└── (source code to be implemented)
```

## Planned Source Structure

The game implementation should follow this modular architecture:

### Backend (Spring Boot)
```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/k8sgame/
│   │   │       ├── K8sGameApplication.java
│   │   │       ├── config/
│   │   │       │   ├── WebSocketConfig.java
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   └── KubernetesConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── GameController.java
│   │   │       │   ├── ChallengeController.java
│   │   │       │   ├── HintController.java
│   │   │       │   ├── ProgressController.java
│   │   │       │   └── TerminalWebSocketController.java
│   │   │       ├── service/
│   │   │       │   ├── GameEngineService.java
│   │   │       │   ├── ValidationService.java
│   │   │       │   ├── NarrativeService.java
│   │   │       │   ├── HintService.java
│   │   │       │   ├── ProgressService.java
│   │   │       │   ├── KubernetesService.java
│   │   │       │   ├── CommandExecutorService.java
│   │   │       │   └── NamespaceManagerService.java
│   │   │       ├── model/
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Player.java
│   │   │       │   │   ├── GameState.java
│   │   │       │   │   ├── Challenge.java
│   │   │       │   │   ├── Achievement.java
│   │   │       │   │   └── Progress.java
│   │   │       │   └── dto/
│   │   │       │       ├── CommandRequest.java
│   │   │       │       ├── CommandResponse.java
│   │   │       │       ├── ValidationResult.java
│   │   │       │       ├── HintResponse.java
│   │   │       │       └── GameStateDto.java
│   │   │       ├── repository/
│   │   │       │   ├── PlayerRepository.java
│   │   │       │   ├── GameStateRepository.java
│   │   │       │   ├── ChallengeRepository.java
│   │   │       │   ├── AchievementRepository.java
│   │   │       │   └── ProgressRepository.java
│   │   │       ├── security/
│   │   │       │   ├── SafetyGuard.java
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   └── UserDetailsServiceImpl.java
│   │   │       └── exception/
│   │   │           ├── ClusterConnectionException.java
│   │   │           ├── NamespaceIsolationException.java
│   │   │           └── ValidationException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── challenges/
│   │       │   ├── beginner/
│   │       │   │   ├── level-01-challenge-01.yaml
│   │       │   │   └── level-01-challenge-02.yaml
│   │       │   ├── intermediate/
│   │       │   └── advanced/
│   │       ├── scenarios/
│   │       │   ├── level-01-story.yaml
│   │       │   ├── level-02-story.yaml
│   │       │   └── ...
│   │       ├── hints/
│   │       │   └── hint-database.yaml
│   │       └── achievements/
│   │           └── achievements.yaml
│   └── test/
│       └── java/
│           └── com/k8sgame/
│               ├── service/
│               ├── controller/
│               └── integration/
├── pom.xml
└── Dockerfile
```

### Frontend (React)
```
frontend/
├── src/
│   ├── components/
│   │   ├── terminal/
│   │   │   ├── Terminal.jsx
│   │   │   ├── CommandInput.jsx
│   │   │   └── OutputDisplay.jsx
│   │   ├── game/
│   │   │   ├── StoryPanel.jsx
│   │   │   ├── ChallengePanel.jsx
│   │   │   ├── HintPanel.jsx
│   │   │   ├── ProgressDashboard.jsx
│   │   │   └── AchievementNotification.jsx
│   │   ├── layout/
│   │   │   ├── Header.jsx
│   │   │   ├── Sidebar.jsx
│   │   │   └── GameLayout.jsx
│   │   └── auth/
│   │       ├── Login.jsx
│   │       └── Register.jsx
│   ├── services/
│   │   ├── api.js
│   │   ├── websocket.js
│   │   ├── authService.js
│   │   ├── gameService.js
│   │   └── commandService.js
│   ├── context/
│   │   ├── AuthContext.jsx
│   │   ├── GameContext.jsx
│   │   └── WebSocketContext.jsx
│   ├── hooks/
│   │   ├── useTerminal.js
│   │   ├── useWebSocket.js
│   │   ├── useGameState.js
│   │   └── useCommandHistory.js
│   ├── utils/
│   │   ├── commandParser.js
│   │   ├── colorScheme.js
│   │   └── formatters.js
│   ├── styles/
│   │   ├── terminal.css
│   │   ├── game.css
│   │   └── theme.css
│   ├── App.jsx
│   └── main.jsx
├── public/
│   └── index.html
├── package.json
├── vite.config.js
└── Dockerfile
```

## Configuration Files

### Backend Configuration
```
backend/src/main/resources/
├── application.yml              # Main Spring Boot configuration
├── application-dev.yml          # Development environment settings
├── application-prod.yml         # Production environment settings
└── kubeconfig                   # Kubernetes cluster credentials (not in git)
```

### Frontend Configuration
```
frontend/
├── .env.development             # Development API endpoints
├── .env.production              # Production API endpoints
└── vite.config.js               # Build configuration
```

## Database Schema

```sql
-- Player table
CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Game state table
CREATE TABLE game_states (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT REFERENCES players(id),
    current_level INT NOT NULL,
    current_challenge INT NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    session_start TIMESTAMP,
    total_playtime INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Progress table
CREATE TABLE progress (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT REFERENCES players(id),
    challenge_id VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    hints_used INT DEFAULT 0,
    commands_executed INT DEFAULT 0
);

-- Achievements table
CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT REFERENCES players(id),
    achievement_id VARCHAR(100) NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Architectural Principles

### Separation of Concerns
- **Engine**: Game logic and state management
- **Validation**: Kubernetes cluster interaction and verification
- **Narrative**: Story content and delivery
- **UI**: Terminal interface and user interaction

### Modularity
Each component should be independently testable and replaceable. For example, the narrative engine should work with any challenge set, and the validation system should work with any Kubernetes cluster.

### Data-Driven Design
- Challenges defined in YAML/JSON files, not hardcoded
- Story content externalized for easy editing
- Achievements and hints configurable without code changes

### Safety First
- All cluster operations go through the safety guard
- Namespace isolation enforced at the client level
- Resource quotas applied automatically

### State Consistency
The validation system must maintain state consistency: checking cluster state, then modifying it, then checking again should accurately reflect player actions.

## File Naming Conventions

### Backend (Java)
- **Java classes**: PascalCase (e.g., `GameEngineService`, `ValidationService`)
- **Methods**: camelCase (e.g., `validateChallenge`, `loadProgress`)
- **Constants**: UPPERCASE with underscores (e.g., `MAX_HINTS`, `DEFAULT_NAMESPACE`)
- **Packages**: lowercase (e.g., `com.k8sgame.service`, `com.k8sgame.controller`)

### Frontend (JavaScript/React)
- **Components**: PascalCase (e.g., `Terminal.jsx`, `ChallengePanel.jsx`)
- **Utilities/Services**: camelCase (e.g., `commandParser.js`, `gameService.js`)
- **Hooks**: camelCase with `use` prefix (e.g., `useTerminal.js`, `useGameState.js`)
- **Constants**: UPPERCASE with underscores (e.g., `API_BASE_URL`, `WS_ENDPOINT`)

### Configuration Files
- **YAML files**: lowercase with hyphens (e.g., `level-01.yaml`, `application-dev.yml`)
- **Environment files**: lowercase with dots (e.g., `.env.development`, `.env.production`)

## Import Organization

### Backend (Java)
1. Java standard library imports
2. Third-party library imports (Spring, Fabric8, etc.)
3. Local application imports
4. Static imports (if any)
5. Blank line between each group

```java
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.k8sgame.model.entity.Challenge;
import com.k8sgame.repository.ChallengeRepository;

import static com.k8sgame.util.Constants.DEFAULT_NAMESPACE;
```

### Frontend (JavaScript/React)
1. React imports
2. Third-party library imports
3. Local component imports
4. Local utility/service imports
5. Style imports
6. Blank line between each group

```javascript
import React, { useState, useEffect } from 'react';

import { Terminal as XTerm } from 'xterm';
import axios from 'axios';

import CommandInput from './CommandInput';
import OutputDisplay from './OutputDisplay';

import { parseCommand } from '../../utils/commandParser';
import { executeCommand } from '../../services/commandService';

import './terminal.css';
```

## Documentation Standards

### Backend (Java)
- All classes should have Javadoc comments explaining purpose
- All public methods should have Javadoc with `@param`, `@return`, and `@throws` tags
- Complex algorithms should have inline comments explaining logic
- REST endpoints should be documented with Swagger/OpenAPI annotations

```java
/**
 * Service for validating player challenge completion.
 * Checks cluster state against challenge success criteria.
 */
@Service
public class ValidationService {
    
    /**
     * Validates if the current cluster state meets challenge criteria.
     *
     * @param namespace the player's isolated namespace
     * @param challengeId the unique challenge identifier
     * @return ValidationResult containing success status and feedback
     * @throws ClusterConnectionException if unable to connect to cluster
     */
    public ValidationResult validateChallenge(String namespace, String challengeId) {
        // Implementation
    }
}
```

### Frontend (JavaScript/React)
- All components should have JSDoc comments explaining purpose and props
- Complex functions should have JSDoc with parameter and return descriptions
- React components should document prop types
- Custom hooks should document parameters and return values

```javascript
/**
 * Terminal component for executing kubectl commands.
 * Provides xterm.js integration with WebSocket communication.
 *
 * @param {Object} props - Component props
 * @param {string} props.namespace - Player's Kubernetes namespace
 * @param {Function} props.onCommandExecute - Callback when command is executed
 * @returns {JSX.Element} Terminal component
 */
export const Terminal = ({ namespace, onCommandExecute }) => {
    // Implementation
};
```

### Configuration Files
- Challenge definitions should include learning objectives and success criteria
- YAML files should have comments explaining non-obvious configurations
- Environment variables should be documented in README
