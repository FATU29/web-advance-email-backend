# LAYERED ARCHITECTURE - BACKEND SERVICE

## Khái Niệm Layered Architecture

Layered Architecture (Kiến trúc phân lớp) là một design pattern trong software architecture được sử dụng rộng rãi để tổ chức code thành các lớp (layers) riêng biệt, mỗi lớp có trách nhiệm và mục đích cụ thể, độc lập với nhau. Trong kiến trúc này, các lớp được xếp chồng theo thứ bậc từ cao xuống thấp, trong đó mỗi lớp chỉ được phép tương tác trực tiếp với lớp liền kề dưới nó, tạo thành một luồng dữ liệu và xử lý logic rõ ràng từ trên xuống dưới (top-down approach). Ví dụ cụ thể: Presentation Layer (lớp giao diện) chỉ gọi Business Logic Layer (lớp xử lý nghiệp vụ), Business Logic Layer chỉ gọi Data Access Layer (lớp truy xuất dữ liệu), và Data Access Layer tương tác trực tiếp với database. Kiến trúc này mang lại nhiều lợi ích quan trọng: Separation of Concerns (tách biệt trách nhiệm - mỗi lớp làm một việc duy nhất), Maintainability (dễ bảo trì - thay đổi ở một lớp ít ảnh hưởng đến các lớp khác), Testability (dễ test - có thể test từng lớp độc lập bằng cách mock các dependency), Reusability (tái sử dụng - business logic có thể được gọi từ nhiều controllers khác nhau), và Scalability (dễ mở rộng - có thể thay thế hoặc nâng cấp từng lớp mà không ảnh hưởng toàn bộ hệ thống). Trong dự án Backend này, chúng ta áp dụng Layered Architecture với 3 lớp chính: Presentation Layer (Controller) xử lý HTTP requests/responses và validation, Business Logic Layer (Service) chứa toàn bộ business rules và orchestration logic, và Data Access Layer (Repository) thực hiện các thao tác với database thông qua Repository Pattern, tạo nên một hệ thống có cấu trúc rõ ràng, dễ hiểu, dễ maintain và scale theo thời gian.

---

## Kiến Trúc Backend Chi Tiết

### Tổng Quan

Backend sử dụng **Layered Architecture (3-Layer)** kết hợp với **Repository Pattern** và **MVC Pattern**.

```
┌─────────────────────────────────────────┐
│   PRESENTATION LAYER (Controller)       │  ← HTTP Requests/Responses
├─────────────────────────────────────────┤
│   BUSINESS LOGIC LAYER (Service)        │  ← Business Rules & Orchestration
├─────────────────────────────────────────┤
│   DATA ACCESS LAYER (Repository)        │  ← Database Operations
└─────────────────────────────────────────┘
              ↓
         [Database]
```

---

## 1. PRESENTATION LAYER (Controller)

### Vai Trò

Presentation Layer đóng vai trò là điểm vào (entry point) của ứng dụng, chịu trách nhiệm tiếp nhận các HTTP requests từ client (Frontend), thực hiện validation cơ bản trên dữ liệu đầu vào, xử lý các ngoại lệ (exception handling), và format response trả về cho client theo chuẩn REST API. Layer này hoạt động như một "cổng giao tiếp" giữa thế giới bên ngoài và logic nghiệp vụ bên trong của ứng dụng, đảm bảo rằng chỉ những requests hợp lệ mới được chuyển tiếp xuống Business Logic Layer để xử lý. Presentation Layer KHÔNG chứa business logic - nó chỉ làm nhiệm vụ điều phối (coordination), validate input, gọi Service layer để xử lý, và transform kết quả thành HTTP response phù hợp. Điều này giúp tách biệt rõ ràng giữa cách thức giao tiếp với bên ngoài (HTTP protocol) và cách thức xử lý nghiệp vụ bên trong (business rules), làm cho code dễ test hơn vì có thể test business logic mà không cần quan tâm đến HTTP layer.

### Cấu Trúc

```
controller/
├── AuthController.java          → Authentication endpoints
│   - POST /api/auth/login
│   - POST /api/auth/signup
│   - POST /api/auth/refresh
│   - GET /api/auth/oauth2/google
│
├── EmailController.java         → Email CRUD operations
│   - GET /api/emails            (pagination, filters)
│   - GET /api/emails/{id}
│   - POST /api/emails           (send email)
│   - PUT /api/emails/{id}
│   - DELETE /api/emails/{id}
│   - POST /api/emails/{id}/star
│   - POST /api/emails/{id}/read
│
├── KanbanController.java        → Kanban board management
│   - GET /api/kanban/boards
│   - POST /api/kanban/boards
│   - PUT /api/kanban/boards/{id}/emails/{emailId}/move
│
├── SearchController.java        → Search functionality
│   - GET /api/search
│   - POST /api/search/semantic
│
├── MailboxController.java       → Mailbox operations
└── AttachmentController.java    → File handling
```

### Trách Nhiệm Cụ Thể

#### 1. Request Validation

```java
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    @PostMapping
    public ResponseEntity<EmailResponse> sendEmail(
        @Valid @RequestBody EmailRequest request  // ← @Valid triggers validation
    ) {
        // Validation tự động check:
        // - Email format hợp lệ không
        // - Required fields có đủ không
        // - String length trong giới hạn không

        // Nếu validation fails → throw MethodArgumentNotValidException
        // → GlobalExceptionHandler catches và return 400 Bad Request
    }
}
```

#### 2. Authentication Context

```java
@GetMapping
@PreAuthorize("hasRole('USER')")  // Spring Security annotation
public ResponseEntity<PagedResponse<Email>> getEmails(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "50") int size
) {
    // Get authenticated user từ SecurityContext
    String userId = SecurityContextHolder.getContext()
        .getAuthentication()
        .getName();

    // Controller biết "ai" đang request, pass xuống Service
    return ResponseEntity.ok(emailService.getUserEmails(userId, page, size));
}
```

#### 3. Response Formatting

```java
@GetMapping("/{id}")
public ResponseEntity<EmailResponse> getEmail(@PathVariable String id) {
    Email email = emailService.getEmailById(getCurrentUserId(), id);

    // Transform domain model → DTO (Data Transfer Object)
    EmailResponse response = EmailResponse.from(email);

    // Return with proper HTTP status
    return ResponseEntity.ok(response);
}

@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    // Convert exception → user-friendly error response
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, ex.getMessage()));
}
```

### Lợi Ích

- **Tách biệt HTTP concerns**: Business logic không biết gì về HTTP, có thể reuse cho gRPC, GraphQL, messaging queues...
- **Centralized validation**: Input validation ở một chỗ, không scatter trong business logic
- **Consistent API**: Tất cả endpoints follow cùng conventions (status codes, error format, pagination)
- **Easy versioning**: Có thể tạo v1, v2 controllers mà không ảnh hưởng business logic
- **Security boundary**: Spring Security filter chain check auth/authorization trước khi vào controller

---

## 2. BUSINESS LOGIC LAYER (Service)

### Vai Trò

Business Logic Layer là trái tim của ứng dụng, nơi chứa toàn bộ business rules, application logic, và orchestration giữa các components khác nhau, đóng vai trò quyết định "ứng dụng làm gì" thay vì "ứng dụng giao tiếp ra sao" (Controller) hay "dữ liệu được lưu như thế nào" (Repository). Layer này implement các use cases của ứng dụng - ví dụ "gửi email" không chỉ đơn giản là insert record vào database, mà còn bao gồm validate business rules (không cho gửi quá 100 emails/giờ), gọi Gmail API để gửi email thật, lưu metadata vào database, invalidate cache, log activities, handle failures với retry logic, và có thể trigger background jobs (như virus scan attachments). Service layer orchestrate nhiều repositories, external services (Gmail API, AI Service), và các services khác để hoàn thành một business operation phức tạp, đồng thời đảm bảo tính nhất quán của data (consistency) thông qua transactions khi cần thiết. Một điểm quan trọng là Service layer độc lập hoàn toàn với cách thức giao tiếp bên ngoài - code ở đây không biết gì về HTTP, không import Spring MVC classes, do đó có thể reuse cho các interfaces khác (CLI, gRPC, message queue) mà không cần thay đổi. Services cũng là nơi tối ưu để implement cross-cutting concerns như caching, logging, monitoring, và business metrics, giúp tách biệt những concerns này ra khỏi controllers và repositories.

### Cấu Trúc

```
service/
├── AuthService.java               → Authentication & Authorization
│   - authenticateUser()
│   - handleOAuth2Login()
│   - refreshAccessToken()
│   - validateToken()
│
├── EmailService.java              → Core email business logic
│   - getUserEmails()
│   - getEmailById()
│   - sendEmail()
│   - deleteEmail()
│   - markAsRead()
│   - starEmail()
│   - getEmailWithSummary()
│
├── GmailService.java              → Gmail API integration
│   - fetchEmails()
│   - sendEmailViaGmail()
│   - modifyLabels()
│   - syncWithGmail()
│
├── KanbanService.java             → Kanban operations
│   - createBoard()
│   - moveEmail()
│   - getKanbanBoard()
│
├── SemanticSearchService.java     → AI-powered search
│   - semanticSearch()
│   - generateEmbeddings()
│
├── MailboxService.java            → Mailbox management
├── OtpService.java                → OTP verification
├── TokenService.java              → Token management
└── AIServiceClient.java           → AI service communication
```

### Trách Nhiệm Cụ Thể

Business Logic Layer có bốn trách nhiệm cốt lõi làm nên giá trị thực sự của ứng dụng. Trách nhiệm đầu tiên và quan trọng nhất là Business Rules Enforcement - việc đảm bảo mọi operation đều tuân thủ các quy tắc nghiệp vụ được định nghĩa, không cho phép bất kỳ vi phạm nào xảy ra dù request đã pass validation ở Controller layer. Khi một operation như sendEmail được gọi, Service layer không chỉ đơn thuần gửi email mà phải kiểm tra một loạt business rules: user có đang vi phạm rate limit không (ví dụ chỉ cho gửi 100 emails/giờ để tránh spam), danh sách recipients có hợp lệ và tồn tại không, nội dung email có chứa spam keywords hay patterns đáng ngờ không, user có đủ quota để gửi email không, email size có vượt quá giới hạn không. Mỗi rule violation throw một custom exception với message rõ ràng giải thích tại sao operation không được phép, giúp user hiểu và sửa lỗi thay vì nhận được generic error message. Business rules được centralized ở Service layer giúp đảm bảo consistency - dù email được gửi từ web UI, mobile app, hay scheduled job thì đều phải tuân thủ cùng rules, không có cách nào bypass được. Trách nhiệm thứ hai là Multi-Source Orchestration - việc phối hợp nhiều data sources và external services khác nhau để hoàn thành một business operation phức tạp, đóng vai trò như một conductor trong dàn nhạc. Ví dụ operation getEmailWithSummary không đơn giản là fetch email từ database mà là một chuỗi steps phức tạp: đầu tiên check cache xem summary đã được generate trước đó chưa (performance optimization), nếu cache miss thì fetch email content từ Gmail API hoặc MongoDB cache tùy vào data freshness, sau đó call AI Service (external microservice) để generate summary - đây là expensive operation tốn 2-3 seconds, cache kết quả trong Redis với TTL 24 hours để tránh phải generate lại cho cùng email, update database với summary và timestamp để track khi nào summary được tạo, và cuối cùng return combined result. Nếu AI Service unavailable (network error, service down, timeout), Service layer implement graceful degradation - return email without summary thay vì fail toàn bộ request, log warning để ops team aware nhưng không impact user experience. Orchestration cũng bao gồm việc coordinate giữa Gmail API (source of truth cho email content) và MongoDB (local cache cho performance), đảm bảo data consistency thông qua cache invalidation strategies. Trách nhiệm thứ ba là Transaction Management - việc đảm bảo data consistency khi một business operation involves multiple database operations phải succeed or fail together (ACID properties). Service layer định nghĩa transaction boundaries thông qua @Transactional annotation, specify rõ operations nào cần atomic execution. Ví dụ deleteEmail operation phải thực hiện nhiều steps: delete email từ Gmail (external API call không thể rollback), delete record từ MongoDB, remove email khỏi kanban board nếu có, clear cache entries related to email. Spring Boot tự động wrap method trong database transaction, nếu bất kỳ database operation nào fails (steps 2-4), transaction tự động rollback, database trở về state trước khi method được gọi. Tuy nhiên có một vấn đề phức tạp: Gmail API call (step 1) đã thành công nhưng không thể rollback vì đó là external system, tạo ra distributed transaction problem. Solution là implement compensating transactions - nếu database operations fail, schedule background job để retry cleanup, hoặc accept eventual consistency - system eventually becomes consistent sau khi background jobs chạy. Annotation @Transactional(readOnly = true) được dùng cho read operations như getUserEmails,告诉 database rằng đây là read-only transaction, database có thể optimize (không cần track changes, có thể dùng read replicas) giúp improve performance. Trách nhiệm thứ tư là Error Handling & Resilience - việc xử lý errors và failures của external systems một cách thông minh, đảm bảo ứng dụng robust và không bị cascade failures khi dependencies có vấn đề. Service layer implement nhiều resilience patterns: Retry pattern với @Retry annotation tự động retry failed operations với exponential backoff - ví dụ khi Gmail API return 429 Rate Limit hay 500 Server Error (transient failures), system tự động retry sau 1 second, 2 seconds, 4 seconds thay vì fail ngay lập tức, tăng success rate dramatically. Circuit Breaker pattern với @CircuitBreaker annotation monitor failure rate của external service (AI Service) - nếu fails 5 lần liên tiếp trong 10 seconds, circuit "opens", ngừng gọi AI Service trong 60 seconds để tránh waste resources và cho service time để recover, thay vào đó dùng fallback method return basic summary (first 200 characters). Sau 60 seconds, circuit chuyển sang "half-open" state, thử gọi AI Service lại, nếu success thì circuit "closes" và hoạt động bình thường, nếu vẫn fail thì circuit opens lại. Timeout configuration đảm bảo không wait forever cho slow external services - mỗi external call có timeout 10 seconds, nếu không response trong thời gian đó thì throw TimeoutException và proceed với fallback logic. Error categorization giúp phân biệt retryable errors (network issues, rate limits, server errors) và non-retryable errors (authentication failed, invalid input, resource not found) để avoid vô ích retry những operations chắc chắn sẽ fail. Tất cả errors được log với full context (userId, operation, parameters, stack trace) để facilitate debugging production issues, nhưng error messages return cho client được sanitized để không expose sensitive information hay internal implementation details.

- **Business logic centralization**: Tất cả business rules ở một nơi, dễ audit và maintain
- **Reusability**: Service có thể được gọi từ nhiều controllers, scheduled jobs, message listeners
- **Testability**: Dễ unit test vì không phụ thuộc vào HTTP hay database (mock được hết)
- **Transaction boundaries**: Rõ ràng operations nào cần ACID guarantees
- **Orchestration**: Hide complexity của việc phối hợp nhiều data sources và external services

---

## 3. DATA ACCESS LAYER (Repository)

### Vai Trò

Data Access Layer chịu trách nhiệm duy nhất là tương tác với database, abstract away tất cả chi tiết về cách thức lưu trữ và truy xuất dữ liệu, cung cấp một interface đơn giản và rõ ràng cho Business Logic Layer để thực hiện các operations CRUD (Create, Read, Update, Delete) mà không cần biết database đang dùng là MongoDB, PostgreSQL, MySQL hay bất kỳ hệ quản trị cơ sở dữ liệu nào khác. Layer này implement Repository Pattern - một design pattern phổ biến giúp tách biệt business logic khỏi data access logic, làm cho code dễ test hơn (có thể mock repository), dễ thay đổi database technology sau này (chỉ cần thay implementation của repository mà không ảnh hưởng service layer), và giúp developer focus vào business logic thay vì phải viết câu lệnh SQL/BSON queries phức tạp. Trong Spring Boot, Repository Layer được implement thông qua Spring Data JPA hoặc Spring Data MongoDB, tận dụng khả năng auto-generate queries từ method names (method name parsing) - ví dụ `findByUserIdAndStarredTrue(String userId)` tự động generate query tìm emails của user có starred = true mà không cần viết code implementation. Repository cũng handle connection pooling, transaction management ở database level, và query optimization thông qua caching và proper indexing strategies.

### Cấu Trúc

```
repository/
├── UserRepository.java              → User data access
│   - findByEmail()
│   - findByGoogleId()
│   - existsByEmail()
│
├── EmailRepository.java             → Email CRUD
│   - findByUserId()
│   - findByUserIdAndLabelContaining()
│   - findByUserIdAndStarredTrue()
│   - findByDateRange()
│
├── KanbanColumnRepository.java      → Kanban data
│   - findByUserId()
│   - findByUserIdAndId()
│
├── MailboxRepository.java           → Mailbox data
├── GoogleTokenRepository.java       → OAuth tokens
├── RefreshTokenRepository.java      → JWT tokens
└── OtpRepository.java               → OTP codes
```

### Trách Nhiệm Cụ Thể

Data Access Layer thông qua Repository Pattern có trách nhiệm duy nhất nhưng cực kỳ quan trọng là abstract away tất cả complexity của database operations, cung cấp cho Business Logic Layer một interface đơn giản, type-safe, và expressive để thực hiện data access mà không cần quan tâm đến underlying database technology, query syntax, connection management, hay performance optimization. Khi Service layer cần fetch starred emails của user, nó chỉ cần gọi repository.findByUserIdAndStarredTrue(userId) - một method call đơn giản như gọi function bình thường, và Spring Data MongoDB tự động parse method name theo convention (findBy = SELECT, UserId = WHERE userId =, And = AND, StarredTrue = WHERE starred = true), generate BSON query tương ứng ({ "userId": userId, "starred": true }), lấy connection từ connection pool (pool of 10-50 pre-established connections để tránh overhead của creating new connection cho mỗi query), execute query với proper indexes được defined trên collection để ensure fast execution (index on userId + starred fields → query take 10-20ms thay vì 2-3 seconds khi scan full collection), parse MongoDB documents thành Java Email objects với automatic field mapping, release connection về pool để reuse cho queries tiếp theo, và return List<Email> với compile-time type safety - compiler sẽ catch lỗi nếu developer accidentally assign result vào wrong type. Pagination là một use case phổ biến khác được Repository Pattern handle elegantly - thay vì developer phải manually calculate skip/limit values, handle edge cases (what if user request page 1000 nhưng chỉ có 10 pages?), count total items, check hasNext/hasPrevious, Spring Data cung cấp Pageable abstraction - developer chỉ cần tạo PageRequest.of(page, size, Sort) object specify page number, page size, và sorting criteria, pass vào repository method, và nhận về Page<Email> object chứa đầy đủ thông tin: current page items, total elements count, total pages, current page number, hasNext/hasPrevious flags. Repository tự động generate 2 queries - một để fetch data với LIMIT và SKIP, một để count total - và combine results vào Page object. Delete operations cũng được simplified - emailRepository.deleteByUserIdAndId(userId, emailId) tự động generate delete query với WHERE conditions, check affected rows count, throw exception nếu email not found (optional behavior), và đảm bảo operation atomic trong database transaction context. Repository Pattern còn hỗ trợ custom queries cho complex use cases không express được qua method names - developer có thể dùng @Query annotation viết raw MongoDB query với parameter binding ({ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 } }), hoặc dùng Criteria API để build dynamic queries programmatically. Query optimization là một benefit lớn khác - repositories tự động leverage indexes defined trên domain models thông qua @Indexed, @CompoundIndex annotations, sử dụng connection pooling để minimize connection overhead, support lazy loading và projection để chỉ fetch fields cần thiết thay vì load entire documents (quan trọng khi email body có thể vài MBs), và integrate với Spring Cache abstraction để cache frequently-accessed data trong memory (Redis, Caffeine) giảm database load. Repository cũng handle transaction participation - khi Service method được annotate với @Transactional, tất cả repository calls trong method đó automatically participate trong cùng transaction, đảm bảo ACID properties. Error handling là transparent - repository throws DataAccessException hierarchy (Spring's abstraction over database-specific exceptions), Service layer catch và convert thành business exceptions với meaningful messages. Repository Pattern làm cho database technology swappable - nếu sau này project quyết định migrate từ MongoDB sang PostgreSQL, chỉ cần change repository implementation và update configurations, Service layer code hoàn toàn không cần sửa vì nó chỉ depend vào repository interfaces, không phải concrete implementations. Testing cũng trở nên đơn giản - trong unit tests, Service layer có thể dùng mock repositories (Mockito.mock(EmailRepository.class)) để test business logic isolation mà không cần real database, trong integration tests có thể dùng in-memory database (H2, embedded MongoDB) để test end-to-end flows without external dependencies. Repository abstraction cũng giúp enforce data access patterns - chỉ có Services mới được phép gọi Repositories, Controllers không bao giờ directly access Repositories (violation của layered architecture principle), đảm bảo tất cả data access đi qua business logic layer nơi business rules được enforced.

findByUserIdAndStarredTrue(String userId)
→ { "userId": userId, "starred": true }

findByUserIdAndDateAfter(String userId, Date date)
→ { "userId": userId, "date": { $gt: date } }

findByUserIdAndLabelContaining(String userId, String label)
→ { "userId": userId, "labels": { $regex: label } }

findByUserIdOrderByDateDesc(String userId)
→ { "userId": userId } với sort: { "date": -1 }

findFirst10ByUserIdOrderByDateDesc(String userId)
→ Limit 10 results với sort

````

### Trách Nhiệm Cụ Thể

Data Access Layer thông qua Repository Pattern có trách nhiệm duy nhất nhưng cực kỳ quan trọng là abstract away tất cả complexity của database operations, cung cấp cho Business Logic Layer một interface đơn giản, type-safe, và expressive để thực hiện data access mà không cần quan tâm đến underlying database technology, query syntax, connection management, hay performance optimization. Khi Service layer cần fetch starred emails của user, nó chỉ cần gọi repository.findByUserIdAndStarredTrue(userId) - một method call đơn giản như gọi function bình thường, và Spring Data MongoDB tự động parse method name theo convention (findBy = SELECT, UserId = WHERE userId =, And = AND, StarredTrue = WHERE starred = true), generate BSON query tương ứng ({ "userId": userId, "starred": true }), lấy connection từ connection pool (pool of 10-50 pre-established connections để tránh overhead của creating new connection cho mỗi query), execute query với proper indexes được defined trên collection để ensure fast execution (index on userId + starred fields → query take 10-20ms thay vì 2-3 seconds khi scan full collection), parse MongoDB documents thành Java Email objects với automatic field mapping, release connection về pool để reuse cho queries tiếp theo, và return List<Email> với compile-time type safety - compiler sẽ catch lỗi nếu developer accidentally assign result vào wrong type. Pagination là một use case phổ biến khác được Repository Pattern handle elegantly - thay vì developer phải manually calculate skip/limit values, handle edge cases (what if user request page 1000 nhưng chỉ có 10 pages?), count total items, check hasNext/hasPrevious, Spring Data cung cấp Pageable abstraction - developer chỉ cần tạo PageRequest.of(page, size, Sort) object specify page number, page size, và sorting criteria, pass vào repository method, và nhận về Page<Email> object chứa đầy đủ thông tin: current page items, total elements count, total pages, current page number, hasNext/hasPrevious flags. Repository tự động generate 2 queries - một để fetch data với LIMIT và SKIP, một để count total - và combine results vào Page object. Delete operations cũng được simplified - emailRepository.deleteByUserIdAndId(userId, emailId) tự động generate delete query với WHERE conditions, check affected rows count, throw exception nếu email not found (optional behavior), và đảm bảo operation atomic trong database transaction context. Repository Pattern còn hỗ trợ custom queries cho complex use cases không express được qua method names - developer có thể dùng @Query annotation viết raw MongoDB query với parameter binding ({ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 } }), hoặc dùng Criteria API để build dynamic queries programmatically. Query optimization là một benefit lớn khác - repositories tự động leverage indexes defined trên domain models thông qua @Indexed, @CompoundIndex annotations, sử dụng connection pooling để minimize connection overhead, support lazy loading và projection để chỉ fetch fields cần thiết thay vì load entire documents (quan trọng khi email body có thể vài MBs), và integrate với Spring Cache abstraction để cache frequently-accessed data trong memory (Redis, Caffeine) giảm database load. Repository cũng handle transaction participation - khi Service method được annotate với @Transactional, tất cả repository calls trong method đó automatically participate trong cùng transaction, đảm bảo ACID properties. Error handling là transparent - repository throws DataAccessException hierarchy (Spring's abstraction over database-specific exceptions), Service layer catch và convert thành business exceptions với meaningful messages. Repository Pattern làm cho database technology swappable - nếu sau này project quyết định migrate từ MongoDB sang PostgreSQL, chỉ cần change repository implementation và update configurations, Service layer code hoàn toàn không cần sửa vì nó chỉ depend vào repository interfaces, không phải concrete implementations. Testing cũng trở nên đơn giản - trong unit tests, Service layer có thể dùng mock repositories (Mockito.mock(EmailRepository.class)) để test business logic isolation mà không cần real database, trong integration tests có thể dùng in-memory database (H2, embedded MongoDB) để test end-to-end flows without external dependencies. Repository abstraction cũng giúp enforce data access patterns - chỉ có Services mới được phép gọi Repositories, Controllers không bao giờ directly access Repositories (violation của layered architecture principle), đảm bảo tất cả data access đi qua business logic layer nơi business rules được enforced.

### Query Optimization

#### 1. Indexing

```java
@Document(collection = "emails")
public class Email {
    @Id
    private String id;

    @Indexed(name = "user_id_idx")
    private String userId;  // Index for fast user lookups

    @Indexed(name = "date_idx")
    private LocalDateTime date;  // Index for sorting by date

    @CompoundIndex(name = "user_starred_idx", def = "{'userId': 1, 'starred': 1}")
    // Compound index for queries filtering by userId AND starred

    @TextIndex
    private String subject;  // Full-text search index
    @TextIndex
    private String body;
}
````

#### 2. Lazy Loading & Projection

```java
public interface EmailRepository extends MongoRepository<Email, String> {

    // Fetch only specific fields (projection)
    @Query(value = "{ 'userId': ?0 }", fields = "{ 'subject': 1, 'from': 1, 'date': 1 }")
    List<EmailSummary> findEmailSummariesByUserId(String userId);

    // Interface-based projection
    interface EmailSummary {
        String getSubject();
        String getFrom();
        LocalDateTime getDate();
    }
}

@Service
public class EmailService {
    public List<EmailSummary> getEmailList(String userId) {
        // Only fetch needed fields - faster query
        // Don't load full email body (could be MBs)
        return emailRepository.findEmailSummariesByUserId(userId);
    }
}
```

### Lợi Ích

- **Abstraction**: Business logic không biết database là gì, dễ thay đổi database technology
- **No boilerplate**: Spring Data auto-implements queries, không cần viết CRUD code
- **Type safety**: Compile-time checks cho query parameters và return types
- **Testing**: Dễ mock repository trong unit tests
- **Optimization**: Spring Data handle connection pooling, caching, batch operations
- **Transaction support**: Automatic transaction management
- **Query consistency**: Queries được define ở một nơi, reuse được nhiều lần

---

## Luồng Xử Lý Request (Request Flow)

### Ví Dụ: User Sends Email

```
1. CLIENT (Frontend)
   POST /api/emails
   Headers: { Authorization: "Bearer <jwt>" }
   Body: { to: "user@example.com", subject: "...", body: "..." }

   ↓

2. PRESENTATION LAYER (EmailController)

   a) Spring Security Filter Chain
      - JwtAuthenticationFilter extracts JWT
      - Validates signature & expiration
      - Loads user from token → sets SecurityContext

   b) EmailController.sendEmail()
      - @Valid validates request body
      - Extracts userId from SecurityContext
      - Calls emailService.sendEmail(userId, request)

   ↓

3. BUSINESS LOGIC LAYER (EmailService)

   a) emailService.sendEmail(userId, request)
      - Check rate limit (business rule)
      - Validate recipients (business rule)
      - Check spam score (business rule)

   b) Orchestrate multiple operations:
      - gmailService.sendEmail() → Gmail API
      - emailRepository.save() → MongoDB
      - cacheManager.invalidate() → Clear cache
      - emailRepository.incrementSentCount() → Update metrics

   ↓

4. DATA ACCESS LAYER (Repository)

   a) gmailService.sendEmail()
      - Get Gmail credentials from DB
      - Build MIME message
      - Call Gmail API
      - Handle token refresh if needed

   b) emailRepository.save(email)
      - Spring Data MongoDB generates query
      - Connection pool provides connection
      - Execute insert operation
      - Return saved entity

   ↓

5. RESPONSE FLOW (back up the layers)

   a) Repository returns: Email entity

   b) Service returns: EmailResponse DTO
      - Transform Email → EmailResponse
      - Add computed fields (summary, etc.)

   c) Controller returns: HTTP Response
      - Wrap in ResponseEntity
      - Add HTTP status (200 OK)
      - Spring converts DTO → JSON

   ↓

6. CLIENT receives response
   {
     "id": "msg123",
     "subject": "...",
     "status": "sent",
     "sentAt": "2026-01-13T10:30:00Z"
   }
```

### Timing Breakdown

```
Total: ~1.5 seconds

- Security filter:        10ms   (JWT validation)
- Controller validation:  5ms    (@Valid annotations)
- Service business logic: 50ms   (rate limits, rules)
- Gmail API call:        1200ms  (network + Google processing)
- MongoDB save:          20ms    (indexed insert)
- Cache invalidation:    5ms
- Response serialization: 10ms   (DTO → JSON)
- HTTP overhead:         200ms   (network)
```

---

## So Sánh Với Kiến Trúc Khác

### Layered vs Microservices

| Aspect          | Layered Architecture       | Microservices                   |
| --------------- | -------------------------- | ------------------------------- |
| **Deployment**  | Monolithic - 1 application | Distributed - nhiều services    |
| **Complexity**  | Đơn giản, dễ hiểu          | Phức tạp, cần orchestration     |
| **Scaling**     | Vertical (thêm CPU/RAM)    | Horizontal (thêm instances)     |
| **Development** | 1 team, 1 codebase         | Nhiều teams, nhiều codebases    |
| **Testing**     | Đơn giản, test toàn bộ app | Phức tạp, cần integration tests |
| **Best For**    | Small-medium apps, MVPs    | Large enterprise, high scale    |

**Khi nào dùng Layered**: Dự án vừa và nhỏ, team nhỏ, deadlines gấp, không cần scale cực lớn.

**Khi nào dùng Microservices**: Dự án lớn, nhiều teams, cần scale độc lập từng phần, long-term investment.

### Layered vs Hexagonal (Ports & Adapters)

| Aspect          | Layered                                      | Hexagonal                              |
| --------------- | -------------------------------------------- | -------------------------------------- |
| **Direction**   | Top-down (Controller → Service → Repository) | Inside-out (Business logic → Adapters) |
| **Dependency**  | Upper layers depend on lower layers          | Core không depend vào infrastructure   |
| **Flexibility** | Medium - thay database/UI cần effort         | High - dễ swap adapters                |
| **Complexity**  | Low                                          | Medium-High                            |
| **Testing**     | Good                                         | Excellent (pure business logic)        |

**Khi nào dùng Layered**: Traditional web apps, CRUD-heavy, team familiar với pattern.

**Khi nào dùng Hexagonal**: Domain-driven design, complex business logic, need maximum flexibility.

---

## Best Practices

### 1. Dependency Direction

```
❌ WRONG - Lower layer depends on upper layer
Repository → imports Service classes

✅ CORRECT - Upper layer depends on lower layer
Service → imports Repository interfaces
```

### 2. Không Skip Layers

```
❌ WRONG - Controller directly calls Repository
@RestController
public class EmailController {
    @Autowired
    private EmailRepository emailRepository;  // Skip Service layer

    @GetMapping
    public List<Email> getEmails() {
        return emailRepository.findAll();  // No business logic!
    }
}

✅ CORRECT - Always go through Service
@RestController
public class EmailController {
    @Autowired
    private EmailService emailService;  // Use Service

    @GetMapping
    public List<Email> getEmails() {
        return emailService.getUserEmails(getCurrentUserId());
    }
}
```

### 3. Thin Controllers, Fat Services

```
❌ WRONG - Business logic in Controller
@PostMapping("/emails")
public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
    if (request.getTo().isEmpty()) return badRequest();
    if (hasExceededRateLimit()) return tooManyRequests();
    if (isSpam(request.getBody())) return forbidden();
    // More business logic...
}

✅ CORRECT - Controller chỉ coordinate
@PostMapping("/emails")
public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
    EmailResponse response = emailService.sendEmail(getCurrentUserId(), request);
    return ResponseEntity.ok(response);
}

// Business logic trong Service
@Service
public class EmailService {
    public EmailResponse sendEmail(String userId, EmailRequest request) {
        validateBusinessRules(userId, request);
        // All business logic here
    }
}
```

### 4. Use DTOs, Not Entities

```
❌ WRONG - Return entity directly
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) {
    return userRepository.findById(id);  // Exposes internal structure
}

✅ CORRECT - Return DTO
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable String id) {
    User user = userService.getUserById(id);
    return UserResponse.from(user);  // Transform entity → DTO
}
```

### 5. Transaction Boundaries

```
✅ Transactions belong in Service Layer
@Service
@Transactional  // ← Here
public class EmailService {
    public void sendEmail(...) {
        // Multiple repository calls trong 1 transaction
    }
}

❌ NOT in Controller
@RestController
@Transactional  // ← Wrong place
public class EmailController { ... }

❌ NOT in Repository
// Repositories don't need @Transactional - they're just data access
```

---

## Kết Luận

**Layered Architecture** kết hợp với **Repository Pattern** là lựa chọn tối ưu cho dự án Backend này vì:

✅ **Đơn giản**: Dễ hiểu, dễ implement, team onboarding nhanh

✅ **Proven**: Pattern được sử dụng rộng rãi, có nhiều best practices

✅ **Spring Boot Native**: Spring Boot được thiết kế cho pattern này

✅ **Testable**: Dễ unit test và integration test

✅ **Maintainable**: Code organized rõ ràng, dễ tìm và sửa bugs

✅ **Scalable**: Đủ cho medium-scale applications (thousands users)

Đây là foundation vững chắc để xây dựng enterprise applications! 🚀
