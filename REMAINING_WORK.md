# Remaining Work

This document records the work still required to turn the repository from a functioning auth/profile foundation into a complete TravelDesk platform.

## 1. Backend business modules still missing

### Destination management
- Create destination controller, service, repository usage, DTOs, and validation
- Add CRUD APIs for destinations and admin controls
- Add tests for listing, creating, updating, and deactivating destinations

### Travel package management
- Implement package listing and detail endpoints
- Add admin create/update/delete workflows
- Add filters for destination, price, duration, and availability

### Booking workflows
- Add booking creation flow for customers
- Add traveller payload management
- Add booking status transitions and validation
- Add booking retrieval and history endpoints

### Payment and refunds
- Implement payment creation and status tracking
- Add refund logic and payment reconciliation
- Add transaction reference handling and validation

### Reviews and ratings
- Implement review submission and retrieval
- Link reviews to packages or destinations
- Add moderation or approval logic if required by product design

### Support ticketing
- Introduce support tickets and staff assignment flows
- Add visible support dashboard and customer request tracking

## 2. Frontend work still missing

### Customer-facing flows
- destination browsing pages
- package detail pages
- booking form flow
- payment checkout screens
- itinerary/trip management

### Admin and staff portals
- role-based dashboards for admin, travel agent, and support staff
- route guards based on user role
- support operations and booking management tables

### State and UX improvements
- loading/error/empty states for every major list and form
- client-side validation beyond the basic form checks
- error boundaries and consistent feedback patterns

## 3. API and integration work

- Connect frontend booking screens to backend endpoints
- Add cross-service validation between user, package, and booking domains
- Establish consistent API response conventions for all modules
- Add API documentation and examples for customers and staff workflows

## 4. Database and migration work

- Expand Flyway migrations from initial schema to full domain schema
- Add indexes and constraints for booking, payment, and search-heavy queries
- Add seed data for destinations, packages, roles, and demo users
- Clarify the intended location of database assets; root `database/` should be either populated or intentionally deprecated

## 5. Security and role work

- Implement authorization rules by role beyond basic authentication
- Restrict operations to authorized resource owners and staff roles
- Add audit logging for admin and staff actions
- Review JWT and session handling against deeper security expectations

## 6. Testing backlog

### Missing automated coverage for:
- destination and package APIs
- booking creation and payment flows
- admin/agent/support authorization
- integration between frontend and backend
- edge cases for transaction and validation behavior

## 7. Repository and project hygiene items

- Decide whether to keep the root `database/` folder as active migration storage or remove it from planning docs
- Add a real PR #22 branch or explicitly document the missing PR if it is intentionally excluded
- Keep the project aligned with the README business scope and actual implementation status
- Review whether the active `fix/login-api-connectivity` branch should be merged or rolled into a planned release branch

## 8. Recommended next milestone

The immediate next milestone should be a focus on the travel operations domain, not more auth scaffolding. The logical next major workstream is:
1. destination and package domain implementation
2. booking creation and booking status APIs
3. frontend pages and forms for booking journeys
4. staff/admin management views and role restrictions

Without this next milestone, the repository remains a secure but incomplete foundation rather than a complete travel management product.
