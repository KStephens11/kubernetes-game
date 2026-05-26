# Kubernetes Learning Game

An interactive, terminal-based educational game that teaches Kubernetes concepts through real kubectl command execution. Players solve narrative-driven challenges against a real Kubernetes cluster in a safe, isolated environment.

## Overview

- **15 challenges** across 5 levels (Pods → Deployments → Services → ConfigMaps/Secrets → Debugging)
- **Real cluster integration** — actual kubectl commands executed server-side
- **Namespace isolation** — each session gets its own namespace with resource quotas
- **Progressive hints** — three-level hint system (conceptual → tactical → explicit)
- **Live validation** — challenge completion detected automatically via WebSocket

## Prerequisites

| Tool | Minimum Version | Notes |
|------|----------------|-------|
| Java | 17+ | JDK required |
| Maven | 3.8+ | Or use the included `mvnw` wrapper |
| Node.js | 18+ | |
| npm | 9+ | |
| Kubernetes cluster | 1.24+ | Minikube, Kind, or any cluster |
| kubectl | 1.24+ | Must be configured and working |

### Kubernetes cluster options

**Minikube (recommended for local development)**
```bash
minikube start --memory=4096 --cpus=2
```

**Kind**
```bash
kind create cluster --name k8s-game
```

**Existing cluster** — ensure your `~/.kube/config` points to a cluster where you have permission to create namespaces and resource quotas.

## Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd new-kubernetes-game
```

### 2. Verify cluster connectivity

```bash
kubectl cluster-info
kubectl get nodes
```

Both commands should succeed before starting the game.

### 3. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.

To use a custom kubeconfig path:
```bash
KUBECONFIG=/path/to/kubeconfig mvn spring-boot:run
```

### 4. Start the frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173** (Vite default).

### 5. Open the game

Navigate to **http://localhost:5173** in your browser.

## Playing the Game

### Terminal commands

The in-browser terminal accepts two types of commands:

**kubectl commands** — executed server-side in your isolated namespace:
```bash
kubectl get pods
kubectl run web-server --image=nginx
kubectl describe pod web-server
kubectl delete pod web-server
kubectl create deployment app --image=nginx --replicas=3
kubectl scale deployment app --replicas=5
kubectl expose deployment app --port=80
kubectl apply -f manifest.yaml
kubectl logs <pod-name>
```

**Game commands** — control the game session:
```bash
game status    # Show current level, hints used, commands executed
game hint      # Request the next progressive hint
game reset     # Reset namespace to challenge start state
game help      # Show available game commands
```

### Progression

1. Read the challenge objective in the right panel
2. Execute kubectl commands in the terminal
3. Validation runs automatically after each successful command
4. When all success criteria are met, the next challenge loads automatically
5. Level transitions display a narrative story in the terminal

## Configuration

### Backend (`backend/src/main/resources/application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `kubernetes.config-path` | `~/.kube/config` | Path to kubeconfig (overridden by `KUBECONFIG` env var) |
| `kubernetes.namespace-prefix` | `game-session-` | Prefix for player namespaces |
| `game.max-hints-per-challenge` | `3` | Maximum hints per challenge |
| `game.session-timeout` | `3600` | Session inactivity timeout (seconds) |

### Frontend (`frontend/.env.development`)

```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws/terminal
```

## Running Tests

### Backend tests

```bash
cd backend
mvn test
```

Tests use mocked Kubernetes client — no cluster required.

### Frontend tests

```bash
cd frontend
npm test
```

## Project Structure

```
.
├── backend/                    # Spring Boot backend
│   └── src/
│       ├── main/
│       │   ├── java/com/k8sgame/
│       │   │   ├── controller/     # REST + WebSocket controllers
│       │   │   ├── service/        # Game engine, validation, narrative
│       │   │   ├── model/          # Domain models
│       │   │   └── config/         # Spring + Kubernetes config
│       │   └── resources/
│       │       ├── application.yml
│       │       └── challenges/     # Challenge YAML definitions (15 files)
│       └── test/                   # Unit + integration tests
└── frontend/                   # React frontend
    └── src/
        ├── components/
        │   ├── terminal/       # xterm.js terminal component
        │   ├── game/           # Challenge panel, story display
        │   └── layout/         # Game layout
        ├── hooks/              # useWebSocket, useCommandHistory
        ├── services/           # API + WebSocket clients
        └── context/            # GameContext (session state)
```

## Troubleshooting

### Backend won't start

**"Unable to connect to Kubernetes cluster"**
- Run `kubectl cluster-info` to verify cluster is reachable
- Check `KUBECONFIG` environment variable or `~/.kube/config`
- Ensure your user has permission to create namespaces: `kubectl auth can-i create namespaces`

**Port 8080 already in use**
```bash
# Change port in application.yml
server:
  port: 8081
```
Then update `frontend/.env.development` to match.

### Frontend won't connect

**"Disconnected" status in terminal**
- Verify backend is running on port 8080
- Check browser console for WebSocket errors
- Ensure no CORS issues (backend allows all origins by default in dev)

### Challenge validation not triggering

- Validation runs asynchronously after each successful kubectl command
- Some challenges require the resource to reach a specific state (e.g., pod `Running`) — this may take a few seconds
- Use `kubectl get pods -w` to watch pod status changes
- If stuck, use `game hint` for guidance

### Namespace cleanup

Player namespaces are cleaned up when a session ends. To manually remove all game namespaces:
```bash
kubectl get namespaces | grep game-session | awk '{print $1}' | xargs kubectl delete namespace
```

## Architecture

```
Browser                    Backend (Spring Boot)           Kubernetes
  │                              │                              │
  │──── WebSocket /ws/terminal ──►│                              │
  │                              │──── Fabric8 Client ─────────►│
  │◄─── /queue/output ───────────│◄─── kubectl response ────────│
  │◄─── /queue/validation ───────│◄─── cluster state check ─────│
  │◄─── /queue/challenge ────────│  (on challenge complete)      │
  │                              │                              │
  │──── REST /api/game/start ───►│──── create namespace ────────►│
  │◄─── sessionId + challenge ───│                              │
```

- All kubectl operations execute **server-side** — the browser never touches the cluster directly
- Each session gets an isolated namespace with resource quotas (10 pods, 5 services, 2 CPU, 4Gi memory)
- WebSocket provides real-time command output and automatic challenge validation
