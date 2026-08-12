# EventTicketingPlatform

## Overview

EventTicketingPlatform, presented in the user interface as Eventix, is a full-stack event discovery, booking, and ticket-management application. Visitors can browse published events, customers can reserve seats and receive tickets, organizers can manage venues and events, and administrators can review applications and supervise the platform.

The project is a monorepo containing an Angular frontend, a Spring Boot backend, PostgreSQL database migrations, Docker configuration, and Kubernetes deployment manifests. The frontend never connects directly to PostgreSQL. It sends HTTP requests to the Spring Boot API, and the API applies security and business rules before reading or writing database records.

## User Roles

The platform supports customers, organizers, and administrators. A new regular account is created with the CUSTOMER role. Organizer access is granted after a guest submits an organizer application and an administrator approves it. Administrators have broader access to users, events, venues, applications, bookings, and tickets.

Anonymous visitors can browse published events, inspect public event details, register a customer account, or submit an organizer application. Protected actions require login and a valid JSON Web Token.

## Customer Experience

Customers can browse events by category, venue, date range, and price range. Public results contain only events whose status is PUBLISHED. Event detail pages display the venue, date, time, description, seat categories, prices, and current seat availability.

A customer reserves seats by selecting a seat category and quantity. The backend checks that the event is published, the category belongs to the event, enough seats remain, and the request does not exceed the category’s per-person limit. The server calculates the total price and creates a PENDING booking that holds the requested inventory for five minutes.

A pending booking can be confirmed or cancelled. Confirmation changes it to CONFIRMED and automatically issues one ticket for the entire booking. Cancellation returns the reserved seats. If a pending booking is not confirmed before its expiry time, the backend marks it EXPIRED and returns its seats to the available inventory.

Customers can inspect their bookings and issued tickets. A confirmed booking has a downloadable PDF ticket containing the event, venue, holder, seat category, party size, total price, ticket code, and QR code.

## Organizer Experience

A prospective organizer submits an application containing identity information, a password, an optional organization name, and an optional explanation. The password is BCrypt-hashed immediately. Approval creates an ORGANIZER account using that existing password hash, while rejection keeps the application as an audit record.

Organizers can submit venue requests and monitor their status. A submitted venue starts as PENDING and cannot be used for an event until an administrator approves it. Organizers can create events only at approved venues.

New events start as DRAFT. An organizer can edit the event, add priced seat categories, and publish it when it has a future date, an approved venue, and at least one valid seat category. A published event appears in the public catalog and becomes available for booking.

Organizers can inspect bookings and tickets associated with their events. Ticket check-in changes an ISSUED ticket to CHECKED_IN and records the check-in time. A cancelled or already checked-in ticket cannot be checked in again.

## Administrator Experience

Administrators review organizer applications and venue requests. Approving an organizer application creates an organizer account. Approving a venue makes it available for event creation. Administrators can also create venues directly, in which case the venue is approved immediately.

Administrators can list users, filter them by role, promote or demote accounts, and delete non-administrator users. The backend prevents the final administrator from being demoted and prevents administrator accounts from being deleted through the user-management API.

Administrators can inspect events in every state, cancel events, inspect tickets by event or ticket UUID, download ticket PDFs, and void issued tickets. Cancelling an event also resolves its active bookings, restores reserved inventory, and voids tickets belonging to confirmed bookings.

## Frontend Architecture

The frontend is located in the `client` directory and uses Angular with standalone components, lazy-loaded routes, Signal Forms, signals, computed state, RxJS, and HttpClient. Components own visible state such as loading indicators, errors, selected filters, forms, and dialogs. Angular services own HTTP communication for authentication, events, venues, seat categories, bookings, tickets, users, and organizer applications.

An authentication interceptor adds the stored bearer token to outgoing requests. An error interceptor converts backend error responses into a consistent frontend error type. Route guards improve navigation by restricting guest, customer, organizer, and administrator screens, while the backend remains the authoritative security boundary.

During local development, the frontend calls `http://localhost:8080/api`. A production build uses the relative `/api` path so Nginx can serve the Angular application and proxy API requests to the backend under the same browser origin.

## Backend Architecture

The backend is located in the `Server` directory and uses Java 21 with Spring Boot. REST controllers receive HTTP input and return DTO responses. Services contain business rules, ownership checks, state transitions, and transaction boundaries. Spring Data repositories perform database operations and define custom filtering and locking queries.

DTOs separate the public API contract from persistence entities. Request DTOs apply validation rules for required fields, lengths, email addresses, phone numbers, prices, quantities, and capacities. Response DTOs return only the data required by clients and do not expose passwords or full persistence graphs.

Centralized exception handling converts validation, authentication, authorization, resource, conflict, and state errors into a consistent JSON response containing a timestamp, HTTP status, error code, message, and optional field errors.

## Authentication and Security

Passwords are stored as BCrypt hashes. Successful login returns a signed JSON Web Token whose subject is the user UUID. The Angular application stores the token and user summary locally and includes the token in protected API requests.

The backend validates the token on every request, reloads the user from the database, and places the current user and authorities in the Spring Security context. Public endpoints include registration, login, organizer application submission, health checks, and public event reads. Customer, organizer, and administrator endpoint groups are protected by role rules, with additional ownership checks applied inside services.

The API uses stateless authentication and does not create server-side sessions. CORS permits configured frontend origins, and CSRF protection is disabled because authentication uses bearer headers instead of session cookies.

## Database Design

PostgreSQL stores users, organizer applications, venues, events, seat categories, bookings, tickets, and optional ticket-attendee associations. UUIDs are used as identifiers, and database foreign keys preserve relationships among records.

Events move through DRAFT, PUBLISHED, and CANCELLED states. Organizer applications and venues move through PENDING, APPROVED, and REJECTED states. Bookings use PENDING, CONFIRMED, CANCELLED, and EXPIRED states. Tickets use ISSUED, CHECKED_IN, and CANCELLED states.

Liquibase manages the schema through ordered changelog files in `Server/src/main/resources/db/changelog`. Hibernate schema generation is disabled, making Liquibase the source of truth for database creation and upgrades.

## Booking Consistency

Seat inventory is protected with database pessimistic write locks. A reservation locks the selected seat-category row before checking and decreasing availability. Booking confirmation, cancellation, and expiration lock the booking before changing its status. These transaction and locking rules prevent concurrent customers from overselling the last available seats or returning the same seats more than once.

A scheduler runs every minute and finds PENDING bookings whose expiry time has passed. Each booking is reloaded and locked before expiration so that the scheduler cannot race incorrectly with a customer confirmation or cancellation.

## Ticket Generation

One ticket is issued for each confirmed booking and covers every seat purchased in that booking. A unique database constraint on the booking reference prevents duplicate tickets, and the ticket-generation service returns the existing ticket if a confirmation request is retried.

Ticket codes use a cryptographically secure random generator and avoid visually ambiguous characters. The PDF service uses OpenPDF to create the document and ZXing to encode the ticket code as a QR code. Only the booking owner or an administrator can download the ticket PDF.

## Technology

The frontend uses Angular 22, TypeScript, RxJS, and npm. The backend uses Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Hibernate, Liquibase, JJWT, OpenPDF, and ZXing. PostgreSQL 16 provides relational persistence. Docker, Nginx, and Kubernetes files support containerized builds and deployment.

## Local Requirements

Local development requires Java 21, Node.js with npm, Docker or an accessible PostgreSQL 16 installation, and available ports for the selected services. The default configuration expects PostgreSQL on port 5432, Spring Boot on port 8080, and Angular on port 4200.

The Docker Compose file starts only PostgreSQL. The Spring Boot backend and Angular frontend must be started separately.

## Starting PostgreSQL

From the repository root, start the development database with `sudo docker compose -f Infrastructure/docker-compose.yaml up -d`. If the normal `docker` command points to an obsolete installation, use `/usr/bin/docker` explicitly.

The default container creates the `eventticketing` database with the `postgres` user and the development password configured in `Infrastructure/docker-compose.yaml`. If port 5432 is already occupied, stop the existing PostgreSQL process or map the container to another host port and override `SPRING_DATASOURCE_URL` when starting the backend.

## Starting the Backend

Enter the `Server` directory and ensure the Maven wrapper is executable with `chmod +x mvnw`. Start the API with `./mvnw spring-boot:run`. Maven must use Java 21 because the project compiler release is set to version 21.

A successful startup connects to PostgreSQL, runs Liquibase migrations, creates the initial administrator if necessary, enables the booking-expiry scheduler, and listens on port 8080. The health endpoint is available at `http://localhost:8080/actuator/health`.

## Starting the Frontend

Enter the `client` directory, install exact dependencies with `npm ci`, and start the Angular development server with `npm start`. Open `http://localhost:4200` after compilation completes.

Running only `ng serve` displays the frontend but does not start the backend or database. Registration, event loading, bookings, and every other persistent action require PostgreSQL and Spring Boot to be running.

## Initial Data

A new database contains no sample venues or events. The public event catalog therefore displays an empty state until an organizer and venue are approved and an event is created, configured, and published.

The development administrator is seeded as `admin@example.com` with password `Admin12345678Admin` unless environment variables override those values. All repository-provided passwords and secrets are development defaults and must be replaced before any real deployment.

## Docker and Kubernetes

The backend Dockerfile builds the application with a Java 21 JDK and runs it with a smaller Java 21 JRE under a non-root user. The frontend Dockerfile builds Angular with Node.js and serves the static output through Nginx.

The Nginx configuration returns `index.html` for client-side routes and proxies `/api` requests to the Kubernetes backend service. Kubernetes manifests define a dedicated namespace, frontend and backend deployments, internal services, NodePort exposure, health probes, resource limits, PostgreSQL storage, and configuration through ConfigMaps and Secrets.

## Testing

Backend tests cover event, venue, and seat-category business rules, repository filtering, and booking concurrency. The concurrency tests use real transactions and PostgreSQL behavior to verify last-seat races, overselling prevention, and safe interaction between confirmation, cancellation, and expiration.

Run backend tests from the `Server` directory with `./mvnw test` while the configured PostgreSQL database is available. Run frontend tests from the `client` directory with `npm test`.

## Development Status

The repository contains the main customer, organizer, and administrator workflows, but some implemented backend capabilities are not fully connected to the current navigation. The Docker Compose file is database-only, the administrator dashboard API has no registered Angular dashboard route, and the richer booking-management component is not the component currently registered for the customer booking list.

Organizer ticket endpoints should also enforce event ownership inside the backend service rather than relying on the user interface to supply only owned event identifiers. Development secrets should be moved out of committed files, and production deployments should use HTTPS and externally managed credentials.

## License

No license is currently declared in the repository. Add a license file before distributing or reusing the project outside its intended environment.
