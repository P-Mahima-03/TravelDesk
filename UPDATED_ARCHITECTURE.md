# Updated Architecture

## Actual architecture in the current repository

The repository currently reflects a simplified, modular monolith architecture with a frontend app and a backend app, but not a full production-ready operational architecture.

```text
TravelDesk
├── frontend (React + Vite)
│   ├── pages
│   │   ├── Home
│   │   ├── Login
│   │   ├── Register
│   ├── services
│   │   ├── api.js
│   │   ├── auth.js
│   │   └── authError.js
│   ├── components
│   │   └── ProtectedRoute.jsx
│   └── routes
│       └── AppRouter.jsx
│
├── backend (Spring Boot + Maven)
│   ├── controller
│   │   ├── auth
│   │   └── UserController.java
│   ├── service
│   │   └── auth
│   ├── repository
│   ├── entity
│   ├── dto
│   ├── enums
│   ├── security
│   └── resources
│       └── db/migration
│
├── database
│   └── .gitkeep
│
└── docs
    └── .gitkeep
```

## Observed runtime behavior

### Frontend layer
- Vite React app renders routes for Home, Login, and Register
- Auth pages call backend `/api/auth/login` and `/api/auth/register`
- JWT token is stored in browser localStorage
- Protected routes redirect unauthenticated users to `/login`
- Axios interceptors attach bearer tokens to authenticated requests

### Backend layer
- Spring Security is configured with stateless JWT authentication
- `JwtAuthenticationFilter` validates bearer tokens for protected requests
- `AuthController` exposes login and registration endpoints
- `UserController` exposes authenticated profile retrieval and update endpoints
- custom exceptions return validation or auth error payloads

### Data layer
- JPA entities and repository interfaces are present for user and core travel-domain objects
- Flyway is configured and initial migration scripts exist
- the database is designed for a travel platform, but only the initial schema is implemented

## Current architectural reality

The current project is best described as:
- a security-enabled starter app
- a user/authentication foundation
- a partial domain model for travel operations
- a static front-end marketing page plus auth screens

It is not yet a complete booking platform architecture with fully implemented modules for search, reservations, payment, or staff operations.

## Target architecture (recommended future state)

The intended architecture for the product described in the README should evolve toward a more complete modular monolith model:

```text
TravelDesk Platform
├── frontend
│   ├── customer-app
│   ├── admin-portal
│   ├── agent-portal
│   └── support-portal
│
├── backend
│   ├── auth
│   ├── user-management
│   ├── destination-management
│   ├── package-management
│   ├── booking-management
│   ├── payment-management
│   ├── review-management
│   ├── support-management
│   ├── analytics
│   └── shared
│       ├── security
│       ├── validation
│       ├── exceptions
│       └── config
│
├── database
│   ├── flyway
│   ├── seed-data
│   └── reporting
│
├── docs
│   ├── api
│   ├── architecture
│   └── operational-guides
│
└── integrations
    └── external booking / payment services (future)
```

## Architectural gaps identified by this audit

1. Authentication is implemented; business domains are not.
2. The repository model exists for travel entities but no corresponding business APIs are active.
3. The project uses the backend module for migrations even though root docs indicate a dedicated `database/` directory.
4. The frontend is feature-rich at a UI shell level but lacks the actual customer and operations workflows.
5. The app lacks separate role-specific portal boundaries for admin, travel agent, customer, and support staff beyond the role enum and basic auth model.

## Architecture conclusion

The repository is currently in a foundation stage with a valid auth/profile backbone. The next architectural step should not be more auth work; it should be business-domain implementation across destinations, travel packages, bookings, and payments.

The current code base is suitable for incremental growth, but it does not yet represent the end state described by the project scope.
