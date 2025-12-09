# Spring Boot Todo 인터뷰 질문지

> 이 문서는 프로젝트를 제출한 개발자를 대상으로 한 기술 면접 질문입니다.
> 각 질문에는 **[키워드]**로 관련 개념을 표시했습니다.

---

## 1. 아키텍처 및 설계

### 1.1 레이어드 아키텍처
- Controller / Service / Domain 레이어의 책임을 어떻게 구분했나요? 예를 들어 `TodoController`에서 `toResponse()` 변환 로직이 있는데, 이것이 Controller에 있어야 하는 이유는 무엇인가요?
  **[Layered Architecture, DTO 변환, Single Responsibility Principle]**
- 답변
  - Controller는 클라이언트와의 요청/응답을 담당하고 
  - Service는 유즈케이스를 메서드로 한 눈에 볼 수 있게 하였고
  - Domain에서는 각 Entity에 대한 필드, 어떤 테이블을 사용하는지를 정의하였습니다.
  - Controller가 클라이언트에게 요청 받고, 응답을 해주기 때문에
  - 어떤 형태로 데이터를 보내줄지 결정하는 toResponse는 Controller에 있어야한다고 생각합니다.

- `TodoService`가 `AuthService`를 의존하고 있는데, 이런 Service 간 의존이 적절한가요? 순환 의존이 발생하면 어떻게 해결할 건가요?
  **[Circular Dependency, DI Container, @Lazy, 이벤트 기반 분리]**
- 답변
  - 하나의 서비스가 또 다른 서비스를 의존하고 있는건 좋지않습니다.
  - 이유는 서비스간 의존성이 없는 것이 좋기 떄문입니다. 
  - 명확한 이유는 잘 모르겠습니다..
  - 단일 책임 원칙(SRP)위반
  - TodoService의 책임은 Todo 관련 비즈니스 로직이어야 하는데
  - AuthService를 직접 의존하면 인증 로직까지 알어야 합니다.
  - AuthService의 구현이 바뀌면 TodoService도 영향을 받아서 결합도가 높아집니다.
 
### 1.2 도메인 모델 설계
- `Todo` 엔티티에 `toggleComplete()`, `changeAssignees()` 같은 비즈니스 메서드를 넣은 이유는 무엇인가요? Service에 두는 것과 어떤 차이가 있나요?
  **[Rich Domain Model vs Anemic Domain Model, DDD, 캡슐화]**
- 답변
  - Todo 엔티티 자체의 값을 변경하는 것이기 때문에 service보다 엔티티에 넣는 것이 맞다고 생각하였습니다.
  - Todo의 상태 변경을 Todo안에 넣어서 외부에서 setter로 임의로 바꾸는 걸 막을 수 있습니다.
  - 도메인 로직을 엔티티에 두는 걸 Rich Domain Model이라고 하고 엔티티가 getter/setter만 있고 로직이 없으면 Anemic Domain Model이라고 합ㄴ디ㅏ.
  - DDD(Domain-Driven Design)에서는 비즈니스 로직을 도메인 객체에 두는 걸 권장합니다.

### 1.3 Soft Delete
- `@Where(clause = "deleted_at IS NULL")`로 Soft Delete를 구현했는데, 삭제된 데이터를 조회해야 할 때는 어떻게 하나요?
  **[@Where, @FilterDef, Native Query, Hibernate Filter]**
- 답변
  - Native Query 사용
    - deleted_at IS NOT NULL 조건으로 직접 SQL을 작성해서 삭제된 데이터를 조회할 수 있습니다.
    - @Query(value = "SELECT * FROM todos WHERE deleted_at IS NOT NULL", nativeQuery = true
  - @FilterDef와 @Filter 사용
    - @Where 대신 Hibernate Filter를 사용하면 필요할 때만 필터를 끄거나 켤 수 있습니다.
    - 예시) 특정 페이지에서는 필터를 끄고 삭제된 데이터까지 볼 수 있습니다.
  - 지금은 @Where를 사용하고 있어서 삭제된 데이터 조회가 필요하면 Native Query를 추가로 작성해야 합니다.

- Soft Delete된 Todo의 `orderIndex`는 어떻게 처리되나요? 삭제 후 남은 Todo들의 순서에 갭(gap)이 생기지 않나요?
  **[데이터 정합성, orderIndex 재정렬, 배치 처리]**
- 답변
  - Soft Delete를 적용하면 기존 Todo orderIndex가 그대로 남기 때문 중간에 Todo가 삭제될 경우 orderIndex에 갭이 생기는 문제가 있었습니다.
  - 이걸 삭제시 재정렬 로직을 추가하여 연속된 orderIndex가 유지되도록 개선하였습니다.

---

## 2. 보안 및 인증

### 2.1 JWT 구조
- 현재 JWT 페이로드에 `subject(userId)`, `issuedAt`, `expiration`만 있는데, access token과 refresh token을 어떻게 구분하나요? 구분하지 않으면 어떤 문제가 생기나요?
  **[JWT Claims, Token Type Claim, 토큰 오용 공격]**
- 답변
  - 지금은 만료시간만 다르고 토큰 구조는 동일해서 구분하지 못합니다.
  - 구분하지 않으면 refresh API에 accessToken을 넣어도 동작해버릴 수 있습니다.
  - type을 추가해서 구분을 하는게 좋을 것 같습니다.

- `/api/auth/refresh` 엔드포인트에 만료되지 않은 access token을 넣으면 어떻게 되나요? 이것이 보안상 문제가 될 수 있나요?
  **[Token Confusion Attack, Claim Validation]**
- 답변
  - 지금은 refresh API에 만료되지 않은 accessToken을 넣어도 검증이 됩니다..
  - type을 추가해서 구분을 하는게 좋을 것 같습니다. refresh API에서는 type이 refresh인지 검증해야 합니다.

### 2.2 인증 필터
- `JwtAuthenticationFilter`에서 토큰이 없거나 유효하지 않을 때 그냥 `filterChain.doFilter()`로 넘기는데, 이렇게 한 이유는 무엇인가요? 401 응답을 직접 반환하지 않은 이유는?
  **[Filter Chain, Spring Security AuthenticationEntryPoint, 인증 vs 인가 분리]**
- 답변
  - 인증이 필요한 API와 필요없는 API를 구분하기 위해서입니다.
  - Filter는 토큰 추출만 담당하고, 인증 필요 여부는 SecurityConfig에서 URL별로 설정하였습니다.
  - /api/auth/login, /api/auth/register는 토큰 없이 접근 가능해야합니다.
  - 401을 바로 반환하면 토큰 없이 접근 가능한 API도 다 막혀버립니다.

- `UsernamePasswordAuthenticationToken`의 principal에 `userId`(Long)만 넣었는데, `UserDetails`를 구현한 객체를 넣지 않은 이유는 무엇인가요? 권한(Role) 기반 인가를 추가하려면 어떻게 해야 하나요?
  **[Principal, UserDetails, GrantedAuthority, @PreAuthorize]**
- 답변
  - 처음에 작은 프로젝트로 판단하여 userId만 넣었었습니다.
  - UserDetail를 구현한 CustomUserDetails를 생성하고 userId, email, role을 저장합니다.
  - Filter에서 CustomUserDetail를 Principal로 설정하면 될 것 같습니다.

### 2.3 토큰 관리
- Refresh Token이 탈취되면 어떻게 대응하나요? 현재 구조에서 특정 토큰만 무효화할 수 있나요?
  **[Token Revocation, Token Blacklist, Redis, Refresh Token Rotation]**
- 답변
  - 현재는 특정 토큰만 무효화할 수 없습니다.
  - JWT를 서버에 저장하지 않는 Stateless 방식이라 '이 토큰이 무효화되었는지' 확인할 방법이 없습니다. 한 번 발급되면 만료 시간까지는 계속 유효합니다..
  - Refresh Token 탈취시 공격자가 7일 동안 계속 새로운 accessToken을 발급받을 수 있습니다.
  - Token BlackList
    - 로그아웃시 해당 토큰을 Redis에 저장
    - 매 요청마다 블랙리스트에 있는지 확인하여 차단
  - Refresh Token Rotation(토큰 교체)
    - Refresh Token을 한 번 사용하면 새로운 Refresh Token 발급
    - 기존 Refresh Token은 데이터베이서에서 삭제하여 무효화
    - 이미 사용된 토큰이 다시 요청되면 탈취로 판단
  - Refresh Token을 데이터베이스에 저장
    - Refresh Token을 발급할 때 DB에 저장
    - 갱신 요청시 DB에 있는지 확인

- access token 만료 시간이 10분, refresh token이 7일인데, 이 값을 어떤 기준으로 정했나요?
  **[Token Lifetime, 보안 vs UX 트레이드오프, Sliding Session]**
- 답변
  - 임의 시간을 넣은 것입나다. 10분과 7일이 너무 짧지도 너무 길지도 않은 시간이라 판단하여 넣었습니다. 

---

## 3. JPA 및 데이터베이스

### 3.1 엔티티 매핑
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용했는데, Lazy Loading이 동작하지 않는 경우는 언제인가요? N+1 문제가 발생할 수 있는 부분이 있나요?
  **[Lazy Loading, Proxy, N+1 Problem, @EntityGraph, Fetch Join]**
- 답변
  - 

- `Todo`와 `User` 관계에서 `CascadeType`을 지정하지 않았는데, User 삭제 시 해당 User의 Todo는 어떻게 되나요?
  **[CascadeType, orphanRemoval, 참조 무결성, ON DELETE CASCADE]**

### 3.2 시퀀스 전략
- `GenerationType.SEQUENCE`를 선택한 이유는 무엇인가요? `IDENTITY`와 비교했을 때 장단점은?
  **[ID Generation Strategy, Batch Insert, allocationSize, Hi-Lo Algorithm]**

- `allocationSize = 1`로 설정했는데, 이 값이 성능에 미치는 영향은 무엇인가요?
  **[Sequence Allocation, DB Round Trip, Pre-allocation]**

### 3.3 트랜잭션
- `TodoService` 클래스에 `@Transactional`을 붙이고, `getTodos()`에만 `@Transactional(readOnly = true)`를 붙였는데, 이 차이점은 무엇인가요?
  **[Transaction Propagation, Read-Only Optimization, Dirty Checking, Flush Mode]**

- `move()` 메서드에서 여러 Todo의 `orderIndex`를 변경하는데, 중간에 예외가 발생하면 어떻게 되나요?
  **[Transaction Atomicity, Rollback, @Transactional 경계]**

---

## 4. API 설계 및 예외 처리

### 4.1 RESTful 설계

- 생성 성공 시 201과 `Location` 헤더를 반환하는데, 이 헤더의 용도는 무엇인가요?
  **[HTTP Status Code, Location Header, HATEOAS]**

### 4.2 예외 처리
- `TodoNotFoundException`이 `@ResponseStatus(HttpStatus.UNAUTHORIZED)`로 401을 반환하는데, 이것이 적절한가요? 401, 403, 404를 어떤 기준으로 구분해야 하나요?
  **[HTTP 401 vs 403 vs 404, 정보 노출 방지, Security by Obscurity]**

- `IllegalArgumentException`이 발생하면 500 에러가 반환되는데, 이를 400으로 바꾸려면 어떻게 해야 하나요?
  **[@ControllerAdvice, @ExceptionHandler, ResponseEntityExceptionHandler]**

### 4.3 Validation
- `CreateTodoRequest`에서 `title`만 `@NotEmpty`이고 `content`는 검증이 없는데, 엔티티에서는 `content`가 `@NotNull`입니다. 이 불일치가 문제를 일으킬 수 있나요?
  **[DTO vs Entity Validation, Fail-Fast, Bean Validation Groups]**

- `@Valid`와 `@Validated`의 차이점은 무엇인가요? Validation Group은 언제 사용하나요?
  **[JSR-303, Spring Validation, Validation Groups]**

---

## 5. 비즈니스 로직

### 5.1 순서 관리 (orderIndex)
- Todo 생성 시 `orderIndex`를 지정하면 해당 위치에 삽입되나요, 아니면 기존 항목들이 밀려나나요? 현재 코드에서는 어떻게 동작하나요?
  **[Insert Position, Index Shifting, Conflict Resolution]**

- `move()` 메서드에서 `targetOrderIndex`가 음수이거나 전체 개수를 초과하면 어떻게 되나요?
  **[Boundary Validation, Defensive Programming]**

- 두 사용자가 동시에 같은 Todo의 순서를 변경하면 어떤 일이 발생하나요? 이를 어떻게 방지하거나 감지할 수 있나요?
  **[Optimistic Locking, @Version, Pessimistic Lock, Lost Update Problem]**

### 5.2 담당자 관리 (Assignees)
- `changeAssignees()`에서 `assignees.clear()` 후 새로 추가하는 방식인데, 기존 담당자 중 일부만 유지하고 싶을 때도 전체를 교체해야 하나요?
  **[Collection 관리, orphanRemoval, 부분 업데이트 vs 전체 교체]**

- 존재하지 않는 `assigneeId`를 전달하면 어떻게 되나요? 에러 메시지가 사용자에게 어떤 정보를 노출하나요?
  **[입력 검증, 에러 메시지 정보 노출, Enumeration Attack]**

---

## 6. 테스트

### 6.1 테스트 전략
- 현재 테스트가 `contextLoads()` 하나뿐인데, 어떤 테스트를 우선적으로 작성해야 한다고 생각하나요?
  **[Test Pyramid, Unit Test, Integration Test, 테스트 우선순위]**

---

## 7. 운영 및 설정

### 7.1 설정 관리
- `application.properties`에 JWT secret과 DB 비밀번호가 평문으로 있는데, 프로덕션에서는 어떻게 관리해야 하나요?
  **[환경변수, Spring Cloud Config, Vault, Kubernetes Secrets]**

- 개발/스테이징/프로덕션 환경별로 설정을 다르게 하려면 어떻게 해야 하나요?
  **[Spring Profiles, application-{profile}.properties, @Profile]**

### 7.2 로깅 및 모니터링
- 현재 로깅 설정이 없는데, 어떤 정보를 어떤 레벨로 로깅해야 하나요? 민감 정보(비밀번호, 토큰)가 로그에 남으면 안 되는데 어떻게 방지하나요?
  **[SLF4J, Logback, Log Level, Log Masking, MDC]**

- API 응답 시간, 에러율 등을 모니터링하려면 어떻게 해야 하나요?
  **[Spring Actuator, Micrometer, Prometheus, Grafana]**

### 7.3 배포
- 스키마 변경이 필요할 때 어떻게 관리하나요? 운영 중인 DB에 컬럼을 추가하려면?
  **[Flyway, Liquibase, Schema Migration, Zero-Downtime Migration]**

- 무중단 배포를 하려면 어떤 점을 고려해야 하나요?
  **[Rolling Deployment, Blue-Green, Health Check, Graceful Shutdown]**

---

## 8. Java 및 Spring 기본 지식

### 8.1 Java 문법
- `record`로 DTO를 만들었는데, `class`로 만드는 것과 어떤 차이가 있나요? `record`의 제약사항은 무엇인가요?
  **[Java Record, Immutability, equals/hashCode, Canonical Constructor]**

- `Instant`를 시간 타입으로 선택한 이유는 무엇인가요? `LocalDateTime`과의 차이점은?
  **[Java Time API, Timezone, UTC, ISO-8601]**

### 8.2 Spring 핵심
- `@Component`, `@Service`, `@Repository`의 차이점은 무엇인가요? 기능적 차이가 있나요?
  **[Stereotype Annotations, Component Scan, Exception Translation]**

- 생성자 주입을 사용했는데, 필드 주입(`@Autowired`)과 비교했을 때 장점은 무엇인가요?
  **[Dependency Injection, Constructor Injection, 불변성, 테스트 용이성]**

### 8.3 Spring Security
- `SecurityFilterChain`에서 `.csrf(AbstractHttpConfigurer::disable)`로 CSRF를 비활성화했는데, 왜 그렇게 했나요? 언제 CSRF 보호가 필요한가요?
  **[CSRF, Stateless API, Cookie-based Auth, SameSite]**

- `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`의 의미는 무엇인가요?
  **[Session Management, Stateless Authentication, JSESSIONID]**

---

## 9. 코드 품질 및 리팩토링

### 9.1 코드 리뷰 포인트
- `Todo.java:72`에 `"must not bee null"` 오타가 있는데, 이런 실수를 방지하려면 어떻게 해야 하나요?
  **[Code Review, Static Analysis, SpotBugs, SonarQube]**

- `currentUserId()` 메서드가 `TodoController`와 `AuthController`에 중복되어 있는데, 이를 어떻게 개선할 수 있나요?
  **[DRY Principle, ArgumentResolver, @AuthenticationPrincipal]**

### 9.2 확장성
- 현재 단일 사용자의 Todo만 관리하는데, 팀/프로젝트 단위로 Todo를 공유하려면 어떤 변경이 필요한가요?
  **[Multi-tenancy, 권한 모델, ACL]**

- Todo에 태그, 우선순위, 반복 일정 기능을 추가하려면 엔티티 구조를 어떻게 변경해야 하나요?
  **[Entity 확장, 상속 vs 조합, EAV Pattern]**

---

## 10. 실전 시나리오 질문

### 10.1 장애 대응
- 프로덕션에서 갑자기 500 에러가 급증한다면 어떻게 원인을 파악하고 대응하나요?
  **[Incident Response, Log Analysis, APM, Circuit Breaker]**

- DB 커넥션 풀이 고갈되면 어떤 증상이 나타나고, 어떻게 해결하나요?
  **[Connection Pool, HikariCP, Connection Leak, Timeout 설정]**

### 10.2 성능
- Todo 목록 조회 API가 느려졌다면 어떤 점을 확인하나요?
  **[Query Optimization, Index, Explain Plan, Slow Query Log]**

- 사용자가 10만 개의 Todo를 가지고 있다면 현재 구조에서 어떤 문제가 생기나요?
  **[Pagination, Cursor-based Pagination, 대용량 데이터 처리]**

### 10.3 보안 사고
- JWT secret이 유출되었다면 어떻게 대응해야 하나요?
  **[Key Rotation, Token Invalidation, Incident Handling]**

- SQL Injection, XSS 공격에 현재 코드가 안전한가요? 어떤 부분을 점검해야 하나요?
  **[OWASP Top 10, Prepared Statement, Input Sanitization, CSP]**

---

## 평가 기준 참고

| 영역 | 기본 | 심화 |
|-----|-----|-----|
| 아키텍처 | 레이어 분리 이해 | DDD, 헥사고날 아키텍처 |
| 보안 | JWT 기본 동작 | 토큰 공격 벡터, OWASP |
| JPA | 연관관계 매핑 | 성능 최적화, 락 전략 |
| API | REST 기본 | HTTP 시맨틱, 버저닝 |
| 테스트 | 단위 테스트 작성 | 테스트 전략, TDD |
| 운영 | 설정 분리 | 모니터링, 무중단 배포 |
