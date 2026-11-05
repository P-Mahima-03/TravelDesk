# TravelDesk – AI Development Instructions

## 1. Project Overview

TravelDesk is an advanced Travel Agency Management and Online Booking Platform.

The application allows customers to explore destinations, search travel packages, manage bookings, organize itineraries, and access travel services.

The platform supports four roles:

- ADMIN
- TRAVEL_AGENT
- CUSTOMER
- SUPPORT_STAFF

## 2. Mandatory Technology Stack

### Frontend
- React.js
- Vite
- JavaScript ES6+
- HTML5 and JSX
- CSS3
- React Router
- Axios

### Backend
- Java 17+
- Spring Boot 3.x
- Spring MVC
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- Maven

### Database
- MySQL 8
- Flyway migrations

### Testing
- JUnit 5
- Mockito
- Vitest
- React Testing Library

Do not introduce TypeScript, Node.js backend, PHP, Python backend, or React Native.

## 3. Architecture Rules

- Follow a modular monolith architecture.
- Frontend communicates with backend through REST APIs.
- Backend follows Controller, Service, Repository, Entity, DTO, and Mapper layers.
- Keep business logic inside service classes.
- Use DTOs for API request and response objects.
- Use Spring Security for authentication and authorization.
- Store passwords only as secure hashes.
- Use environment variables for credentials and secrets.
- Use Flyway for database schema changes.
- Never use Hibernate automatic schema creation in production.

## 4. Frontend Development Rules

- Use reusable React components.
- Use functional components and React Hooks.
- Keep API calls inside service modules.
- Use React Router for navigation.
- Create role-based protected routes.
- Keep CSS organized and maintainable.
- Build responsive interfaces for desktop, tablet, and mobile.
- Provide loading, empty, success, and error states.
- Do not create fake buttons or non-functional interactions.
- Avoid unnecessary gradients, glowing effects, and excessive animations.
- Maintain consistent typography, spacing, colors, and component styles.

## 5. Backend Development Rules

- Use clear package structures.
- Follow REST API conventions.
- Validate incoming request data.
- Handle exceptions centrally.
- Use meaningful HTTP status codes.
- Avoid exposing entity objects directly in API responses.
- Use transactions for important booking and payment operations.
- Enforce role-based access control.
- Write unit and integration tests for important business logic.

## 6. Database Rules

- Use meaningful table and column names.
- Define appropriate primary keys and foreign keys.
- Add indexes for frequently searched fields.
- Use DECIMAL for monetary values.
- Use unique constraints where required.
- Maintain database changes through versioned Flyway migrations.
- Never commit database passwords or personal credentials.

## 7. Git and GitHub Rules

- Every assigned task must have its own focused feature branch.
- Use descriptive branch names.
- Keep every Pull Request limited to one clearly defined task.
- Never combine unrelated features into one PR.
- Never create fake commits or duplicate functionality to increase contribution counts.
- Review changes before committing.
- Run relevant tests before opening a PR.
- Do not push directly to the main branch.
- Merge only after reviewing the PR and verifying its dependencies.
- Preserve existing work and never overwrite another contributor's changes.

## 8. Branch Naming Convention

Use these prefixes:

- feat/ – New features
- fix/ – Bug fixes
- chore/ – Project configuration and maintenance
- docs/ – Documentation
- test/ – Testing improvements
- refactor/ – Code restructuring

Example:

chore/project-structure

## 9. AI Coding Assistant Rules

Before modifying code:

1. Inspect the existing project structure.
2. Read relevant source files.
3. Understand existing implementations.
4. Identify dependencies and affected files.
5. Implement only the requested task.
6. Avoid unrelated modifications.
7. Run relevant validation and tests.
8. Report all modified files and commands executed.
9. Mention any remaining issues honestly.

Never claim that functionality or tests are complete unless they have actually been implemented and verified.

## 10. Current Development Task

PR 01 – Initialize TravelDesk Project Structure.

Branch:

chore/project-structure

Scope:

- Establish the root project directory.
- Create frontend, backend, database, and docs directories.
- Add README.md.
- Add .gitignore.
- Add AGENTS.md.

Do not implement React setup, Spring Boot setup, database schemas, authentication, or business modules in PR 01. Those belong to separate Pull Requests.

## 11. Project Quality Standards

- Write readable, maintainable code.
- Use meaningful names.
- Avoid duplicate implementations.
- Keep responsibilities separated.
- Validate functionality before marking tasks complete.
- Update documentation when relevant.
- Prioritize working functionality, security, and maintainability over unnecessary code volume.