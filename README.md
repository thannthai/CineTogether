# CineTogether

CineTogether is a personal movie-watching and Watch Party web application.

## Overview

The application aims to support two main experiences:

- **Watch Alone:** Browse and watch movies individually.
- **Watch Together:** Watch movies with friends and family in private or public rooms with Host-controlled synchronized
  playback and realtime chat.

## Tech Stack

- **Backend:** Java 21, Spring Boot, Spring Modulith
- **Frontend:** React
- **Database:** PostgreSQL
- **Authentication:** Supabase Auth
- **Realtime:** WebSocket (REST for catalog and room management)
- **Video:** HTML5 video / HLS.js
- **Build Tool:** Gradle Kotlin DSL

## Project Status

Planning & Design.

The backend foundation has been initialized. Product requirements, architecture, and detailed design for the private and
public Watch Party MVP are drafted in `docs/`; feature implementation has not started.

## Documentation

Project documentation lives in the `docs/` directory (`product/`, `architecture/`, `design/`).