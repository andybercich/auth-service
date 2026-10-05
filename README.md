# Auth Service

## Overview

The Auth Service is responsible for authentication and user management within the system.

It manages user registration, login, password hashing, JWT generation, user administration, and role assignment.

The service owns its own database and does not share user tables with other microservices.

The API Gateway exposes the authentication endpoints externally, while the Auth Service independently validates JWTs for protected operations.

## Responsibilities

The Auth Service is responsible for:

* User registration.
* User login.
* Password hashing.
* JWT generation and validation.
* User management.
* User roles.
* User activation and deactivation.
* Authentication-related validation and error handling.

It does not participate in the Kafka event-driven communication used by the operational and analytics services.

## Technology Stack

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* MySQL
* JWT / JJWT
* BCrypt
* Spring Cloud Eureka
* Gradle
* Docker

## User Management

Users are represented by the `User` entity.

The entity contains:

```text
id
username
password
role
enabled
```

The database table is:

```text
auth_users
```

The username is unique and cannot be duplicated.

Passwords are never stored as plain text. BCrypt is used to hash passwords before they are persisted.

The `enabled` field allows an account to be disabled without deleting the user from the database.

## User Registration

User creation follows this general flow:

```text
Create User Request
        ↓
Input validation
        ↓
Check username uniqueness
        ↓
Hash password with BCrypt
        ↓
Persist user
        ↓
Return UserResponse
```

If the username already exists, the operation is rejected.

The password is not exposed through user responses.

User creation can also be used by administrators to create accounts with a specific role.

## Login

Users authenticate using their username and password.

The login flow is:

```text
Username + Password
        ↓
Find user
        ↓
Validate credentials
        ↓
Check account status
        ↓
Generate JWT
        ↓
Return access token
```

Only an access token is used. The service does not implement refresh tokens.

For security reasons, invalid credentials are handled uniformly.

A nonexistent user, an incorrect password, or a disabled user results in an authentication failure rather than exposing whether a particular username exists.

## JWT Authentication

The Auth Service generates signed JWT access tokens after successful authentication.

The token contains the following claims:

```text
sub       → username
userId    → user ID
role      → user role
iat       → issued-at timestamp
exp       → expiration timestamp
```

The `sub` claim contains the username.

The `userId` claim identifies the authenticated user.

The `role` claim is used by the security layer to determine authorization.

JWTs are signed using an HMAC signing key created from the configured secret.

The secret is provided through the `JWT_SECRET` environment variable rather than being stored directly in the source code.

## JWT Expiration

Access tokens have a configured expiration time of:

```text
86,000,000 milliseconds
```

No refresh token mechanism is currently implemented.

Once the access token expires, the client must authenticate again to obtain a new token.

## Roles

The system currently defines two roles:

```text
ADMIN
USER
```

Authorization is distributed across the microservices.

For example, the Product Service restricts product, category, and pack creation, modification, and deletion to `ADMIN`, while authenticated users can perform the permitted read operations.

The Order and Analytics services apply their own role-based authorization according to their responsibilities.

This means Auth Service is responsible for establishing the authenticated identity and role, while individual services enforce authorization for their own resources.

## User Administration

The Auth Service provides user management operations including:

* Creating users.
* Listing users.
* Retrieving the authenticated user's information.
* Retrieving a user by ID.
* Disabling users.

The authenticated user can access their own information through the `/me` operation.

Disabled users cannot authenticate successfully.

Disabling an account changes its enabled state rather than deleting the user record.

## Password Security

Passwords are hashed using BCrypt before being stored.

For example, a password such as:

```text
password123
```

is never stored directly in the database.

The persisted value is a BCrypt hash beginning with a BCrypt identifier such as:

```text
$2...
```

Passwords are also excluded from `UserResponse` objects.

The service applies validation rules to incoming password data before processing it.

## Security Architecture

The Auth Service is itself protected by Spring Security.

The API Gateway is the public HTTP entry point of the architecture and validates JWTs before forwarding authenticated requests to internal services.

The Auth Service also validates JWT authentication independently for its protected endpoints.

This provides an additional security layer instead of relying exclusively on the Gateway.

The login operation is public because users must be able to obtain a JWT before accessing protected resources.

User management operations require authentication and role-based authorization according to the configured security rules.

## API Gateway Integration

External authentication requests follow this architecture:

```text
Client
  ↓
API Gateway
  ↓
Auth Service
  ↓
Authentication / JWT generation
```

The Gateway discovers Auth Service through Eureka.

Auth Service is registered in Eureka under:

```text
auth-service
```

The Gateway can therefore route requests to the service using service discovery rather than relying on a fixed service IP.

## Database

Auth Service owns a dedicated MySQL database:

```text
auth_service
```

The database contains the authentication-related data owned by the service.

Other microservices do not access the Auth Service tables directly.

This follows the database-per-service principle used by the architecture.

```text
Auth Service
      ↓
auth_service database
```

Other services access authentication functionality through the service APIs rather than directly querying its database.

## Error Handling

The service uses a global `@RestControllerAdvice` for application-level error handling.

Validation errors return a structured response containing a specific error code and validation information.

For example:

```json
{
  "code": "VALIDATION_ERROR",
  "errors": {
    "username": "..."
  }
}
```

Authentication failures return:

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Credenciales inválidas"
}
```

Other handled cases include:

```text
INVALID_CREDENTIALS
USER_NOT_FOUND
USERNAME_ALREADY_EXISTS
VALIDATION_ERROR
INTERNAL_ERROR
```

Unexpected errors are converted into a generic internal error response instead of exposing implementation details.

Authentication failures return HTTP `401 Unauthorized`.

This includes invalid credentials and authentication failures involving nonexistent or disabled users.

## Security Against User Enumeration

Authentication failures are intentionally handled without revealing whether a username exists.

For example, these situations produce an authentication failure:

```text
Existing user + incorrect password
Nonexistent user + password
Disabled user
```

This prevents the authentication endpoint from unnecessarily exposing information that could be used to determine which usernames exist.

## Observability

The service uses structured application logging and includes relevant identifiers in log messages without logging passwords or other sensitive credentials.

Authentication-related operations can therefore be traced through the application logs while keeping credential information out of the logs.

The service is also integrated with the application's Eureka-based service discovery architecture.

## Kafka and Redis

Auth Service does not produce or consume Kafka events.

It does not use Redis directly.

Kafka is used by other services for event-driven communication, while Redis is used by the API Gateway for rate limiting.

This keeps authentication independent from the event-driven analytics flow.

```text
Auth Service
   │
   ├── MySQL
   └── Eureka

API Gateway
   └── Redis

Product / Order / Analytics
   └── Kafka
```

## Testing

The Auth Service includes integration-oriented tests using Spring Boot, MockMvc, and the application's real service components.

The test suite covers important authentication and security scenarios.

### Successful login

A successful login must generate a non-empty JWT containing:

```text
username
userId
role
issued-at timestamp
expiration timestamp
```

### Invalid password

An incorrect password results in:

```text
401 Unauthorized
```

### Nonexistent user

A login attempt using a nonexistent username also results in:

```text
401 Unauthorized
```

### Disabled user

A disabled user cannot authenticate and receives:

```text
401 Unauthorized
```

### Password hashing

The tests verify that the stored password:

* Is not equal to the original password.
* Uses BCrypt.

### Duplicate username

Attempting to create an existing username results in a `DuplicateUsernameException`.

### DTO validation

Invalid authentication input produces:

```text
400 Bad Request
```

with a `VALIDATION_ERROR` response.

### Protected endpoints

Requests to protected endpoints without authentication are rejected with:

```text
401 Unauthorized
```

### Administrative user creation

The tests verify that an administrator can create another user and that the password is not exposed in the HTTP response.

## Reliability and Service Isolation

Auth Service has its own database and authentication logic.

Other services do not depend on direct database access to authenticate users.

JWTs allow the Gateway and downstream services to validate authentication without requiring a database lookup for every request.

The authentication service itself does not depend on Kafka or Redis for its core authentication flow.

## Docker and Deployment

Auth Service is containerized and deployed as an internal microservice.

External clients should not connect directly to the Auth Service container.

The intended external flow is:

```text
Client
  ↓
API Gateway
  ↓
Auth Service
```

In the deployed architecture, the Auth Service communicates with Eureka and its own MySQL database through the internal network.

Only the API Gateway is exposed as the public HTTP entry point.

## Configuration

Environment-specific configuration is externalized through environment variables.

Important configuration includes:

```text
JWT_SECRET
JWT expiration configuration
Database connection
Eureka connection
```

The JWT secret and database credentials are not stored directly in the source code or committed to the repository.

## Architecture Role

Auth Service is the identity and authentication component of the microservice architecture.

Its primary responsibility is to authenticate users and issue signed JWT access tokens containing the user's identity and role.

The overall authentication flow is:

```text
                    ┌──────────────┐
                    │    Client    │
                    └──────┬───────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │   API Gateway    │
                  │                  │
                  │ JWT validation   │
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │   Auth Service   │
                  │                  │
                  │ Login            │
                  │ User management  │
                  │ JWT generation   │
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │  auth_service    │
                  │     MySQL        │
                  └──────────────────┘
```

After authentication, the issued JWT is used throughout the architecture.

The API Gateway validates the token at the external entry point, while individual microservices also apply their own security rules and authorization policies.

This keeps authentication centralized while allowing each microservice to remain responsible for authorization of its own resources.
