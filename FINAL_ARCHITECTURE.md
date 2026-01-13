# Backend Service - Final Architecture Documentation

## 🎯 Tổng Quan (Overview)

Backend Service là core system của ứng dụng email, được xây dựng bằng Spring Boot (Java 21). Đây là trung tâm xử lý toàn bộ business logic, authentication, Gmail API integration, và orchestration giữa các services.

---

## 🤔 Tại Sao Phải Dùng Spring Boot? (Why Spring Boot?)

### 1. **Enterprise-Grade Framework**

- **Lý do**: Ứng dụng email cần độ tin cậy cao, bảo mật tốt, và khả năng mở rộng
- **Spring Boot**: Framework được trust bởi hàng triệu doanh nghiệp worldwide
- **Lợi ích**: Production-ready features out-of-the-box (security, monitoring, logging)

### 2. **Robust Security**

- **Spring Security**: Best-in-class security framework
- **OAuth2 Support**: Native support cho Google OAuth2
- **JWT Management**: Built-in token handling
- **Lợi thế**: Không cần reinvent the wheel, đã được battle-tested

### 3. **Rich Ecosystem**

- **Dependencies**: Hàng nghìn libraries được maintain tốt
- **Integration**: Dễ tích hợp MongoDB, Redis, Kafka, etc.
- **Community**: Community lớn, dễ tìm solutions
- **Documentation**: Docs cực kỳ chi tiết

### 4. **Scalability & Performance**

- **Multi-threading**: Xử lý concurrent requests hiệu quả
- **Connection Pooling**: Optimize database connections
- **Caching**: Built-in cache abstraction
- **Load Balancing**: Easy to scale horizontally

---

## 💡 Lợi Ích Cụ Thể (Specific Benefits)

### 1. **Type Safety**

- Java static typing catches errors at compile-time
- IntelliJ IDEA support: Amazing auto-complete & refactoring
- Reduce runtime errors by 80%+

### 2. **Maintainability**

- Clean architecture: Controllers → Services → Repositories
- Dependency Injection: Loose coupling, easy testing
- Standardized patterns: Team onboarding nhanh

### 3. **Integration with Gmail**

- **Google API Client**: Official Java library
- **Reliable**: Handles rate limits, retries, auth refresh
- **Complete**: Full access to Gmail API features

### 4. **Business Logic Centralization**

- **Single Source of Truth**: All business rules ở Backend
- **Consistency**: Frontend chỉ là UI layer
- **Security**: Sensitive logic không expose ra client

---

## 🏗️ Kiến Trúc Chi Tiết (Detailed Architecture)

### **Cấu Trúc Thư Mục (Folder Structure)**

```
backend/
├── pom.xml                           # Maven dependencies
├── Dockerfile                        # Container definition
├── src/
│   ├── main/
│   │   ├── java/com/hcmus/awad/
│   │   │   ├── AwadEmailApplication.java      # Main Spring Boot app
│   │   │   │
│   │   │   ├── config/                        # 🔹 Configuration
│   │   │   │   ├── SecurityConfig.java        # Spring Security setup
│   │   │   │   ├── WebConfig.java             # CORS, interceptors
│   │   │   │   ├── MongoConfig.java           # MongoDB connection
│   │   │   │   └── GmailConfig.java           # Gmail API client
│   │   │   │
│   │   │   ├── controller/                    # 🔹 REST Controllers
│   │   │   │   ├── AuthController.java        # Authentication endpoints
│   │   │   │   ├── EmailController.java       # Email CRUD operations
│   │   │   │   ├── KanbanController.java      # Kanban board management
│   │   │   │   └── SearchController.java      # Search & filters
│   │   │   │
│   │   │   ├── service/                       # 🔹 Business Logic Layer
│   │   │   │   ├── AuthService.java           # OAuth2, JWT handling
│   │   │   │   ├── GmailService.java          # Gmail API operations
│   │   │   │   ├── EmailService.java          # Email business logic
│   │   │   │   ├── KanbanService.java         # Kanban orchestration
│   │   │   │   ├── AIServiceClient.java       # Call AI service
│   │   │   │   └── TokenService.java          # Token refresh management
│   │   │   │
│   │   │   ├── repository/                    # 🔹 Data Access Layer
│   │   │   │   ├── UserRepository.java        # User CRUD
│   │   │   │   ├── EmailRepository.java       # Email metadata storage
│   │   │   │   └── KanbanBoardRepository.java # Board state persistence
│   │   │   │
│   │   │   ├── model/                         # 🔹 Domain Models
│   │   │   │   ├── User.java                  # User entity
│   │   │   │   ├── Email.java                 # Email metadata
│   │   │   │   ├── KanbanBoard.java           # Board structure
│   │   │   │   └── RefreshToken.java          # OAuth tokens
│   │   │   │
│   │   │   ├── dto/                           # 🔹 Data Transfer Objects
│   │   │   │   ├── request/                   # Request DTOs
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── EmailRequest.java
│   │   │   │   │   └── SearchRequest.java
│   │   │   │   └── response/                  # Response DTOs
│   │   │   │       ├── AuthResponse.java
│   │   │   │       ├── EmailResponse.java
│   │   │   │       └── PagedResponse.java
│   │   │   │
│   │   │   ├── security/                      # 🔹 Security Components
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT validation
│   │   │   │   ├── JwtTokenProvider.java         # JWT generation
│   │   │   │   ├── OAuth2SuccessHandler.java     # OAuth callback
│   │   │   │   └── CustomUserDetailsService.java # User loading
│   │   │   │
│   │   │   ├── exception/                     # 🔹 Exception Handling
│   │   │   │   ├── GlobalExceptionHandler.java   # Global error handler
│   │   │   │   ├── CustomExceptions.java         # Custom exceptions
│   │   │   │   └── ErrorResponse.java            # Error DTOs
│   │   │   │
│   │   │   └── util/                          # 🔹 Utilities
│   │   │       ├── EmailParser.java           # Parse email content
│   │   │       ├── DateUtils.java             # Date formatting
│   │   │       └── ValidationUtils.java       # Custom validators
│   │   │
│   │   └── resources/
│   │       ├── application.yml                # Main configuration
│   │       ├── application-dev.yml            # Dev environment
│   │       └── application-prod.yml           # Production settings
│   │
│   └── test/                                  # Unit & Integration tests
│       └── java/com/hcmus/awad/
│           ├── controller/
│           ├── service/
│           └── integration/
│
└── target/                                    # Build output
    └── awad-email-0.0.1-SNAPSHOT.jar
```

---

## 📁 Chi Tiết Từng Folder (Folder-by-Folder Breakdown)

### 1️⃣ **config/ - Configuration Layer**

**Mục đích**: Cấu hình toàn bộ application behavior

#### **SecurityConfig.java** - Security Core Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    SecurityFilterChain filterChain(HttpSecurity http)
    JwtAuthenticationFilter jwtAuthFilter()
    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService()
    AuthenticationManager authenticationManager()
}
```

**Trách nhiệm và Kiến trúc Bảo mật**:

Đây là trái tim bảo mật của toàn bộ Backend service - nơi define tất cả security rules, authentication flows, và authorization policies. Mọi request đi qua application đều được xử lý bởi config này.

**Spring Security Architecture Deep Dive**:

**1. Security Filter Chain**:

- **Filter chain là gì**: Khi HTTP request vào server, nó đi qua chuỗi filters trước khi đến controller. Mỗi filter là checkpoint kiểm tra authentication, authorization, logging, etc. Spring Security inject 15+ filters vào chain.

- **Important filters trong chain**:

  - **CorsFilter** (đầu tiên): Kiểm tra CORS headers. Nếu request từ origin không allowed, reject ngay. Critical cho security vì prevent unauthorized cross-origin requests.
  - **CsrfFilter**: Kiểm tra CSRF tokens cho state-changing operations (POST, PUT, DELETE). We disable nó vì dùng JWT (stateless) thay vì sessions (stateful). JWT-based APIs không vulnerable với CSRF.
  - **JwtAuthenticationFilter** (custom): Extract JWT token từ `Authorization: Bearer <token>` header. Validate token signature, expiration, claims. Nếu valid, set Authentication object vào SecurityContext.
  - **OAuth2LoginAuthenticationFilter**: Handle Google OAuth2 login flow. Process callback từ Google với authorization code, exchange for access token.
  - **AuthorizationFilter**: Kiểm tra user có permissions để access endpoint không. So sánh required roles với user's roles.
  - **ExceptionTranslationFilter**: Catch authentication/authorization exceptions, convert thành proper HTTP responses (401 Unauthorized, 403 Forbidden).

- **Filter order matters**: CORS phải đầu tiên (trước authentication). JWT filter phải trước authorization filter. Sai thứ tự gây security holes.

**2. OAuth2 Integration Chi tiết**:

- **Tại sao OAuth2**: Users đã có Google account (có Gmail). Không cần tạo account mới, nhớ password. OAuth2 cho phép app access Gmail emails ON BEHALF OF user, không store password.

- **OAuth2 Flow Detail**:

  1. **Authorization Request**: User click "Login with Google" → Frontend redirect đến `/oauth2/authorization/google` → Backend redirect đến Google consent screen
  2. **User consent**: User chọn Google account, approve permissions (read emails, send emails, manage labels)
  3. **Authorization callback**: Google redirect lại Backend với authorization code: `/login/oauth2/code/google?code=abc123`
  4. **Token exchange**: Backend gửi code + client_secret đến Google để exchange for:
     - **Access token** (valid 1 hour): Dùng để call Gmail API
     - **Refresh token** (valid forever until revoked): Dùng để lấy access token mới khi expired
     - **ID token** (JWT): Chứa user info (email, name, profile pic)
  5. **Create session**: Backend generate JWT token (OUR token, không phải Google's), store user info + Google tokens trong database, return JWT cho Frontend
  6. **Subsequent requests**: Frontend send JWT trong header. Backend validate JWT, extract user ID, lấy Google access token từ DB, call Gmail API.

- **Token refresh flow**: Khi Google access token expires (sau 1 hour), Backend tự động dùng refresh token để lấy access token mới. User không aware, không cần login lại. Refresh token có thể expire nếu user revoke access hay không dùng app 6 months - lúc đó force re-login.

**3. JWT Authentication Strategy**:

- **Tại sao JWT thay vì Sessions**: Traditional sessions store state server-side (Redis, database). Khó scale horizontally - cần sticky sessions hay shared session storage. JWT stateless - server không store anything, chỉ verify signature. Easy scale to 100s servers.

- **JWT Structure**:

  ```
  Header.Payload.Signature
  {"alg":"HS256"}.{"userId":"123","email":"user@gmail.com","exp":1234567890}.HMACSHA256(...)
  ```

  - **Header**: Algorithm (HS256 = HMAC with SHA-256)
  - **Payload**: Claims (user ID, email, roles, expiration time)
  - **Signature**: HMAC(header + payload, secret_key). Prevent tampering - nếu ai đó change payload, signature không match.

- **JWT Validation Process**:

  1. Extract token từ `Authorization: Bearer <token>` header
  2. Decode Base64 để lấy header + payload
  3. Verify signature với secret key. Nếu không match → token tampered → reject 401
  4. Kiểm tra expiration (`exp` claim). Nếu expired → reject 401 với error "Token expired"
  5. Extract user info từ payload, load user details từ database (roles, permissions)
  6. Create Spring Security Authentication object, set vào SecurityContext
  7. Request proceed đến controller với authenticated user

- **Token expiration strategy**: Access token expire sau 24 hours (balance giữa security và UX). Nếu expire, Frontend call `/api/auth/refresh` endpoint với refresh token để get new access token. Refresh token expire sau 30 days.

**4. CORS Configuration**:

- **CORS là gì**: Cross-Origin Resource Sharing. Frontend chạy `localhost:3000`, Backend chạy `localhost:8080` - different origins. Browser block requests by default (security). Phải configure CORS để allow.

- **Allowed origins**: Development: `http://localhost:3000`. Production: `https://yourdomain.com`. Không dùng `*` (allow all) - security risk. Specific domains only.

- **Allowed methods**: GET, POST, PUT, DELETE, OPTIONS. OPTIONS là preflight request - browser gửi trước actual request để check permissions.

- **Allowed headers**: `Authorization` (for JWT), `Content-Type`, `Accept`. Custom headers phải explicitly allowed.

- **Credentials**: `allowCredentials(true)` cho phép cookies/auth headers. Required cho authenticated requests.

**5. Authorization Rules**:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()           // Login/register public
    .requestMatchers("/api/health").permitAll()            // Health check public
    .requestMatchers("/api/emails/**").authenticated()     // Emails require auth
    .requestMatchers("/api/admin/**").hasRole("ADMIN")    // Admin only
    .anyRequest().authenticated()                          // Default: require auth
)
```

- **permitAll()**: Endpoints không cần authentication. Login flow, health checks, public APIs.
- **authenticated()**: User phải login (có valid JWT). Most endpoints.
- **hasRole()**: User phải có specific role. Admin panels, sensitive operations.

**Tại sao Config này Critical**:

- **Single source of truth**: Tất cả security rules ở 1 chỗ. Không scatter across codebase. Audit security dễ dàng.
- **Prevent vulnerabilities**: Proper config prevent common attacks: unauthorized access, CSRF, XSS, token tampering, session hijacking.
- **Scalability**: Stateless JWT + filter-based architecture scale horizontally easily. Add 10 servers, no config changes needed.
- **Flexibility**: Easy update rules (add new public endpoints, change token expiration). Restart service, rules apply immediately.
- **Integration**: Spring Security integrates với Spring Boot actuator (metrics), audit logging, method-level security annotations.

#### **MongoConfig.java** - Database Configuration

```java
@Configuration
public class MongoConfig {
    MongoClient mongoClient()
    MongoTemplate mongoTemplate()
    MongoTransactionManager transactionManager()
    MongoCustomConversions customConversions()
}
```

**MongoDB Configuration Chi tiết**:

MongoDB là NoSQL database lựa chọn cho email application - flexible schema, high performance cho read-heavy workloads, excellent horizontal scaling.

**1. Tại sao chọn MongoDB thay vì PostgreSQL**:

- **Flexible schema**: Email data structure vary wildly. Email có thể có 0-10 attachments, 0-100 recipients, optional fields (cc, bcc, reply-to). Trong SQL cần nhiều tables + JOINs. Trong Mongo là 1 document chứa tất cả (embedded documents).
- **JSON-native**: Gmail API trả JSON. Store trực tiếp vào Mongo mà không cần transform. Với SQL phải map JSON fields → table columns (tedious).

- **Performance cho reads**: Email apps là read-heavy (1000 reads : 1 write). Mongo có excellent read performance với indexes. Query 10000 emails với filters (label, date range, starred) take <50ms với proper indexes.

- **Horizontal scaling**: Khi data grow đến millions emails, Mongo sharding distribute data across servers easily. SQL sharding cực phức tạp (foreign keys, transactions across shards).

**2. Connection Pooling Configuration**:

- **Tại sao cần pooling**: Mỗi database connection tốn resource (memory, network socket). Tạo connection cho mỗi request chậm (100-200ms TCP handshake). Pooling maintain sẵn 10-50 connections reusable.

- **Optimal pool settings**:

  - **minPoolSize = 10**: Luôn có 10 connections ready. Avoid cold starts.
  - **maxPoolSize = 50**: Maximum 50 concurrent connections. Prevent overwhelming database.
  - **maxIdleTime = 60000ms** (1 minute): Close idle connections để free resources.
  - **maxWaitTime = 30000ms**: Nếu pool full, wait max 30s for available connection, else throw exception.

- **Connection lifecycle**: Request arrive → Get connection from pool → Execute query → Return connection to pool. Connection reused cho nhiều requests. Giảm overhead dramatically.

**3. Indexing Strategy**:

- **Tại sao indexes critical**: Without indexes, query scan toàn bộ collection (O(n)). Với 100k emails = 100k document checks. Với index, query take O(log n) = ~17 checks. Difference giữa 2 seconds vs 20ms.

- **Index definitions cho emails**:

  ```java
  @Indexed(name = "user_id_idx")
  private String userId;  // Filter emails by user - most common query

  @Indexed(name = "label_idx")
  private List<String> labels;  // Filter by INBOX, SENT, STARRED

  @CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': -1}")
  // Common query: Get user's emails sorted by date DESC

  @TextIndex
  private String subject;  // Full-text search on subject
  @TextIndex
  private String snippet;  // Full-text search on email preview
  ```

- **Index trade-offs**: Indexes speed up reads but slow down writes (must update index on every insert/update). Email apps read-heavy, so indexes worth it. Rule of thumb: Index fields used trong 80%+ queries.

**4. Transaction Support**:

- **Khi nào dùng transactions**: Operations cần ACID guarantees. Ví dụ: Move email to archive (update email document + update folder counts). Nếu operation 1 success nhưng operation 2 fails, data inconsistent. Transaction ensures both success hay both rollback.

- **MongoDB transactions** (replica set required): Wrap multiple operations trong transaction. Có thể read + write nhiều documents, nhiều collections. All-or-nothing semantics.

- **Performance impact**: Transactions slower vì phải hold locks, coordinate với replica set. Dùng sparingly - chỉ cho critical operations. Normal operations (fetch emails, mark as read) không cần transactions.

**5. Custom Type Conversions**:

- **Java types ↔ MongoDB types**: Java Date/LocalDateTime → MongoDB ISODate. Java Enum → String. Custom objects → embedded documents.

- **Email-specific converters**:
  - **AttachmentConverter**: Java Attachment object → MongoDB subdocument {fileName, fileSize, mimeType, storageUrl}
  - **LabelConverter**: Java Label enum → String. Một số labels custom (user-created), store as-is.
  - **EmailAddressConverter**: Parse "John Doe <john@example.com>" → {name: "John Doe", email: "john@example.com"}

**6. Data Modeling Best Practices**:

- **Embedded vs Referenced**: Email attachments embedded trong email document (query 1 lần lấy tất cả). User info referenced (user ID only) vì user data change rarely, avoid duplication.

- **Document size limits**: MongoDB document max 16MB. Email với large attachments không store attachment content trong document - store URL đến S3/cloud storage. Document chỉ chứa metadata.

- **Denormalization**: Store email snippet (first 200 chars) trong email document for quick preview. Không cần load full content để show list view. Trade-off: Duplicate data for performance.

**Lợi ích Cụ thể**:

- **Developer productivity**: Spring Data MongoDB provides repository abstractions. Write `findByUserIdAndLabelsContaining(userId, label)` - auto generate query. Không cần viết raw queries.

- **Performance**: Proper indexes + connection pooling handle 1000s requests/second. Latency p95 < 100ms cho complex queries.

- **Reliability**: Replica sets provide high availability. Primary fails → automatic failover đến secondary trong 10-15 seconds. Zero data loss.

- **Scalability**: Sharding support millions users, billions emails. Linear scaling - add more shards để increase capacity.

#### **GmailConfig.java** - Gmail API Setup

```java
@Configuration
public class GmailConfig {
    @Bean
    public Gmail gmailClient(GoogleCredential credential) {
        return new Gmail.Builder(...)
            .setApplicationName("Email Client")
            .build();
    }
}
```

**Tại sao riêng biệt**:

- Gmail setup phức tạp (credentials, scopes, transport)
- Reusable across services
- Easy to mock trong tests

---

### 2️⃣ **controller/ - REST API Layer**

**Mục đích**: Định nghĩa HTTP endpoints và handle requests

#### **AuthController.java** - Authentication API

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Validate credentials
        // Generate JWT token
        // Return token + user info
    }

    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) {
        // Redirect to Google OAuth
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleCallback(@RequestParam String code) {
        // Exchange code for tokens
        // Create user if not exists
        // Return JWT
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest req) {
        // Validate refresh token
        // Generate new access token
    }
}
```

**Endpoints**:

- `POST /api/auth/login` - Traditional login
- `GET /api/auth/oauth2/google` - Initiate OAuth
- `GET /api/auth/oauth2/callback` - OAuth callback
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Logout & revoke tokens

**Tại sao quan trọng**:

- **Entry Point**: Frontend tương tác qua đây
- **Security**: First line of defense
- **User Experience**: Login flow smooth

#### **EmailController.java** - Email Operations

```java
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    @GetMapping
    public ResponseEntity<PagedResponse<Email>> getEmails(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) String label
    ) {
        // Get paginated emails from Gmail
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailDetailResponse> getEmail(@PathVariable String id) {
        // Get full email details
    }

    @PostMapping
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest request) {
        // Send email via Gmail API
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable String id) {
        // Move to trash
    }

    @PostMapping("/{id}/star")
    public ResponseEntity<Void> starEmail(@PathVariable String id) {
        // Add STARRED label
    }
}
```

**Chức năng**:

- CRUD operations cho emails
- Pagination & filtering
- Label management (star, archive, trash)
- Reply & forward

#### **KanbanController.java** - Kanban Board

```java
@RestController
@RequestMapping("/api/kanban")
public class KanbanController {

    @GetMapping("/boards")
    public ResponseEntity<List<KanbanBoard>> getBoards() {
        // Get all boards for user
    }

    @PostMapping("/boards")
    public ResponseEntity<KanbanBoard> createBoard(@RequestBody BoardRequest req) {
        // Create new board with columns
    }

    @PostMapping("/boards/{boardId}/emails/{emailId}")
    public ResponseEntity<Void> addEmailToBoard(
        @PathVariable String boardId,
        @PathVariable String emailId,
        @RequestParam String columnId
    ) {
        // Add email to specific column
    }

    @PutMapping("/boards/{boardId}/emails/{emailId}/move")
    public ResponseEntity<Void> moveEmail(
        @PathVariable String boardId,
        @PathVariable String emailId,
        @RequestBody MoveRequest request
    ) {
        // Move email between columns
        // Update position
    }
}
```

**Tại sao có Kanban**:

- **Productivity**: Organize emails như tasks
- **Workflow**: Drag-drop email management
- **Visual**: Kanban board interface
- **Unique Feature**: Standout từ competitors

#### **SearchController.java** - Search & Filters

```java
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @PostMapping("/semantic")
    public ResponseEntity<SearchResponse> semanticSearch(
        @RequestBody SemanticSearchRequest request
    ) {
        // Call AI Service for semantic search
        // Return ranked results
    }

    @GetMapping
    public ResponseEntity<List<Email>> search(
        @RequestParam String query,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to
    ) {
        // Traditional keyword search
        // Filter by parameters
    }
}
```

**Lợi ích**:

- Semantic search: Tìm theo meaning, không chỉ keywords
- Advanced filters: date range, sender, labels
- Fast: Index-based search

---

### 3️⃣ **service/ - Business Logic Layer**

**Mục đích**: Core business logic, không phụ thuộc vào HTTP layer

#### **AuthService.java** - Authentication Logic

```java
@Service
public class AuthService {

    public AuthResponse authenticateUser(LoginRequest request) {
        // Validate credentials
        // Check user exists
        // Generate JWT tokens
        // Return auth response
    }

    public AuthResponse handleOAuth2Login(String authCode) {
        // Exchange code for Google tokens
        // Get user info from Google
        // Create/update user in database
        // Generate our JWT tokens
        // Store Google refresh token securely
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        // Validate refresh token
        // Generate new access token
        // Return new auth response
    }
}
```

**Tại sao quan trọng**:

- **Security**: Xử lý authentication logic an toàn
- **Token Management**: JWT generation & validation
- **OAuth Flow**: Handle Google OAuth complexity

#### **GmailService.java** - Gmail API Integration

```java
@Service
public class GmailService {

    public List<Email> fetchEmails(String userId, int page, int size) {
        // Get Gmail client with user credentials
        // Fetch messages with pagination
        // Parse Gmail format to our Email model
        // Handle rate limits & errors
    }

    public EmailDetail getEmailById(String userId, String emailId) {
        // Fetch full email details
        // Parse HTML body
        // Extract attachments
    }

    public void sendEmail(String userId, EmailRequest request) {
        // Build MIME message
        // Send via Gmail API
        // Handle threading
    }

    public void modifyLabels(String userId, String emailId, List<String> addLabels, List<String> removeLabels) {
        // Update Gmail labels
        // Sync with local database
    }
}
```

**Phức Tạp**:

- Gmail API có nhiều quirks (pagination, rate limits)
- MIME message encoding
- Token refresh handling
- Batch operations optimization

**Giải Quyết**:

- Abstraction: Hide Gmail complexity
- Retry logic: Handle transient failures
- Caching: Reduce API calls

#### **EmailService.java** - Core Email Business Logic

```java
@Service
public class EmailService {
    PagedResponse<Email> getUserEmails(String userId, EmailFilter filter)
    Email getEmailById(String userId, String emailId)
    Email markAsRead(String userId, String emailId)
    void deleteEmail(String userId, String emailId)
    EmailSummary getEmailWithSummary(String userId, String emailId)
}
```

**Service Orchestration - Trái Tim Business Logic**:

EmailService là conductor orchestrate nhiều data sources và external services để provide unified email experience cho users.

**Architecture Pattern - Service Composition**:

**1. Multi-Source Data Aggregation**:

**Thách thức**: Email data scattered across 3 places:

- **Gmail API**: Source of truth cho email content, headers, attachments (live data)
- **MongoDB**: Local cache cho performance + kanban metadata (our enrichment)
- **AI Service**: Summaries, sentiment analysis, embeddings (AI-generated)

**Orchestration strategy**:

```
getUserEmails(userId, filter):
  1. Kiểm tra cache (MongoDB) trước
     - Nếu có và fresh (< 5 minutes old) → return immediately
     - Performance win: 20ms MongoDB query vs 500ms Gmail API call

  2. Nếu cache miss hay stale:
     - Fetch từ Gmail API (slow but authoritative)
     - Enrich với local metadata (kanban column, custom labels, notes)
     - Store trong MongoDB for future requests
     - Return combined data

  3. Apply business filters:
     - Filter by label (INBOX, SENT, STARRED)
     - Date range filtering
     - Starred/unread status
     - Kanban column assignment
```

**Trade-offs đã consider**:

- **Consistency vs Performance**: Cache có thể slightly stale (eventual consistency), but queries 25x faster. Acceptable cho email app - users không expect real-time updates mili-second level.
- **Storage cost**: Caching emails trong MongoDB duplicates data, but disk cheap. Worth it cho UX improvement.

**2. Gmail API Integration Complexity**:

**Vấn đề với Gmail API**:

- **Rate limits**: 250 requests/second per user. Exceed = 429 errors. Large inbox (5000 emails) = many requests.
- **Pagination**: Gmail returns 100 emails/request max. Phải loop multiple times for full inbox.
- **Token expiration**: Access tokens valid 1 hour. Phải refresh automatically mid-operation.
- **Transient failures**: Network timeouts, temporary 500 errors from Google. Phải retry intelligently.

**EmailService handles**:

```java
// Pseudo-code showing complexity hidden
private List<Email> fetchFromGmail(String userId) {
    GoogleCredential credential = tokenService.getCredential(userId);

    // Auto-refresh token if expired
    if (credential.getExpiresInSeconds() < 300) {  // < 5 min left
        tokenService.refreshToken(userId);
    }

    List<Email> allEmails = new ArrayList<>();
    String pageToken = null;

    do {
        try {
            // Fetch page of emails
            ListMessagesResponse response = gmailService
                .users().messages().list("me")
                .setPageToken(pageToken)
                .setMaxResults(100L)
                .execute();

            // Fetch full content for each email (another API call per email!)
            for (Message msg : response.getMessages()) {
                Email email = fetchEmailDetails(msg.getId());
                allEmails.add(email);
            }

            pageToken = response.getNextPageToken();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 429) {  // Rate limit
                Thread.sleep(1000);  // Wait 1 second
                continue;  // Retry
            }
            throw e;
        }
    } while (pageToken != null);

    return allEmails;
}
```

Service layer abstracts tất cả complexity này. Controllers chỉ gọi `emailService.getUserEmails()` - simple.

**3. Cache Invalidation Strategy**:

**Khi nào invalidate cache**:

- **Write operations**: User marks email as read, stars it, moves to folder → update Gmail + invalidate cache → next read gets fresh data
- **Time-based**: Cache entries có TTL (5 minutes). After expiry, auto-refetch.
- **Webhook-based** (future): Gmail push notifications khi emails arrive → proactive cache update

**Consistency guarantee**: Eventual consistency acceptable. User marks read → maybe sees "unread" indicator for 1-2 seconds until cache updates. Not critical for email app.

**4. AI Service Integration**:

```java
public EmailWithSummary getEmailWithSummary(String userId, String emailId) {
    // Step 1: Get email content
    Email email = getEmailById(userId, emailId);

    // Step 2: Check if summary already cached
    EmailSummary cached = summaryCache.get(emailId);
    if (cached != null) {
        return new EmailWithSummary(email, cached);
    }

    // Step 3: Call AI Service (slow - 2-3 seconds)
    try {
        EmailSummary summary = aiServiceClient.summarizeEmail(
            emailId,
            email.getBody()
        );

        // Step 4: Cache result (never recompute same email)
        summaryCache.put(emailId, summary);

        return new EmailWithSummary(email, summary);

    } catch (AIServiceException e) {
        // Fallback: return email without summary
        log.warn("AI service unavailable: {}", e.getMessage());
        return new EmailWithSummary(email, null);
    }
}
```

**Resilience patterns**:

- **Timeout**: AI service calls timeout after 10 seconds. Don't wait forever.
- **Circuit breaker**: After 5 consecutive failures, stop calling AI service for 60 seconds. Prevent cascade failures.
- **Graceful degradation**: App works without AI features. Summaries nice-to-have, not critical.

**5. Transaction Boundaries**:

**Khi cần transactions**: Operations affecting multiple data sources

Example: Delete email

```java
@Transactional
public void deleteEmail(String userId, String emailId) {
    // 1. Delete from Gmail (external API - can't rollback!)
    gmailService.deleteMessage(userId, emailId);

    // 2. Delete from MongoDB cache
    emailRepository.deleteById(emailId);

    // 3. Remove from kanban board if present
    kanbanService.removeEmailFromBoard(userId, emailId);

    // 4. Delete cached summary
    summaryCache.evict(emailId);
}
```

**Problem**: Gmail delete irreversible. Nếu MongoDB delete fails, data inconsistent.

**Solution**: Compensating transactions

```java
try {
    gmailService.deleteMessage(userId, emailId);
} catch (Exception e) {
    // Gmail delete failed - nothing to compensate
    throw e;
}

try {
    emailRepository.deleteById(emailId);
    kanbanService.removeEmailFromBoard(userId, emailId);
} catch (Exception e) {
    // MongoDB failed but Gmail already deleted
    // Schedule background job to clean up cache later
    cleanupQueue.add(new CacheCleanupTask(emailId));
    // Still return success to user (Gmail delete worked)
}
```

**Key Design Principles**:

- **Single Responsibility**: EmailService manages emails. Không handle authentication, kanban logic, AI processing - delegate to specialized services.
- **Dependency Injection**: All dependencies injected via constructor. Easy to mock trong tests, swap implementations.
- **Error handling**: Checked exceptions for expected errors (email not found), unchecked for bugs. Meaningful error messages.
- **Logging**: Log all operations với context (userId, emailId, operation). Debugging production issues easy.
- **Performance**: Batch operations where possible. Fetch 100 emails in 2 API calls, not 100 calls.

#### **AIServiceClient.java** - AI Service Integration

```java
@Service
public class AIServiceClient {
    private final RestTemplate restTemplate;
    private final String aiServiceUrl = "http://ai:8000";

    public EmailSummary summarizeEmail(String emailId, String content) {
        // Build request
        // Call AI Service REST API
        // Handle errors & timeouts
        // Parse response
    }

    public List<Email> semanticSearch(String userId, String query) {
        // Get all user emails
        // Send to AI Service for semantic ranking
        // Return ranked results
    }
}
```

**Tại sao cần Client**:

- **Abstraction**: Hide HTTP details từ business logic
- **Retry**: Automatic retry on failures
- **Circuit Breaker**: Prevent cascade failures
- **Timeout**: Don't wait forever

#### **TokenService.java** - Token Management

```java
@Service
public class TokenService {

    public void refreshGoogleTokenIfNeeded(String userId) {
        // Check token expiry
        // Refresh if needed
        // Update stored token
    }

    public GoogleCredential getCredentialForUser(String userId) {
        // Load tokens from database
        // Build credential object
        // Auto-refresh setup
    }
}
```

**Critical**:

- Gmail tokens expire sau 1 giờ
- Phải refresh tự động
- Không để user bị logged out

---

### 4️⃣ **repository/ - Data Access Layer**

**Mục đích**: Interface với MongoDB, abstract database operations

#### **UserRepository.java**

```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
}
```

**Lợi ích**:

- Spring Data auto-implements methods
- Type-safe queries
- No boilerplate SQL/BSON

#### **EmailRepository.java**

```java
@Repository
public interface EmailRepository extends MongoRepository<Email, String> {
    List<Email> findByUserIdAndLabelContaining(String userId, String label);
    Page<Email> findByUserIdAndStarredTrue(String userId, Pageable pageable);

    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
    List<Email> findByDateRange(String userId, Date start, Date end);
}
```

**Tại sao lưu emails**:

- **Performance**: Cache thay vì fetch Gmail mỗi lần
- **Metadata**: Store kanban assignments, custom labels
- **Offline**: Data available khi Gmail API down

#### **KanbanBoardRepository.java**

```java
@Repository
public interface KanbanBoardRepository extends MongoRepository<KanbanBoard, String> {
    List<KanbanBoard> findByUserId(String userId);
    Optional<KanbanBoard> findByUserIdAndId(String userId, String boardId);
}
```

**Data Structure**:

```json
{
  "_id": "board123",
  "userId": "user456",
  "name": "Project X Emails",
  "columns": [
    {
      "id": "col1",
      "name": "To Do",
      "emailIds": ["email1", "email2"]
    },
    {
      "id": "col2",
      "name": "In Progress",
      "emailIds": ["email3"]
    }
  ]
}
```

---

### 5️⃣ **model/ - Domain Models**

**Mục đích**: Represent business entities

#### **User.java**

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String email;
    private String name;
    private String googleId;

    @Field("google_access_token")
    private String googleAccessToken;

    @Field("google_refresh_token")
    private String googleRefreshToken;

    @Field("token_expiry")
    private LocalDateTime tokenExpiry;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Annotations**:

- `@Document`: MongoDB collection mapping
- `@Id`: Primary key
- `@Field`: Custom field naming
- `@Indexed`: Create database index

#### **Email.java**

```java
@Document(collection = "emails")
public class Email {
    @Id
    private String id;  // Gmail message ID

    private String userId;
    private String subject;
    private String from;
    private List<String> to;
    private String snippet;
    private String body;
    private List<String> labels;
    private boolean starred;
    private boolean read;
    private LocalDateTime date;

    // Kanban metadata
    private String kanbanBoardId;
    private String kanbanColumnId;

    // AI-generated
    private String summary;
    private LocalDateTime summarizedAt;
}
```

**Design Decisions**:

- Store Gmail ID as primary key
- Cache email content locally
- Add custom fields (kanban, summary)

---

### 6️⃣ **dto/ - Data Transfer Objects**

**Mục đích**: Define API contracts, không expose internal models

#### **Request DTOs**

```java
public class EmailRequest {
    @NotBlank
    private String to;

    @NotBlank
    private String subject;

    @NotNull
    private String body;

    private String replyToId;  // For threading

    // Validation annotations
}
```

**Tại sao cần DTO**:

- **Security**: Không expose sensitive fields
- **Validation**: Bean validation annotations
- **API Contract**: Stable external interface
- **Versioning**: Dễ version API

#### **Response DTOs**

```java
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDTO user;

    // No sensitive fields like password, internal IDs
}
```

---

### 7️⃣ **security/ - Security Components**

#### **JwtAuthenticationFilter.java**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // Extract JWT from Authorization header
        // Validate token signature
        // Parse user info from token
        // Set SecurityContext
    }
}
```

**Workflow**:

```
Request → Extract JWT → Validate → Load User → Set Context → Continue
```

#### **JwtTokenProvider.java**

```java
@Component
public class JwtTokenProvider {
    private final String SECRET_KEY;
    private final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15 min
    private final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; // 7 days

    public String generateAccessToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId())
            .claim("email", user.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
            .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
            .compact();
    }

    public boolean validateToken(String token) {
        // Parse & validate
    }
}
```

**Security Practices**:

- Short-lived access tokens (15 min)
- Long-lived refresh tokens (7 days)
- HS512 signing algorithm
- Secret key từ environment variables

---

### 8️⃣ **exception/ - Exception Handling**

#### **GlobalExceptionHandler.java**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            404, ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(GmailApiException.class)
    public ResponseEntity<ErrorResponse> handleGmailError(GmailApiException ex) {
        // Log error
        // Return user-friendly message
        return ResponseEntity.status(503).body(...);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        // Log full stack trace
        // Return generic error (don't expose internals)
        return ResponseEntity.status(500).body(
            new ErrorResponse(500, "Internal server error", ...)
        );
    }
}
```

**Tại sao cần**:

- **Consistent Errors**: Tất cả errors có format giống nhau
- **Security**: Không expose stack traces ra ngoài
- **User Experience**: Friendly error messages
- **Monitoring**: Centralized logging

---

## 🔄 Luồng Xử Lý Hoàn Chỉnh (Complete Request Flow)

### **Example: User Sends Email**

```
1. Frontend
   POST /api/emails
   Headers: { Authorization: "Bearer <jwt>" }
   Body: { to: "user@example.com", subject: "...", body: "..." }

2. JwtAuthenticationFilter
   ├─ Extract JWT from header
   ├─ Validate token signature
   ├─ Load user from token claims
   └─ Set SecurityContext (Spring knows who is logged in)

3. EmailController.sendEmail()
   ├─ @Valid validates request body
   ├─ Get current user from SecurityContext
   └─ Call emailService.sendEmail()

4. EmailService.sendEmail()
   ├─ Build email object
   ├─ Validate business rules (rate limits, spam check)
   ├─ Call gmailService.sendEmail()
   └─ Save to local database (optional cache)

5. GmailService.sendEmail()
   ├─ Get user's Gmail credentials (tokenService)
   ├─ Build MIME message
   ├─ Call Gmail API
   ├─ Handle threading (reply-to)
   └─ Return sent message ID

6. TokenService (auto-called if needed)
   ├─ Check if Google token expired
   ├─ Refresh token if needed
   └─ Update stored credentials

7. Response
   └─ Return EmailResponse with message ID

8. Frontend receives confirmation
```

**Tổng thời gian**: ~500ms - 2s (phụ thuộc Gmail API)

---

## 🔗 Tích Hợp Với Các Services (Service Integration)

### **Backend ↔ AI Service**

```java
// Backend calls AI Service
@Service
public class EmailService {
    @Autowired
    private AIServiceClient aiClient;

    public EmailSummary getSummary(String emailId) {
        Email email = emailRepository.findById(emailId);
        return aiClient.summarizeEmail(email.getId(), email.getBody());
    }
}
```

### **Backend ↔ Gmail API**

```java
// Backend calls Gmail API
@Service
public class GmailService {
    public List<Message> fetchMessages(String userId) {
        Gmail gmail = getGmailClient(userId);
        ListMessagesResponse response = gmail.users()
            .messages()
            .list(userId)
            .setMaxResults(50L)
            .execute();
        return response.getMessages();
    }
}
```

### **Backend ↔ MongoDB**

```java
// Backend reads/writes MongoDB
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User createUser(String email, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        return userRepository.save(user);
    }
}
```

---

## 🐳 Deployment Architecture

### **Docker Setup**

```yaml
# docker-compose.yml
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - MONGODB_URI=mongodb://mongo:27017/email-app
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - JWT_SECRET=${JWT_SECRET}
      - AI_SERVICE_URL=http://ai:8000
    depends_on:
      - mongo
      - ai
    networks:
      - email-network

  mongo:
    image: mongo:7.0
    volumes:
      - mongo-data:/data/db
    networks:
      - email-network
```

---

## 📊 Performance & Monitoring

### **Key Metrics**

1. **API Performance**

   - Average response time: <500ms
   - P99 latency: <2s
   - Throughput: 1000 req/min

2. **Gmail API Usage**

   - Rate limit: 250 units/user/second
   - Daily quota: 1 billion units/day
   - Caching: 70% hit rate

3. **Database Performance**
   - MongoDB queries: <50ms
   - Connection pool: 10-50 connections
   - Index optimization

### **Monitoring Tools**

- **Spring Boot Actuator**: Health checks, metrics
- **Prometheus**: Metrics collection
- **Grafana**: Dashboards
- **ELK Stack**: Log aggregation

---

## 🔐 Security Best Practices

### **1. Authentication & Authorization**

- JWT with short expiry (15 min)
- HttpOnly cookies for refresh tokens
- Role-based access control (RBAC)

### **2. API Security**

- Rate limiting per user
- Request validation
- CORS configuration
- HTTPS only in production

### **3. Data Protection**

- Encrypt tokens in database
- Never log sensitive data
- Secure credential storage
- Regular security audits

### **4. Gmail Token Management**

- Store encrypted in MongoDB
- Auto-refresh mechanism
- Revoke on logout
- Scope limitation (minimum required)

---

## 🚀 Future Enhancements

### **Planned Features**

1. **Performance**

   - Redis caching layer
   - Database query optimization
   - Async processing for heavy tasks

2. **Scalability**

   - Horizontal scaling with load balancer
   - Database sharding
   - Message queue (RabbitMQ/Kafka)

3. **Features**

   - Email templates
   - Scheduled sending
   - Email rules & filters
   - Calendar integration

4. **Monitoring**
   - Real-time alerts
   - Performance dashboards
   - Error tracking (Sentry)
   - User analytics

---

---

## 🧪 Testing Strategy (Chiến Lược Testing Chi Tiết)

### **1. Unit Testing với JUnit 5 & Mockito**

#### **Testing Controllers**

```java
@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @Test
    @WithMockUser(username = "user123")
    void testGetEmails_Success() throws Exception {
        // Arrange
        List<Email> mockEmails = Arrays.asList(
            new Email("1", "Test Subject 1", "sender1@test.com"),
            new Email("2", "Test Subject 2", "sender2@test.com")
        );
        PagedResponse<Email> pagedResponse = new PagedResponse<>(
            mockEmails, 2, 0, 50, false
        );

        when(emailService.getUserEmails(anyString(), any(EmailFilter.class)))
            .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/emails")
                .param("page", "0")
                .param("size", "50")
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].subject").value("Test Subject 1"))
            .andExpect(jsonPath("$.total").value(2));

        verify(emailService, times(1)).getUserEmails(anyString(), any());
    }

    @Test
    void testGetEmails_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/emails"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user123")
    void testSendEmail_ValidationError() throws Exception {
        String invalidRequest = """
            {
                "to": "",
                "subject": "",
                "body": ""
            }
            """;

        mockMvc.perform(post("/api/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest)
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @WithMockUser(username = "user123")
    void testDeleteEmail_Success() throws Exception {
        String emailId = "test123";

        doNothing().when(emailService).deleteEmail(anyString(), eq(emailId));

        mockMvc.perform(delete("/api/emails/{id}", emailId)
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isNoContent());

        verify(emailService).deleteEmail(anyString(), eq(emailId));
    }
}
```

#### **Testing Services**

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private GmailService gmailService;

    @Mock
    private AIServiceClient aiServiceClient;

    @InjectMocks
    private EmailService emailService;

    @Test
    void testGetUserEmails_Success() {
        // Arrange
        String userId = "user123";
        EmailFilter filter = new EmailFilter();
        List<Email> mockEmails = createMockEmails(10);

        when(gmailService.fetchEmails(userId, 0, 50))
            .thenReturn(mockEmails);
        when(emailRepository.findByUserId(userId))
            .thenReturn(mockEmails);

        // Act
        PagedResponse<Email> result = emailService.getUserEmails(userId, filter);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getItems().size());
        verify(gmailService).fetchEmails(userId, 0, 50);
    }

    @Test
    void testSendEmail_Success() {
        // Arrange
        String userId = "user123";
        EmailRequest request = new EmailRequest(
            "recipient@test.com",
            "Test Subject",
            "Test Body"
        );

        when(gmailService.sendEmail(eq(userId), any(EmailRequest.class)))
            .thenReturn("sent_message_id");

        // Act
        EmailResponse response = emailService.sendEmail(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals("sent_message_id", response.getId());
        verify(gmailService).sendEmail(userId, request);
    }

    @Test
    void testGetEmailSummary_WithCache() {
        // Arrange
        String userId = "user123";
        String emailId = "email456";
        Email email = new Email(emailId, "Subject", "Long content...");
        email.setSummary("Cached summary");

        when(emailRepository.findById(emailId))
            .thenReturn(Optional.of(email));

        // Act
        EmailSummary summary = emailService.getEmailSummary(userId, emailId);

        // Assert
        assertEquals("Cached summary", summary.getSummary());
        verify(aiServiceClient, never()).summarizeEmail(anyString(), anyString());
    }

    @Test
    void testGetEmailSummary_WithoutCache() {
        // Arrange
        String userId = "user123";
        String emailId = "email456";
        Email email = new Email(emailId, "Subject", "Long content...");
        EmailSummary mockSummary = new EmailSummary(
            "AI generated summary",
            Arrays.asList("Point 1", "Point 2"),
            "neutral"
        );

        when(emailRepository.findById(emailId))
            .thenReturn(Optional.of(email));
        when(aiServiceClient.summarizeEmail(emailId, email.getBody()))
            .thenReturn(mockSummary);

        // Act
        EmailSummary summary = emailService.getEmailSummary(userId, emailId);

        // Assert
        assertEquals("AI generated summary", summary.getSummary());
        verify(aiServiceClient).summarizeEmail(emailId, email.getBody());
        verify(emailRepository).save(any(Email.class));
    }

    @Test
    void testDeleteEmail_NotFound() {
        // Arrange
        String userId = "user123";
        String emailId = "nonexistent";

        when(emailRepository.findById(emailId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> emailService.deleteEmail(userId, emailId)
        );
    }

    private List<Email> createMockEmails(int count) {
        List<Email> emails = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            emails.add(new Email(
                "id" + i,
                "Subject " + i,
                "sender" + i + "@test.com"
            ));
        }
        return emails;
    }
}
```

#### **Testing Security**

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testAccessProtectedEndpoint_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/emails"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpoint_WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/emails")
                .header("Authorization", "Bearer invalid_token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessAdminEndpoint_AsUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAccessAdminEndpoint_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk());
    }
}
```

### **2. Integration Testing**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class EmailIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String authToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // Create test user and get auth token
        User testUser = createTestUser();
        authToken = generateAuthToken(testUser);

        // Clean database
        emailRepository.deleteAll();
    }

    @Test
    void testFullEmailFlow() {
        // 1. Send email
        EmailRequest sendRequest = new EmailRequest(
            "recipient@test.com",
            "Integration Test",
            "This is a test email"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<EmailRequest> request = new HttpEntity<>(sendRequest, headers);

        ResponseEntity<EmailResponse> sendResponse = restTemplate.postForEntity(
            baseUrl + "/api/emails",
            request,
            EmailResponse.class
        );

        assertEquals(HttpStatus.OK, sendResponse.getStatusCode());
        assertNotNull(sendResponse.getBody());
        String sentEmailId = sendResponse.getBody().getId();

        // 2. Get emails list
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<PagedResponse> getResponse = restTemplate.exchange(
            baseUrl + "/api/emails?page=0&size=50",
            HttpMethod.GET,
            getRequest,
            PagedResponse.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertTrue(getResponse.getBody().getTotal() > 0);

        // 3. Get specific email
        ResponseEntity<EmailDetailResponse> detailResponse = restTemplate.exchange(
            baseUrl + "/api/emails/" + sentEmailId,
            HttpMethod.GET,
            getRequest,
            EmailDetailResponse.class
        );

        assertEquals(HttpStatus.OK, detailResponse.getStatusCode());
        assertEquals("Integration Test", detailResponse.getBody().getSubject());

        // 4. Star email
        ResponseEntity<Void> starResponse = restTemplate.exchange(
            baseUrl + "/api/emails/" + sentEmailId + "/star",
            HttpMethod.POST,
            getRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, starResponse.getStatusCode());

        // 5. Delete email
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            baseUrl + "/api/emails/" + sentEmailId,
            HttpMethod.DELETE,
            getRequest,
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
    }

    @Test
    void testKanbanFlow() {
        // 1. Create kanban board
        BoardRequest createRequest = new BoardRequest(
            "My Project",
            Arrays.asList("To Do", "In Progress", "Done")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<BoardRequest> request = new HttpEntity<>(createRequest, headers);

        ResponseEntity<KanbanBoard> createResponse = restTemplate.postForEntity(
            baseUrl + "/api/kanban/boards",
            request,
            KanbanBoard.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        String boardId = createResponse.getBody().getId();

        // 2. Add email to board
        String emailId = "test_email_123";
        ResponseEntity<Void> addResponse = restTemplate.exchange(
            baseUrl + "/api/kanban/boards/" + boardId + "/emails/" + emailId + "?columnId=col1",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, addResponse.getStatusCode());

        // 3. Move email
        MoveRequest moveRequest = new MoveRequest("col2", 0);
        ResponseEntity<Void> moveResponse = restTemplate.exchange(
            baseUrl + "/api/kanban/boards/" + boardId + "/emails/" + emailId + "/move",
            HttpMethod.PUT,
            new HttpEntity<>(moveRequest, headers),
            Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, moveResponse.getStatusCode());
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setGoogleId("google_123");
        return userRepository.save(user);
    }

    private String generateAuthToken(User user) {
        // Generate JWT token for testing
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        return tokenProvider.generateAccessToken(user);
    }
}
```

### **3. Performance Testing**

```java
@SpringBootTest
class PerformanceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void testGetEmails_Performance() {
        String userId = "test_user";
        EmailFilter filter = new EmailFilter();

        // Warm up
        for (int i = 0; i < 10; i++) {
            emailService.getUserEmails(userId, filter);
        }

        // Measure
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            emailService.getUserEmails(userId, filter);
        }
        long endTime = System.currentTimeMillis();

        long avgTime = (endTime - startTime) / 100;

        // Assert average response time < 500ms
        assertTrue(avgTime < 500, "Average response time: " + avgTime + "ms");
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        int numThreads = 50;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        emailService.getUserEmails("user123", new EmailFilter());
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        int totalRequests = numThreads * requestsPerThread;
        System.out.println("Success: " + successCount.get() + "/" + totalRequests);
        System.out.println("Errors: " + errorCount.get());

        assertTrue(successCount.get() > totalRequests * 0.95,
            "Success rate should be > 95%");
    }
}
```

### **4. Test Coverage Goals**

```xml
<!-- pom.xml - JaCoCo configuration -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.75</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Coverage Targets**:

- **Controllers**: 90%+
- **Services**: 85%+
- **Repositories**: 80%+
- **Utilities**: 95%+
- **Overall**: 85%+

---

## 🚀 Advanced Implementation Patterns

### **1. Caching Strategy với Redis**

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        // Different TTL for different caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("emails",
            config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("emailDetails",
            config.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("summaries",
            config.entryTtl(Duration.ofDays(7)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

@Service
public class EmailService {

    @Cacheable(value = "emails", key = "#userId + ':' + #filter.hashCode()")
    public PagedResponse<Email> getUserEmails(String userId, EmailFilter filter) {
        // Expensive operation - fetch from Gmail
        return gmailService.fetchEmails(userId, filter);
    }

    @Cacheable(value = "emailDetails", key = "#emailId")
    public EmailDetail getEmailDetail(String userId, String emailId) {
        return gmailService.getEmailById(userId, emailId);
    }

    @CacheEvict(value = "emails", key = "#userId + '*'", allEntries = true)
    public void sendEmail(String userId, EmailRequest request) {
        gmailService.sendEmail(userId, request);
        // Cache invalidated automatically
    }

    @CachePut(value = "emailDetails", key = "#emailId")
    public EmailDetail updateEmail(String userId, String emailId, EmailUpdate update) {
        EmailDetail updated = gmailService.updateEmail(userId, emailId, update);
        return updated;
    }

    // Conditional caching
    @Cacheable(value = "summaries", key = "#emailId",
        condition = "#result.length() > 100")
    public String getEmailSummary(String emailId) {
        return aiServiceClient.summarizeEmail(emailId);
    }
}
```

### **2. Async Processing với @Async**

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}

@Service
public class EmailService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendBulkEmails(
        String userId,
        List<EmailRequest> requests
    ) {
        logger.info("Processing {} emails asynchronously", requests.size());

        for (EmailRequest request : requests) {
            try {
                gmailService.sendEmail(userId, request);
                Thread.sleep(100); // Rate limiting
            } catch (Exception e) {
                logger.error("Failed to send email: {}", e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<EmailSummary> generateSummaryAsync(
        String emailId,
        String content
    ) {
        EmailSummary summary = aiServiceClient.summarizeEmail(emailId, content);
        emailRepository.updateSummary(emailId, summary);
        return CompletableFuture.completedFuture(summary);
    }

    // Parallel processing
    public List<EmailSummary> generateBulkSummaries(List<String> emailIds) {
        List<CompletableFuture<EmailSummary>> futures = emailIds.stream()
            .map(id -> {
                Email email = emailRepository.findById(id).orElseThrow();
                return generateSummaryAsync(id, email.getBody());
            })
            .collect(Collectors.toList());

        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
}
```

### **3. Rate Limiting Implementation**

```java
@Component
public class RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public RateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String userId, int maxRequests, Duration window) {
        String key = RATE_LIMIT_PREFIX + userId;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == 1) {
            redisTemplate.expire(key, window);
        }

        return currentCount <= maxRequests;
    }

    public long getWaitTime(String userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
}

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        String userId = extractUserId(request);

        if (!rateLimiter.allowRequest(userId, MAX_REQUESTS, WINDOW)) {
            long waitTime = rateLimiter.getWaitTime(userId);
            response.setStatus(429);
            response.addHeader("Retry-After", String.valueOf(waitTime));
            response.getWriter().write(
                "{\"error\": \"Rate limit exceeded\", \"retryAfter\": " + waitTime + "}"
            );
            return false;
        }

        return true;
    }

    private String extractUserId(HttpServletRequest request) {
        // Extract from JWT token
        String token = request.getHeader("Authorization");
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
```

### **4. Circuit Breaker với Resilience4j**

```java
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(100)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(10)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .retryExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(ValidationException.class)
            .build();

        return RetryRegistry.of(config);
    }
}

@Service
public class AIServiceClient {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RestTemplate restTemplate;

    public AIServiceClient(
        CircuitBreakerRegistry cbRegistry,
        RetryRegistry retryRegistry
    ) {
        this.circuitBreaker = cbRegistry.circuitBreaker("ai-service");
        this.retry = retryRegistry.retry("ai-service");
        this.restTemplate = new RestTemplate();
    }

    public EmailSummary summarizeEmail(String emailId, String content) {
        return Decorators.ofSupplier(() -> callAIService(emailId, content))
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withFallback(
                Arrays.asList(CallNotPermittedException.class, IOException.class),
                e -> generateFallbackSummary(content)
            )
            .get();
    }

    private EmailSummary callAIService(String emailId, String content) {
        SummarizeRequest request = new SummarizeRequest(emailId, content);
        ResponseEntity<EmailSummary> response = restTemplate.postForEntity(
            aiServiceUrl + "/api/email/summarize",
            request,
            EmailSummary.class
        );
        return response.getBody();
    }

    private EmailSummary generateFallbackSummary(String content) {
        // Simple fallback when AI service is down
        String snippet = content.length() > 200
            ? content.substring(0, 200) + "..."
            : content;

        return new EmailSummary(
            snippet,
            Collections.emptyList(),
            "unknown"
        );
    }
}
```

### **5. Event-Driven Architecture với Spring Events**

```java
// Event definitions
public class EmailSentEvent extends ApplicationEvent {
    private final String emailId;
    private final String userId;
    private final String recipient;

    public EmailSentEvent(Object source, String emailId, String userId, String recipient) {
        super(source);
        this.emailId = emailId;
        this.userId = userId;
        this.recipient = recipient;
    }

    // Getters
}

public class EmailReceivedEvent extends ApplicationEvent {
    private final String emailId;
    private final String userId;

    public EmailReceivedEvent(Object source, String emailId, String userId) {
        super(source);
        this.emailId = emailId;
        this.userId = userId;
    }

    // Getters
}

// Publisher
@Service
public class EmailService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public EmailResponse sendEmail(String userId, EmailRequest request) {
        // Send email via Gmail
        String emailId = gmailService.sendEmail(userId, request);

        // Publish event
        eventPublisher.publishEvent(new EmailSentEvent(
            this, emailId, userId, request.getTo()
        ));

        return new EmailResponse(emailId);
    }
}

// Listeners
@Component
public class EmailEventListener {

    @Async
    @EventListener
    public void handleEmailSent(EmailSentEvent event) {
        logger.info("Email sent: {} to {}", event.getEmailId(), event.getRecipient());

        // Update analytics
        analyticsService.trackEmailSent(event.getUserId());

        // Send notification
        notificationService.notifyEmailSent(event.getUserId(), event.getEmailId());
    }

    @Async
    @EventListener
    @Transactional
    public void handleEmailReceived(EmailReceivedEvent event) {
        logger.info("Email received: {}", event.getEmailId());

        // Auto-categorize
        categorizationService.categorizeEmail(event.getEmailId());

        // Generate summary if important
        Email email = emailRepository.findById(event.getEmailId()).orElseThrow();
        if (email.isImportant()) {
            aiServiceClient.summarizeEmail(event.getEmailId(), email.getBody());
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleEmailReceivedPriority(EmailReceivedEvent event) {
        // Process high-priority emails first
        Email email = emailRepository.findById(event.getEmailId()).orElseThrow();
        if (email.getPriority() == Priority.HIGH) {
            pushNotificationService.sendUrgent(event.getUserId(), email);
        }
    }
}
```

---

## 📊 Monitoring & Observability

### **1. Application Metrics với Micrometer**

```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class EmailService {

    private final Counter emailSentCounter;
    private final Counter emailFailureCounter;
    private final Timer emailProcessingTimer;
    private final Gauge activeEmailsGauge;

    public EmailService(MeterRegistry registry) {
        this.emailSentCounter = Counter.builder("emails.sent")
            .description("Total emails sent")
            .tag("service", "email")
            .register(registry);

        this.emailFailureCounter = Counter.builder("emails.failed")
            .description("Total emails failed")
            .tag("service", "email")
            .register(registry);

        this.emailProcessingTimer = Timer.builder("emails.processing.time")
            .description("Email processing duration")
            .tag("service", "email")
            .register(registry);

        this.activeEmailsGauge = Gauge.builder("emails.active", this,
                EmailService::getActiveEmailCount)
            .description("Number of active emails")
            .register(registry);
    }

    @Timed(value = "emails.send", description = "Time to send email")
    public EmailResponse sendEmail(String userId, EmailRequest request) {
        return emailProcessingTimer.record(() -> {
            try {
                EmailResponse response = gmailService.sendEmail(userId, request);
                emailSentCounter.increment();
                return response;
            } catch (Exception e) {
                emailFailureCounter.increment();
                throw e;
            }
        });
    }

    private double getActiveEmailCount() {
        return emailRepository.countByStatus(EmailStatus.PROCESSING);
    }
}
```

### **2. Distributed Tracing với Sleuth & Zipkin**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  sleuth:
    sampler:
      probability: 1.0 # Sample 100% of requests in dev
  zipkin:
    base-url: http://localhost:9411
    enabled: true
```

```java
@Service
public class EmailService {

    private final Tracer tracer;

    public EmailService(Tracer tracer) {
        this.tracer = tracer;
    }

    public EmailResponse sendEmail(String userId, EmailRequest request) {
        Span span = tracer.nextSpan().name("send-email").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.id", userId);
            span.tag("email.to", request.getTo());
            span.tag("email.subject", request.getSubject());

            // Step 1: Validate
            Span validateSpan = tracer.nextSpan().name("validate-email").start();
            try (Tracer.SpanInScope ws2 = tracer.withSpan(validateSpan)) {
                validateEmail(request);
            } finally {
                validateSpan.end();
            }

            // Step 2: Send via Gmail
            Span gmailSpan = tracer.nextSpan().name("call-gmail-api").start();
            try (Tracer.SpanInScope ws2 = tracer.withSpan(gmailSpan)) {
                return gmailService.sendEmail(userId, request);
            } finally {
                gmailSpan.end();
            }
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### **3. Structured Logging**

```java
@Configuration
public class LoggingConfig {

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger("application");
    }
}

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailResponse sendEmail(String userId, EmailRequest request) {
        // Structured logging with MDC
        MDC.put("userId", userId);
        MDC.put("emailTo", request.getTo());
        MDC.put("operation", "send-email");

        try {
            logger.info("Sending email",
                kv("subject", request.getSubject()),
                kv("hasAttachments", request.getAttachments().size() > 0)
            );

            EmailResponse response = gmailService.sendEmail(userId, request);

            logger.info("Email sent successfully",
                kv("emailId", response.getId()),
                kv("duration", response.getProcessingTime())
            );

            return response;

        } catch (GmailApiException e) {
            logger.error("Gmail API error",
                kv("errorCode", e.getCode()),
                kv("errorMessage", e.getMessage()),
                e
            );
            throw new EmailSendException("Failed to send email", e);

        } finally {
            MDC.clear();
        }
    }
}
```

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeStructuredArguments>true</includeStructuredArguments>
            <includeTags>true</includeTags>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 🔧 Deployment & DevOps

### **1. Multi-stage Docker Build**

```dockerfile
# Dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -D -u 1000 -G appgroup appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### **2. Kubernetes Deployment**

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-service
  namespace: email-app
  labels:
    app: backend
    version: v1.0.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: backend-sa
      containers:
        - name: backend
          image: your-registry/backend:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: MONGODB_URI
              valueFrom:
                secretKeyRef:
                  name: backend-secrets
                  key: mongodb-uri
            - name: GOOGLE_CLIENT_ID
              valueFrom:
                secretKeyRef:
                  name: backend-secrets
                  key: google-client-id
            - name: GOOGLE_CLIENT_SECRET
              valueFrom:
                secretKeyRef:
                  name: backend-secrets
                  key: google-client-secret
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: backend-secrets
                  key: jwt-secret
            - name: AI_SERVICE_URL
              value: "http://ai-service:80"
          resources:
            requests:
              memory: "1Gi"
              cpu: "1000m"
            limits:
              memory: "4Gi"
              cpu: "4000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          volumeMounts:
            - name: logs
              mountPath: /app/logs
            - name: config
              mountPath: /app/config
              readOnly: true
      volumes:
        - name: logs
          emptyDir: {}
        - name: config
          configMap:
            name: backend-config

---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: email-app
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
      name: http

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: email-app
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
        - type: Pods
          value: 2
          periodSeconds: 60
      selectPolicy: Max

---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: backend-pdb
  namespace: email-app
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: backend
```

### **3. CI/CD Pipeline**

```yaml
# .github/workflows/backend-deploy.yml
name: Backend CI/CD

on:
  push:
    branches: [main, develop]
    paths:
      - "backend/**"
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: "21"
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: "maven"

      - name: Run tests
        run: |
          cd backend
          mvn clean test

      - name: Generate coverage report
        run: |
          cd backend
          mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: backend/target/site/jacoco/jacoco.xml
          flags: backend

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Trivy security scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: "fs"
          scan-ref: "backend"
          format: "sarif"
          output: "trivy-results.sarif"

      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: "trivy-results.sarif"

  build:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix={{branch}}-

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          platforms: linux/amd64,linux/arm64

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}

      - name: Deploy to staging
        run: |
          kubectl set image deployment/backend-service \
            backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
            -n email-app-staging
          kubectl rollout status deployment/backend-service \
            -n email-app-staging --timeout=5m

  integration-test:
    needs: deploy-staging
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run integration tests
        run: |
          cd backend
          mvn verify -Pintegration-test \
            -Dtest.url=${{ secrets.STAGING_URL }}

  deploy-production:
    needs: integration-test
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_PROD }}

      - name: Deploy to production (Blue-Green)
        run: |
          # Deploy to green environment
          kubectl set image deployment/backend-service-green \
            backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
            -n email-app

          # Wait for rollout
          kubectl rollout status deployment/backend-service-green \
            -n email-app --timeout=10m

          # Run smoke tests
          ./scripts/smoke-test.sh ${{ secrets.PROD_URL_GREEN }}

          # Switch traffic
          kubectl patch service backend-service -n email-app \
            -p '{"spec":{"selector":{"version":"green"}}}'

          # Monitor for 5 minutes
          sleep 300

          # If successful, update blue
          kubectl set image deployment/backend-service-blue \
            backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
            -n email-app
```

---

## 📝 Conclusion

Backend Service là trái tim của ứng dụng email, xử lý:

- ✅ **Authentication**: Secure login với Google OAuth2 & JWT
- ✅ **Gmail Integration**: Full email functionality
- ✅ **Business Logic**: Rules, validation, orchestration
- ✅ **Data Management**: MongoDB persistence
- ✅ **Service Orchestration**: Coordinate AI & Gmail APIs
- ✅ **Security**: Enterprise-grade protection
- ✅ **Scalability**: Horizontal scaling với Kubernetes
- ✅ **Resilience**: Circuit breaker, retry, rate limiting
- ✅ **Observability**: Comprehensive monitoring & tracing
- ✅ **Performance**: Caching, async processing, optimization
- ✅ **Testing**: High coverage với unit, integration, performance tests
- ✅ **DevOps**: CI/CD pipeline, Docker, Kubernetes deployment

Spring Boot framework cung cấp foundation vững chắc với production-ready features, giúp team focus vào business logic thay vì infrastructure!
