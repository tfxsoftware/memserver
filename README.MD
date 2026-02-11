E-Strategist Core Engine (Backend)

This is the Backend for E-Strategist. For the corresponding UI, see the Frontend Repository repository.

E-Strategist is a high-performance MOBA management simulation engine built with Spring Boot 4 and Java 21. It manages the game world's logic, from complex match simulations to the player-driven auction house.

### Open Source

E-Strategist Backend is an open-source project, and we welcome contributions from the community!

### Key Technical Features

1. The Match Engine

The heart of the simulation is a probabilistic engine that determines match outcomes based on:

- Weighted Role Strength (RS): 60% of player power.
- Champion Mastery (CS): 40% of player power, modified by seasonal meta tiers.
- The "Clutch Window": A probability swing mechanism (+20%) that activates in tight games if a team possesses a player with the CLUTCH_FACTOR trait.
- Clamped Outcomes: All matches are clamped between 5% and 95% win probability to ensure a "miracle win" is always possible.

2. Persistence Model (PostgreSQL + JSONB)

We utilize a hybrid relational approach:

- Strict Data: Users, Rosters, and Player entities are managed via standard JPA relationships for transactional integrity.
- Flexible Logs: Match "Box Scores" and play-by-play logs are stored using PostgreSQL JSONB columns. This allows for rich, searchable history without the overhead of a secondary NoSQL database.

3. Automated Lifecycle (The Watcher)

The system remains "alive" via Cron-based scheduled tasks:

- Event Orchestrator: Polls every minute to transition league statuses (CLOSED -> OPEN -> ONGOING).
- Match Orchestrator: Triggers the simulation engine as scheduled match times arrive.
- Energy/Bootcamp Ticks: Handles passive energy recovery and training XP distribution every 6 hours.

### Tech Stack

- Framework: Spring Boot 4.0.2 (Java 21)
- Security: Spring Security with JWT & BCrypt
- Database: PostgreSQL 16 (Relational + JSONB)
- Containerization: Docker & Docker Compose
- Build Tool: Gradle (Kotlin DSL)
- Environment Variables: dotenv-java

### Getting Started

#### Prerequisites

- Docker & Docker Compose
- Java 21 JDK

#### Setup

Clone the repo:

```bash
git clone https://github.com/tfxsoftware/e-strategist.backend.git
```

Start the Infrastructure:

```bash
docker-compose up -d
```

Run the Application:

```bash
./gradlew dev
```

The app runs with the local profile active by default via the custom Gradle task.

### API Documentation

The API follows RESTful principles. Key modules include:

- /api/auth: Registration and login.
- /api/players: Marketplace access and scouting.
- /api/matches: Draft intentions and history.
- /api/events: League standings and registration.

### Contributing

E-Strategist Backend is an open-source project and welcomes contributions from the community! Whether you're fixing bugs, adding new features, or improving documentation, your help is greatly appreciated.

Please see the [CONTRIBUTING.MD](CONTRIBUTING.MD) file for guidelines on how to contribute.

### License

This project is licensed under the MIT License.
