# Project Architecture – Spring Boot Todo App

이 문서는 Spring Boot 기반 Todo 애플리케이션의 전체 구조(아키텍처), 도메인 모델, 서비스/컨트롤러 책임, 인증(JWT) 흐름을 한눈에 보기 위해 작성한 문서입니다.  
코드 리뷰/공유/발표 시 이 문서를 기준으로 설명할 수 있습니다.

---

## 1. 프로젝트 개요

### 1.1 목적

- 사용자의 Todo를 관리하는 REST API 서버
- 주요 기능
    - Todo 생성/수정/삭제 (soft delete)
    - 완료 상태 토글
    - 순서(orderIndex) 변경
    - 담당자(assignees) 지정
    - 마감일(dueDate) 설정
    - JWT 기반 인증/인가 (회원가입/로그인/토큰 재발급)

### 1.2 기술 스택

- Spring Boot
- Spring Web (REST API)
- Spring Security + JWT
- JPA (Hibernate)
- Jakarta Validation
- RDBMS (JPA 기반, SEQUENCE 전략 사용)
- (예정) Swagger / springdoc-openapi

---

## 2. 전체 아키텍처 구조

### 2.1 레이어 구조

```text
Client
  ↓
Controller (AuthController, TodoController)
  ↓
Service (AuthService, TodoService)
  ↓
Domain (User, Todo, TodoAssignee)
  ↓
Repository (UserRepository, TodoRepository)
  ↓
Database
```

### 2.2 인증/보안 흐름

```text
Request
  ↓
JwtAuthenticationFilter (JWT 검증, userId 추출)
  ↓
SecurityContextHolder (principal = userId)
  ↓
Controller (currentUserId()로 userId 조회)
  ↓
Service 로직 수행
```

---

## 3. 도메인 모델

### 3.1 User

#### 목적
- 애플리케이션의 사용자(User)를 표현하는 도메인 모델
- Todo의 소유자이자 인증/로그인 주체

#### 필드
- id: Long — PK, SEQUENCE 전략
- name: String — 필수, 최대 200자
- email: String — 필수, 유일 (unique = true)
- password: String — 필수, 암호화된 상태로 저장

#### 규칙
1. name, email, password는 null일 수 없다.
2. email은 시스템 내에서 유일해야 한다.
3. 비밀번호는 평문이 아니라 암호화된 값(BCrypt)으로 저장된다.
4. setter가 없고, 생성 후 값 변경을 제한하는 구조(불변에 가깝게 사용).

#### 연관관계
- User 엔티티 내부에서는 Todo/TodoAssignee와 양방향 매핑을 두지 않는다.
- Todo → User, TodoAssignee → User 방향에서만 참조.

---

### 3.2 Todo

#### 목적
- 한 개의 할 일(Todo)을 표현하는 도메인 모델

#### 주요 필드
- id: Long
- title: String
- content: String
- completed: Boolean
- orderIndex: Integer
- dueDate: LocalDate
- user: User (소유자)
- assignees: Set<TodoAssignee> (담당자 관계)

#### 연관관계
- ManyToOne User (소유자)
- OneToMany TodoAssignee (담당자 관계)

#### 도메인 메서드
- changeTitleAndContent(String title, String content)
- toggleComplete()
- changeOrderIndex(Integer orderIndex)
- changeAssignees(Set<TodoAssignee> assignees)
- changeDueDate(LocalDate dueDate)

#### 규칙
1. title은 비어 있을 수 없다.
2. completed 상태는 toggleComplete()로만 변경한다.
3. orderIndex는 0 이상이며, 실제 순서 무결성은 TodoService.move()에서 관리한다.
4. assignees는 Set으로 관리하며, 중복 담당자를 허용하지 않는다.
5. 삭제는 soft delete 방식으로 처리된다(Repository 쿼리에서 필터링).

---

### TodoAssignee

#### 목적
- Todo와 User 간의 “담당자” 관계를 나타내는 엔티티
- Todo : User = N:N을 풀어내기 위한 조인 테이블

#### 필드
- id: Long
- todo: Todo
- user: User
- createdAt: Instant

#### 연관관계
- ManyToOne Todo (todo_id)
- ManyToOne User (user_id)

#### 규칙
1. todo, user는 null일 수 없다.
2. 하나의 (todo, user) 조합은 한 번만 존재할 수 있다.
   - @UniqueConstraint(name = "uk_todo_assignee_todo_user", columnNames = {"todo_id", "user_id"})
3. createdAt은 생성 시점에 Instant.now()로 설정된다.
4. 생성은 TodoAssignee.builder().todo(todo).user(user).build()를 통해서만 한다.

---

## 4. 서비스 레이어

### 4.1 AuthService

#### 책임
- 회원가입(register)
- 로그인(login)
- 사용자 정보 조회(getUserById)
- refreshToken을 통한 accessToken 재발급(refresh)

#### 협력 객체
- UserRepository
- PasswordEncoder (BCryptPasswordEncoder)
- JwtTokenProvider

#### 공통 규칙
1. 이메일은 시스템 내에서 유일해야 한다.
   - userRepository.existsByEmail(email)로 중복 체크
2. 비밀번호는 항상 암호화된 상태로 저장된다.
   - passwordEncoder.encode(password)
3. 로그인 실패 시 "Invalid email or password."로 에러 메시지를 통일한다.
4. JWT 토큰의 subject에는 userId가 들어간다.

#### 주요 메서드
- AuthResult register(String email, String password, String name)
  - 이메일 중복 체크 → 비밀번호 해시 → User 생성/저장 → access/refresh 토큰 발급 → AuthResult 반환
- AuthResult login(String email, String rawPassword)
  - 이메일로 User 조회 → 비밀번호 검증 → access/refresh 토큰 발급 → AuthResult 반환
- User getUserById(Long userId)
  - ID로 User 조회, 없으면 AuthenticationException
- AuthResult refresh(String refreshToken)
  - refresh 토큰 검증 → userId 추출 → User 조회 → 새 access 토큰 발급 → 기존 refresh 토큰과 함께 AuthResult 반환

---

### 4.2 TodoService

#### 책임
- 특정 사용자(userId)의 Todo 목록 조회
- Todo 생성, 수정, 삭제(soft delete)
- Todo 완료 여부 토글
- Todo 순서(orderIndex) 변경(move)
- Todo 담당자(assignee) 변경
- Todo 마감일(dueDate) 변경

#### 협력 객체
- TodoRepository
- AuthService (userId로 User 조회)
- UserRepository (assignee용 User 조회)

#### 도메인 규칙
1. 소유자 검증
   - 대부분의 메서드가 todoRepository.findByIdAndUserId(todoId, userId)를 사용한다.
   - 항상 요청한 userId가 소유자인 Todo만 조작 가능하도록 한다.
2. soft delete
   - 삭제는 todoRepository.softDelete(todo)로 처리한다.
   - 실제 삭제가 아닌 논리 삭제.
3. orderIndex
   - 생성 시:
     - orderIndex == null이면 countByUserId(userId)로 Todo 개수를 세고 맨 뒤에 추가.
   - 순서 변경:
     - 대상 Todo의 orderIndex를 targetOrderIndex로 변경하고,
     그 사이에 있는 Todo들의 orderIndex를 +1/-1로 재배치.
4. assignees
   - resolveAssignees(List<Long> assigneeIds)
     - null 또는 빈 리스트면 빈 Set
     - userRepository.findAllById(assigneeIds)로 전체 조회
     - 조회된 User 수 != assigneeIds의 고유 개수면 예외 (유효하지 않은 userId 포함 시 실패)
   - 항상 Set<User>로 관리

#### 주요 메서드
- List<Todo> getTodos(Long userId)
  - userId 기준으로 orderIndex 오름차순 조회
- Todo createTodo(Long userId, CreateTodoRequest request)
  - User 조회 → orderIndex 결정 → assignees resolve → Todo 생성/저장
- Todo updateTodo(Long userId, Long todoId, UpdateTodoRequest request)
  - 소유자 검증 → title/content 수정 → 저장
- void deleteTodo(Long userId, Long todoId)
  - 소유자 검증 → soft delete
- void toggleTodoComplete(Long userId, Long todoId)
  - 소유자 검증 → complete 상태 토글 → 저장
- void move(Long userId, Long todoId, MoveTodoRequest request)
  - 소유자 검증 → 현재/목표 orderIndex 비교 → 사이 구간 Todo들 orderIndex 재조정 → 대상 Todo 순서 변경
- Todo updateAssignees(Long userId, Long todoId, UpdateAssigneesRequest request)
  - 소유자 검증 → assignees 전체 교체 → 저장
- Todo updateDueDate(Long userId, Long todoId, UpdateDueDateRequest request)
  - 소유자 검증 → 마감일 변경 → 저장

---

## 5. 컨트롤러 및 API 구조

### 5.1 AuthController(/api/auth)

#### 책임
- 회원가입
- 로그인
- 내 정보 조회
- refreshToken으로 accessToken 재발급

#### 엔드포인트
- POST /api/auth/register
  - 회원가입 + accessToken/refreshToken 발급
  - Body: { "name", "email", "password" }
- POST /api/auth/login
  - 로그인 + accessToken/refreshToken 발급
  - Body: { "email", "password" }
- GET /api/auth/me
  - Authorization 헤더의 accessToken 기반으로 내 정보 조회
  - Body: AuthResponse(id, name, email, null, null)
- POST /api/auth/refresh
  - refreshToken으로 새 accessToken 발급
  - Body: { "refreshToken" }

---

### 5.2 TodoController(/api/todos)

#### 책임
- Todo 목록 조회
- Todo 생성/수정/삭제
- 완료 상태 토글
- 순서 변경(move)
- 담당자/마감일 변경

#### 엔드포인트
- GET /api/todos
  - 현재 사용자(userId)의 Todo 목록 조회 (orderIndex 오름차순)
- POST /api/todos
  - Todo 생성
  - Body: CreateTodoRequest(title, content, dueDate, orderIndex?, assigneeIds?)
- PUT /api/todos/{id}
  - Todo title/content 수정
- DELETE /api/todos/{id}
  - Todo soft delete
- PATCH /api/todos/{id}/toggle
  - completed 상태 토글
- PATCH /api/todos/{id}/move
  - 순서 변경 (MoveTodoRequest.targetOrderIndex)
- PATCH /api/todos/{id}/assignees
  - 담당자 전체 교체
- PATCH /api/todos/{id}/due-date
  - 마감일 변경

---

## 6. 인증/보안 아키텍처

### 6.1 JwtAuthenticationFilter
- OncePerRequestFilter를 상속받아 요청당 한 번 실행.
- Authorization 헤더에서 "Bearer {token}" 형식의 토큰을 읽는다.
- 토큰이 없거나 형식이 다르면 → 그냥 다음 필터로 넘김.
- 토큰이 있으면:
  - jwtTokenProvider.validateToken(token)으로 검증
  - 유효하면 getUserIdFromToken(token)으로 userId 추출
  - UsernamePasswordAuthenticationToken(userId, null, emptyList) 생성
  - SecurityContextHolder.getContext().setAuthentication(authentication)에 저장
- 이후 Controller에서는 SecurityContextHolder에서 userId를 꺼내 현재 사용자 식별.

### 6.2 JwtTokenProvider
- jwt.secret, jwt.access-expiration-seconds, jwt.refresh-expiration-seconds 설정값을 주입받아 사용.
- HMAC-SHA 서명용 SecretKey 생성.
- generateAccessToken(Long userId)
  - subject = userId.toString()
  - 발급/만료 시간 설정
  - HS256으로 서명된 JWT 문자열 반환
- generateRefreshToken(Long userId)
  - 구조는 동일, 만료 시간만 더 길게 설정
- getUserIdFromToken(String token)
  - 토큰 파싱 → Claims → subject(String)를 Long으로 변환하여 반환
- validateToken(String token)
  - 파싱 성공 → true
  - JwtException, IllegalArgumentException 발생 → false

### 6.3 SecurityConfig
- CSRF 비활성화
- 세션 전략: SessionCreationPolicy.STATELESS (세션 미사용)
- 인가 규칙:
  - /api/auth/register, /api/auth/login, /api/auth/refresh → permitAll
  - /api/auth/me, /api/todos/** → authenticated
  - 그 외 → permitAll
- addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
  - JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
- PasswordEncoder로 BCryptPasswordEncoder 사용

---

## 7. 마무리
이 문서는 Spring Boot Todo 프로젝트의 구조와 도메인/서비스/보안 흐름을 요약한 아키텍처 문서입니다.