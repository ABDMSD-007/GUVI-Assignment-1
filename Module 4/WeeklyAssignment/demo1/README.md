# Secure EMI Loan Management API

A production-style Spring Boot backend for a leading NBFC (Non-Banking Financial Company) that provides instant personal loans. Customers apply for loans, repay monthly EMIs, view repayment history, and branch managers monitor collections and overdue accounts.

The application demonstrates **JPA entity mapping, derived queries, advanced JPQL, DTO projections, pagination, JWT security, role-based authorization, global exception handling, transactional updates, and performance optimization** — mirroring real enterprise coding tests conducted by banks, NBFCs, and fintech companies.

---

## Table of Contents

1. [Technology Stack](#technology-stack)
2. [Project Structure](#project-structure)
3. [Domain Model & Entity Relationships](#domain-model--entity-relationships)
4. [How the Code Works (Request Lifecycle)](#how-the-code-works-request-lifecycle)
5. [Security: JWT + Role-Based Authorization](#security-jwt--role-based-authorization)
6. [Layer-by-Layer Detailed Explanation](#layer-by-layer-detailed-explanation)
7. [Task-by-Task Mapping](#task-by-task-mapping)
8. [API Endpoints](#api-endpoints)
9. [Setup & Running](#setup--running)
10. [Testing](#testing)
11. [Performance: Avoiding the N+1 Problem](#performance-avoiding-the-n1-problem)

---

## Technology Stack

| Technology | Purpose |
|-----------|---------|
| **Java 17** | Language |
| **Spring Boot 4.1.0** | Application framework |
| **Spring Data JPA** | ORM / repository abstraction |
| **PostgreSQL** | Relational database |
| **Spring Security** | Authentication & authorization |
| **JWT (jjwt 0.12.5)** | Stateless token-based auth |
| **Bean Validation** | Request/entity validation |
| **Lombok** | Boilerplate reduction (getters/setters/constructors) |
| **JUnit 5 + Mockito** | Unit testing |

---

## Project Structure

```
src/main/java/com/example/demo/
├── Demo1Application.java          # Spring Boot entry point
├── entity/                        # JPA entities (DB tables)
│   ├── Customer.java
│   ├── Loan.java
│   ├── EMITransaction.java
│   └── Penalty.java
├── dto/                           # Data Transfer Objects (no entities leak out)
│   ├── CustomerSummaryDTO.java
│   ├── BranchCollectionDTO.java
│   ├── MonthlyCollectionDTO.java
│   ├── DashboardDTO.java
│   └── AuthRequest.java
├── repository/                    # Spring Data JPA repositories
│   ├── CustomerRepository.java
│   ├── LoanRepository.java
│   ├── EMITransactionRepository.java
│   └── PenaltyRepository.java
├── service/
│   └── LoanService.java           # Business-logic interface
├── serviceimpl/
│   └── LoanServiceImpl.java       # Business-logic implementation
├── controller/                    # REST endpoints
│   ├── AuthController.java        # /login
│   ├── CustomerController.java    # /customers/register
│   ├── LoanController.java        # /loans/**
│   └── DashboardController.java   # /dashboard
├── security/                      # JWT + Spring Security wiring
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   ├── CustomerUserDetailsService.java
│   └── SecurityConfig.java
└── exception/                     # Centralized error handling
    ├── GlobalExceptionHandler.java
    ├── CustomerNotFoundException.java
    ├── LoanNotFoundException.java
    └── EMINotFoundException.java
```

---

## Domain Model & Entity Relationships

```
Customer
  | 1
  | *
Loan
  | 1
  |----------------------*
  |                      |
EMITransaction       Penalty
```

- **Customer** can have **many** Loans.
- **Loan** belongs to one Customer; has **many** EMITransactions and **many** Penalties.
- **EMITransaction** and **Penalty** each belong to one Loan.

### Entity Field Reference

**Customer** — `customerId`, `customerName`, `email`, `password`, `mobileNumber`, `branchName`, `creditScore`, `role`
**Loan** — `loanId`, `loanType` (PERSONAL/HOME/VEHICLE/EDUCATION), `principalAmount`, `interestRate`, `tenureMonths`, `emiAmount`, `loanStatus` (ACTIVE/CLOSED/DEFAULTED), `active` (soft-delete flag)
**EMITransaction** — `transactionId`, `installmentNumber`, `amountPaid`, `paymentDate`, `paymentMode` (UPI/CARD/NETBANKING/CASH), `paymentStatus` (PAID/MISSED/PENDING)
**Penalty** — `penaltyId`, `penaltyAmount`, `reason`, `penaltyDate`

### Mapping Details

- Relationships use `@OneToMany(mappedBy=..., cascade=CascadeType.ALL, fetch=FetchType.LAZY)` and `@ManyToOne(fetch=FetchType.LAZY)`.
- Collections are initialized (`= new ArrayList<>()`) to **prevent NullPointerExceptions**.
- `@ToString.Exclude` and `@EqualsAndHashCode.Exclude` break circular references and prevent accidental lazy-loading in `toString()`.
- **Type-safe enums** (`com.example.demo.enums`) replace magic strings and are persisted as text via `@Enumerated(EnumType.STRING)`:
  `LoanType`, `LoanStatus`, `PaymentMode`, `PaymentStatus`, `Role`. Repository queries reference them by fully-qualified name (e.g. `com.example.demo.enums.LoanType.PERSONAL`), and controllers bind path variables straight to the enum (invalid values return `400` via `MethodArgumentTypeMismatchException`).
- **DTOs are immutable `record`s** (`BranchCollectionDTO`, `CustomerSummaryDTO`, `MonthlyCollectionDTO`, `DashboardDTO`, `AuthRequest`, `ErrorResponse`) — JPQL constructor expressions map directly onto the record's canonical constructor.

---

## How the Code Works (Request Lifecycle)

A typical authenticated request (e.g. `GET /loans`) flows like this:

1. **HTTP request arrives** with header `Authorization: Bearer <token>`.
2. **`JwtFilter`** intercepts the request, extracts the token, validates it via `JwtUtil`, loads the user with `CustomerUserDetailsService`, and sets the `SecurityContext`.
3. **`SecurityConfig`** rules confirm the URL requires authentication (everything except `/login` and `/customers/register`).
4. **`@PreAuthorize`** on the controller method enforces the correct role (USER/MANAGER/ADMIN).
5. **`LoanController`** receives the call and delegates to `LoanService`.
6. **`LoanServiceImpl`** runs business logic, calling **repositories** for data access.
7. **Repositories** execute JPQL / derived queries against PostgreSQL.
8. Results are mapped to **DTOs** and returned as JSON.
9. If anything throws, **`GlobalExceptionHandler`** converts it into a clean JSON error with `timestamp/status/message/path`.

---

## Security: JWT + Role-Based Authorization

### Flow
1. `POST /login` with `{ "username": "<email>", "password": "<pwd>" }`.
2. `AuthController` authenticates via `AuthenticationManager` → `DaoAuthenticationProvider` → `CustomerUserDetailsService` (loads customer by email) + `BCryptPasswordEncoder`.
3. On success, `JwtUtil.generateToken(...)` returns a signed JWT (valid 1 hour).
4. Client sends `Authorization: Bearer <token>` on every other request.

### Roles & Permissions

| Role | Capabilities |
|------|-------------|
| **USER** | View loans, view EMI schedule, pay EMI |
| **MANAGER** | + Update interest rate, approve loan, view dashboard, foreclose loan |
| **ADMIN** | + Delete loan (everything via role hierarchy) |

Enforced with `@PreAuthorize("hasRole('...')")` and a **role hierarchy** `ROLE_ADMIN > ROLE_MANAGER > ROLE_USER`, so higher roles inherit lower ones.

---

## Layer-by-Layer Detailed Explanation

### Entities (`entity/`)
JPA-annotated classes that map to DB tables. Lombok `@Data` generates getters/setters; `@NoArgsConstructor`/`@AllArgsConstructor` give constructors. Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, `@Positive`, `@PositiveOrZero`, `@NotNull`) validate input at the boundary.

### DTOs (`dto/`)
Plain carriers that prevent leaking entities to the client and enable **JPQL constructor expressions** (e.g. `CustomerSummaryDTO`) for efficient projections. `AuthRequest` is a record for login.

### Repositories (`repository/`)
Extend `JpaRepository<Entity, Long>`. They contain:
- **Derived queries** (method-name parsing) — e.g. `findByLoanType`.
- **JPQL `@Query`** — aggregates, joins, grouping, constructor projections.
- **`@Modifying @Transactional`** bulk update (interest rate increase).
- **`Pageable`** parameters for pagination & `LIMIT`-style queries.

### Service (`service/` + `serviceimpl/`)
`LoanService` is the contract; `LoanServiceImpl` holds the business rules: pagination, soft-delete, foreclosure validation, dashboard aggregation, eligibility threshold (injected via `@Value`).

### Controllers (`controller/`)
Thin REST layer. Validate input (`@Valid`), enforce roles (`@PreAuthorize`), delegate to the service, return `ResponseEntity`.

### Security (`security/`) & Exceptions (`exception/`)
JWT plumbing and a `@RestControllerAdvice` that maps exceptions to structured JSON errors.

---

## Task-by-Task Mapping

| Task | What it covers | Where |
|------|----------------|-------|
| 1 | Entity mapping (`@OneToMany`, `@ManyToOne`, `mappedBy`, `CascadeType.ALL`, `LAZY`, initialized lists) | `entity/*` |
| 2 | Bean Validation | `entity/*` |
| 3 | Derived queries | `*Repository` |
| 4 | JPQL: >N loans, branch collection, multiple loan types, latest EMI, no-penalty loans, top 5 by EMI | `Customer/Loan/EMITransactionRepository` |
| 5 | `@Modifying`+`@Transactional` interest increase (+0.5% on PERSONAL) | `LoanRepository.increaseInterestRate()` |
| 6 | Pagination & sorting (`emiAmount DESC`) | `LoanController.getLoans()` |
| 7 | DTO projection via constructor expression | `CustomerSummaryDTO`, `getCustomerSummaries()` |
| 8 | JWT auth (UserDetailsService, AuthManager, encoder, filter, util, config) | `security/*` |
| 9 | Role-based authorization | `@PreAuthorize` in controllers |
| 10 | Global exception handling (timestamp/status/message/path) | `GlobalExceptionHandler` |
| Final | `/dashboard` analytics via minimal optimized queries | `DashboardController`, `getDashboard()` |
| Bonus | Overdue detection, foreclosure, top 10 branches, eligibility, monthly report, soft delete | `LoanService` + repos |

---

## API Endpoints

### Auth (public)
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/login` | public | Returns a JWT |
| POST | `/customers/register` | public | Register a customer |

### Loans
| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/loans` | USER | Paginated, sorted by emiAmount DESC |
| GET | `/loans/{id}` | USER | Loan by id |
| GET | `/loans/{id}/schedule` | USER | EMI schedule |
| POST | `/loans/{id}/pay` | USER | Pay an EMI |
| PUT | `/loans/{id}/approve` | MANAGER | Approve loan |
| PUT | `/loans/increase-interest` | MANAGER | +0.5% on PERSONAL |
| PUT | `/loans/{id}/foreclose` | MANAGER | Close if cleared |
| DELETE | `/loans/{id}` | ADMIN | Soft delete |

### Analytics / Queries
| Method | Path | Description |
|--------|------|-------------|
| GET | `/dashboard` (MANAGER) | Aggregated stats |
| GET | `/loans/branch-collection` | EMI sum per branch |
| GET | `/loans/customers/min-loans/{n}` | Customers with > n loans |
| GET | `/loans/customers/multiple-loan-types` | Multiple loan types |
| GET | `/loans/latest-payment` | Latest EMI |
| GET | `/loans/no-penalty` | Loans with no penalties |
| GET | `/loans/top5-emi` | Top 5 EMI payers |
| GET | `/loans/customer-summaries` | DTO projection summary |
| GET | `/loans/overdue` | Pending EMIs > 30 days |
| GET | `/loans/top10-branches` | Top 10 branches |
| GET | `/loans/eligible-customers` | Credit score > threshold |
| GET | `/loans/monthly-report` | Monthly collection report |

---

## API Documentation (Swagger / OpenAPI)

Interactive docs are auto-generated with **springdoc-openapi**:

| Resource | URL |
|----------|-----|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

- A global **`bearerAuth`** JWT scheme is configured (`OpenApiConfig`). Click **Authorize** in Swagger UI and paste the token from `POST /login` to call secured endpoints.
- Swagger URLs are whitelisted in `SecurityConfig`; `/login` and `/customers/register` are marked public via `@SecurityRequirements`.
- Endpoints are grouped by `@Tag` and described with `@Operation`.

---

## Logging

Uses **SLF4J** (Lombok `@Slf4j`) across controllers, the service layer and the JWT filter:

- Request-level `INFO` logs on every controller action; `WARN` on soft-delete/foreclosure blocks.
- Business events logged in `LoanServiceImpl` (approve, pay EMI, interest increase, foreclosure).
- Auth tracing in `JwtFilter` (`DEBUG` on success, `WARN` on invalid token).
- Configured in `application.properties`: console pattern, per-package levels (`com.example.demo=DEBUG`), and a rolling file at `logs/emi-application.log` (10MB, 7 days history).

---

## Setup & Running

### Prerequisites
- Java 17, PostgreSQL running locally.
- Create DB matching `application.properties`:
  ```ini
  spring.datasource.url=jdbc:postgresql://localhost:5432/nacl
  spring.datasource.username=postgres
  spring.datasource.password=12345
  ```

### Run
```powershell
.\mvnw.cmd spring-boot:run
```

### Quick test
```powershell
# Register
curl -X POST http://localhost:8080/customers/register -H "Content-Type: application/json" -d '{"customerName":"Rahul","email":"rahul@nbfc.com","password":"pass","mobileNumber":"9999999999","branchName":"Bangalore","creditScore":750,"role":"MANAGER"}'

# Login -> copy token
curl -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"rahul@nbfc.com","password":"pass"}'

# Call dashboard
curl http://localhost:8080/dashboard -H "Authorization: Bearer <token>"
```

---

## Testing

The suite has **65 tests**: fast unit tests plus full integration tests.

```powershell
.\mvnw.cmd test
```

### Unit tests (JUnit 5 + Mockito, no DB)
- `LoanServiceImplTest` — business logic & edge cases (not-found, soft delete, foreclosure blocks, empty dashboard, top-10 trim).
- `JwtUtilTest` — token generate/validate/expire/tamper.
- `CustomerUserDetailsServiceTest` — user loading, default role, missing user.
- `GlobalExceptionHandlerTest` — error response structure.

### Integration tests (Spring context + in-memory **H2**, `test` profile)
Repository tests (`@SpringBootTest` + `@Transactional`, seeded via `EntityManager`) exercise the real JPQL/derived queries against a database:
- `LoanRepositoryIntegrationTest` — derived queries, branch aggregation, soft-delete filter, no-penalty anti-join, bulk interest update, overdue detection.
- `CustomerRepositoryIntegrationTest` — `findByEmail`, HAVING queries, eligibility, constructor-expression projection.
- `EMITransactionRepositoryIntegrationTest` — paid totals, latest payment, monthly report, penalty aggregation.

Web/security tests (`@SpringBootTest` + `MockMvc`, real JWT + filter chain):
- `CustomerRegistrationIntegrationTest` — registration, **409 duplicate email**, 400 validation.
- `SecurityIntegrationTest` — JWT login, wrong-password **401**, unauthenticated rejection, role-based access (USER vs MANAGER **403**, ADMIN inherits via hierarchy).
- `LoanApiIntegrationTest` — **404** not-found, **400** invalid enum path variable, paged JSON.

Test config lives in `src/test/resources/application-test.properties` (H2, `create-drop`, test JWT secret).

---

## Error Handling & Custom Exceptions

`GlobalExceptionHandler` (`@RestControllerAdvice`) returns a typed `ErrorResponse` (`timestamp`, `status`, `message`, `path`) for:

| Exception | HTTP |
|-----------|------|
| `CustomerNotFoundException`, `LoanNotFoundException`, `EMINotFoundException` | 404 |
| `DuplicateEmailException` (registering an existing email) | 409 |
| `InvalidLoanOperationException` (e.g. foreclosing with pending/missed EMIs) | 400 |
| `MethodArgumentNotValidException`, `ValidationException`, `IllegalArgumentException`, `MethodArgumentTypeMismatchException` (bad enum path var) | 400 |
| `AuthenticationException` (bad login) | 401 |
| `AccessDeniedException` (insufficient role) | 403 |
| any other `Exception` | 500 |

---

## Performance: Avoiding the N+1 Problem

The dashboard and reports never loop entities to fetch children. Instead they use **aggregate JPQL** (`SUM`, `COUNT`, `MAX`), `JOIN`/`LEFT JOIN`, `GROUP BY`, and **constructor expressions**, so the heavy stats compute in a handful of queries rather than thousands. Loans are soft-deleted (`active=false`) and active-only queries hide them automatically.

