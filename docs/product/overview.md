# CineTogether — Product Overview

**Status:** Planning & Design  
**Version:** 0.3

## 1. Introduction

CineTogether is a web application that allows users to watch movies individually or enjoy a shared watching experience
with friends and family.

The application provides two main experiences:

- **Watch Alone:** Discover, browse, and watch movies independently.
- **Watch Together:** Create or join Watch Party rooms where participants can watch the same movie with synchronized
  playback and communicate in real time.

CineTogether is a solo portfolio project intended to become a functional application that can be used over the Internet.

## 2. Problem Statement

Watching movies remotely with friends or family often requires multiple applications for video playback, communication,
and coordination.

CineTogether aims to simplify this experience by allowing users to watch the same movie together through synchronized
playback while communicating within a shared room.

The application also supports individual movie watching without requiring participation in a Watch Party.

## 3. Product Vision

To provide a simple and reliable movie-watching platform where users can enjoy movies individually or share the
experience with others, regardless of physical location.

The focus is on usability, real-time interaction, and a consistent shared watching experience.

## 4. Target Users

CineTogether primarily targets:

- **Individual Viewers:** Users who want to browse and watch movies independently.
- **Friends and Family:** Small groups who want to watch movies together remotely.
- **Community Viewers:** Users who want to discover and join public Watch Party rooms.

The MVP serves individual viewers, private groups, and community viewers through private and public Watch Party rooms.

## 5. Core Experiences

### 5.1. Watch Alone

Users can explore available movies and watch content through CineTogether's video player.

Planned capabilities include:

- Browse and search movies.
- View movie details, posters, and trailers.
- Watch movies and select episodes.
- Manage a personal watchlist.
- View watch history and continue watching.

### 5.2. Watch Together

Users can create or join Watch Party rooms to watch the same movie with synchronized playback.

Each participant plays the movie through their own CineTogether video player. Playback actions are synchronized across
the room.

Planned capabilities include:

- Create and manage Watch Party rooms.
- Create PRIVATE or PUBLIC rooms.
- Join private rooms through invitation links.
- Discover, preview, and join public rooms.
- Watch movies with synchronized playback.
- Allow the Host to control shared Play, Pause, and Seek actions.
- Communicate through real-time chat.
- Support microphone and camera communication in a future phase.
- Manage room membership.
- Handle disconnections and reconnections.

The **Host** is responsible for room management and shared playback control.

Unlike traditional screen sharing, synchronized playback does not require one participant to broadcast their screen to
others.

## 6. Product Goals

CineTogether aims to:

1. Provide a convenient individual movie-watching experience.
2. Enable friends and family to watch movies together remotely.
3. Keep playback reasonably synchronized across participants.
4. Support real-time communication and simple room management.
5. Deliver a functional and reliable application for small groups.

As a portfolio project, CineTogether also provides an opportunity to practice Java backend development and software
engineering through real-world application requirements.

## 7. Scope and Constraints

The initial development focuses on delivering a manageable and functional product.

Key considerations:

- Private and public Watch Party rooms are both part of the MVP and share one Watch Party engine.
- Synchronized Playback is the primary shared watching mechanism.
- Screen Sharing is not included in the MVP.
- Public room discovery is deliberately simple (a paginated list and preview); search, ranking, and advanced moderation
  are not initial priorities.
- Movie playback must use content sources with appropriate usage rights.
- The application does not aim to build its own video transcoding or large-scale streaming infrastructure.
- Advanced features and large-scale scalability are not initial priorities.

Detailed feature priorities, business rules, and MVP requirements will be defined in `requirements.md`.

## 8. Current Status

CineTogether is currently in the **Planning & Design** phase.

The initial Spring Boot backend project has been created, and the technology direction has been outlined.

Product requirements, system architecture, database design, and implementation details are still being developed.

Core application features have not been implemented yet.