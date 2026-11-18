# CineTogether — Claude Code Instructions

## Project

CineTogether is a solo-developer Java Backend portfolio project featuring movie browsing, Watch Alone playback, and
private and public synchronized Watch Parties.

Stack:

- Java 21, Spring Boot 4.x, Gradle Kotlin DSL
- Spring Modulith modular monolith
- PostgreSQL, Spring Data JPA, Flyway
- Spring Security with Supabase Auth JWT
- React frontend, REST APIs, native WebSocket
- External movie provider adapter and browser-side HLS playback

Repository:

- `backend/` — Spring Boot application
- `frontend/` — React application
- `docs/` — product, architecture, design, and development documentation

## Sources of Truth

Consult relevant documentation before changing a feature:

- `docs/product/requirements.md` — MVP scope and business rules
- `docs/architecture/architecture.md` — architecture and module boundaries
- `docs/design/database.md` — persistence and transaction rules
- `docs/design/api.md` — REST and WebSocket contracts
- `docs/design/watch-party.md` — synchronization, lifecycle, concurrency, and recovery
- `docs/development/progress.md` — current implementation progress, when available
- `docs/development/setup.md` — verified local development instructions, when available

Read the relevant sections, not necessarily every document for every task. Do not silently override confirmed decisions.
Ask for clarification when a change would affect business rules, public contracts, or architecture.

## Engineering Principles

- Prefer simple, maintainable, testable solutions.
- Preserve the modular monolith and existing module boundaries.
- Keep business logic separate from transport and persistence concerns.
- Enforce authentication, membership, and Host authorization on the backend.
- Reuse one Watch Party engine for PRIVATE and PUBLIC rooms; public discovery and preview must never expose private
  rooms, invitation codes, or participant identities.
- Use short transactions and the documented concurrency strategy.
- Keep external provider calls outside database transactions.
- Do not introduce unnecessary abstractions, dependencies, services, or infrastructure.
- Do not implement out-of-scope features, including screen sharing, voice/camera, watchlists, recommendations, and
  advanced public-room search or moderation.

## Development Workflow

1. Inspect relevant code, documentation, and Git status before editing.
2. For nontrivial tasks, explain a concise implementation plan first.
3. Implement one small, reviewable task at a time.
4. Add or update appropriate automated tests.
5. Run relevant build and test commands when available.
6. Explain what changed, why, and how it was verified.
7. Identify assumptions, limitations, and remaining work.

Never claim tests passed without running them.

Do not create Git commits, push changes, or perform destructive operations unless explicitly requested.

## Testing Approach

- Apply Selective TDD: follow RED → GREEN → REFACTOR, one behavior at a time, for business rules and high-risk behavior
  such as authentication, authorization, validation, idempotent upsert, and concurrency.
- For framework configuration and external integrations, choose appropriate verification and integration tests instead
  of forcing TDD mechanically.
- Prefer fast unit tests for pure domain logic and integration tests with real PostgreSQL via Testcontainers for
  persistence, transactions, and concurrency.
- Test observable behavior rather than implementation details. Avoid unnecessary mocking and abstractions created solely
  for tests.
- Run the failing test before implementation when applying TDD, then rerun after implementation and refactoring. Never
  claim tests pass without actually running them.

## Learning-Oriented Collaboration

The developer wants to understand the implementation, not only generate working code.

After implementing a meaningful feature:

- Explain its request/data flow and important design decisions.
- Highlight relevant Java, Spring, database, or WebSocket concepts.
- Mention important edge cases and tests.
- Suggest a few concise questions the developer should be able to answer.

Keep explanations practical and specific to the code.

## Documentation Maintenance

- Keep documentation in English.
- Update relevant design documents when confirmed behavior or contracts change.
- Update `docs/development/progress.md` when a task or milestone is completed.
- Keep `docs/development/setup.md` aligned with verified setup commands.
- Avoid duplicating detailed specifications across files.

## Model Delegation

Use low-cost read-only subagents for straightforward file discovery and documentation lookups when beneficial.

The main agent remains responsible for validating relevant source material, architectural decisions, implementation
correctness, and testing.

Do not delegate critical concurrency or security decisions solely to a lightweight documentation agent.

### Session Continuity

- At the start of a new session, read `docs/development/progress.md` to understand the current project state.
- Before starting a task, inspect the relevant code and Git status to verify actual progress.
- After completing a meaningful task, update `docs/development/progress.md` with:
    - The current phase and milestone status.
    - What was completed and verified.
    - The current or next task.
    - Any unresolved blockers or important implementation decisions.
- Keep progress notes concise and useful for continuing work in a new session.
- Never mark a task as completed without verification.