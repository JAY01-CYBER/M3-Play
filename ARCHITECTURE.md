# M3-Play Architecture Overview

This document provides an overview of the M3-Play application architecture using a diagram-based approach.

## Architecture Diagram

```mermaid
graph TB
    subgraph UI["User Interface Layer"]
        Activity["Android Activities"]
        Fragment["Fragments"]
        Compose["Jetpack Compose UI"]
    end

    subgraph Presentation["Presentation Layer"]
        ViewModel["ViewModels"]
        Navigation["Navigation Manager"]
        StateManagement["State Management"]
    end

    subgraph Domain["Domain Layer"]
        UseCases["Use Cases"]
        Entities["Entities"]
        Repository["Repository Interface"]
    end

    subgraph Data["Data Layer"]
        LocalDB["Local Database"]
        RemoteAPI["Remote API"]
        DataRepository["Repository Implementation"]
        Cache["Cache Manager"]
    end

    subgraph External["External Services"]
        Network["Network Service"]
        Storage["File Storage"]
    end

    UI -->|Observes| Presentation
    Presentation -->|Executes| Domain
    Domain -->|Implements| Data
    Data -->|Communicates| External
    
    Activity -->|Binds| ViewModel
    Fragment -->|Binds| ViewModel
    Compose -->|Observes| ViewModel
    ViewModel -->|Triggers| UseCases
    UseCases -->|Accesses| Repository
    Repository -->|Fetches/Stores| LocalDB
    Repository -->|Fetches| RemoteAPI
    Repository -->|Manages| Cache
    RemoteAPI -->|Uses| Network
    LocalDB -->|Uses| Storage

    style UI fill:#e1f5ff
    style Presentation fill:#fff3e0
    style Domain fill:#f3e5f5
    style Data fill:#e8f5e9
    style External fill:#fce4ec
```

## Layer Descriptions

### UI Layer
- **Android Activities & Fragments**: Main entry points for user interaction
- **Jetpack Compose UI**: Modern declarative UI toolkit for Android

### Presentation Layer
- **ViewModels**: Manage UI state and handle lifecycle awareness
- **Navigation Manager**: Coordinates screen transitions
- **State Management**: Manages application state

### Domain Layer
- **Use Cases**: Business logic encapsulation
- **Entities**: Core business objects
- **Repository Interface**: Abstraction for data access

### Data Layer
- **Local Database**: SQLite/Room database for local persistence
- **Remote API**: REST/GraphQL endpoints
- **Repository Implementation**: Concrete data access logic
- **Cache Manager**: In-memory caching for performance

### External Services
- **Network Service**: HTTP/Socket communication
- **File Storage**: Persistent file system operations

## Design Patterns

- **MVVM**: Model-View-ViewModel architecture
- **Clean Architecture**: Separation of concerns across layers
- **Repository Pattern**: Abstraction of data sources
- **Dependency Injection**: Inversion of control (likely using Hilt)

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Android Framework / Jetpack Compose
- **Architecture**: Clean Architecture with MVVM
- **Local Storage**: Room Database
- **HTTP Client**: Retrofit/OkHttp
- **Dependency Injection**: Hilt

---

*Last Updated: 2026-06-28*
