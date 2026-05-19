# 수강 신청 시스템

## 프로젝트 개요

크리에이터가 강의를 개설하고, 클래스메이트가 원하는 강의에 수강 신청할 수 있는 Spring Boot 기반 백엔드 API입니다.

강의는 `DRAFT`, `OPEN`, `CLOSED` 상태를 가지며, 수강 신청은 대기열 진입 후 `READY` 상태가 된 사용자만 진행할 수 있습니다. 실제 신청은 `PENDING` 상태로 생성되고, 결제 확정 시 `CONFIRMED`, 취소 시 `CANCELLED`로 변경됩니다.

상세 API 명세와 데이터 모델 설명은 README에도 포함되어 있으며, 별도 정리본은 [Notion 상세 문서](https://www.notion.so/36591db934b08013827cd3e8a8389e2a?source=copy_link)에서도 확인할 수 있습니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Jakarta Validation
- MySQL 8
- H2 Database: 테스트 프로필 전용
- JUnit 5
- Docker Compose

## 실행 방법

### 1. MySQL 실행

```bash
docker compose -p course-enrollment up -d
```

Docker MySQL 접속 정보:

```text
Host: localhost
Port: 3307
Database: course_enrollment
Username: root
Password: password
```

컨테이너 확인:

```bash
docker ps
```

`course-enrollment-mysql` 컨테이너가 보이면 정상입니다.

### 2. 애플리케이션 실행

```bash
mvn spring-boot:run
```

또는 IntelliJ에서 `CourseEnrollmentApplication`을 실행합니다.

기본 서버 주소:

```text
http://localhost:8080
```

MySQL 정보를 바꾸고 싶다면 환경변수를 사용할 수 있습니다.

```bash
MYSQL_URL="jdbc:mysql://localhost:3307/course_enrollment?serverTimezone=Asia/Seoul&characterEncoding=UTF-8" \
MYSQL_USERNAME=root \
MYSQL_PASSWORD=password \
mvn spring-boot:run
```

## 요구사항 해석 및 가정

- 인증/인가는 과제 조건에 따라 간략화했습니다.
- 강사 식별은 `X-CREATOR-ID` 헤더를 사용합니다.
- 수강생 식별은 `X-USER-ID` 헤더를 사용합니다.
- Java의 `Class` 타입과 이름 충돌을 피하기 위해 내부 도메인명은 `Course`를 사용했습니다.
- 외부 결제 시스템 연동은 구현하지 않고, 결제 확정 API 호출로 상태만 변경합니다.
- 정원에 포함되는 신청 상태는 `PENDING`, `CONFIRMED`입니다.
- `CANCELLED` 신청은 정원 계산에서 제외합니다.
- 대기열의 `READY` 상태는 10분 동안 유효합니다.
- `READY` 시간이 만료되면 `EXPIRED` 처리됩니다.

## 설계 결정과 이유

### Course와 Enrollment 분리

강의 정보와 수강 신청 정보를 별도 엔티티로 분리했습니다.

- `Course`: 강의 제목, 설명, 가격, 정원, 수강 기간, 강의 상태 관리
- `Enrollment`: 사용자별 실제 수강 신청 상태 관리

### Waiting Room 방식 대기열

대기열은 단순히 정원 초과자를 저장하는 방식이 아니라, 티켓팅 서비스의 waiting room 방식으로 구현했습니다.

사용자는 실제 수강 신청 전에 대기열에 진입합니다. 남은 정원만큼 `READY` 상태를 받고, 초과 인원은 `WAITING` 상태가 됩니다.

예시:

```text
정원 100명, 현재 신청 0명, 접속 10명
→ 10명 모두 READY

정원 100명, 현재 신청 95명, 접속 10명
→ 5명 READY
→ 5명 WAITING

정원 100명, 현재 신청 100명, 접속 10명
→ 10명 WAITING
```

### 동시성 처리

마지막 자리에 여러 사용자가 동시에 접근하는 상황을 고려해 강의 조회 시 `PESSIMISTIC_WRITE` 잠금을 사용했습니다.

대기열 진입, 실제 수강 신청, 취소 후 대기자 승격 과정에서 같은 강의 row를 잠그기 때문에 정원 초과와 중복 승격을 방지할 수 있습니다.

### 대기 순번 계산

대기 순번은 고정 컬럼으로 저장하지 않고 조회 시 계산합니다.

```text
position = 나보다 먼저 대기열에 들어온 WAITING 사용자 수 + 1
```

구현에서는 자동 증가 `id`를 진입 순서로 사용합니다. 이 방식은 앞사람이 취소되거나 `READY`로 승격될 때 순번이 자연스럽게 줄어드는 장점이 있습니다.

## 미구현 / 제약사항

- 실제 로그인, JWT, 세션 기반 인증은 구현하지 않았습니다.
- 실제 결제 시스템 연동은 구현하지 않았습니다.
- 대기열 실시간 알림, WebSocket, SSE는 구현하지 않았습니다.
- `READY` 만료 처리는 스케줄러가 아니라 대기열 조회/진입/신청 흐름에서 지연 처리합니다.
- 운영용 DB 마이그레이션 도구인 Flyway, Liquibase는 사용하지 않았습니다.
- `ddl-auto: update`를 사용해 개발 환경에서 테이블을 자동 생성합니다.

## AI 활용 범위

ChatGPT Codex를 사용해 다음 작업을 보조받았습니다.

- Spring Boot 프로젝트 구조 설계
- JPA 엔티티, Repository, Service, Controller 작성
- MySQL 및 Docker Compose 실행 설정
- 대기열 waiting room 방식 설계
- 테스트 코드 작성
- README 문서 작성

## API 목록 및 예시

### 강의 등록

```http
POST /api/classes
X-CREATOR-ID: 1
Content-Type: application/json
```

```json
{
  "title": "Spring Boot 입문",
  "description": "Spring Boot와 JPA로 API를 만들어봅니다.",
  "price": 100000,
  "capacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-07-01"
}
```

응답 예시:

```json
{
  "id": 1,
  "creatorId": 1,
  "title": "Spring Boot 입문",
  "description": "Spring Boot와 JPA로 API를 만들어봅니다.",
  "price": 100000,
  "capacity": 30,
  "enrolledCount": 0,
  "startDate": "2026-06-01",
  "endDate": "2026-07-01",
  "status": "DRAFT",
  "createdAt": "2026-05-19T12:00:00"
}
```

### 강의 상태 변경

```http
PATCH /api/classes/1/status
X-CREATOR-ID: 1
Content-Type: application/json
```

```json
{
  "status": "OPEN"
}
```

### 강의 목록 조회

```http
GET /api/classes
```

상태 필터:

```http
GET /api/classes?status=OPEN
```

### 강의 상세 조회

```http
GET /api/classes/1
```

응답에는 현재 신청 인원인 `enrolledCount`가 포함됩니다.

### 대기열 진입

```http
POST /api/classes/1/queue
X-USER-ID: 10
```

남은 정원이 있으면 `READY` 상태를 반환합니다.

```json
{
  "id": 1,
  "courseId": 1,
  "userId": 10,
  "status": "READY",
  "position": 0,
  "readyAt": "2026-05-19T12:00:00",
  "expiresAt": "2026-05-19T12:10:00",
  "createdAt": "2026-05-19T12:00:00"
}
```

남은 정원이 없으면 `WAITING` 상태를 반환합니다.

```json
{
  "id": 2,
  "courseId": 1,
  "userId": 11,
  "status": "WAITING",
  "position": 1,
  "readyAt": null,
  "expiresAt": null,
  "createdAt": "2026-05-19T12:00:10"
}
```

### 내 대기열 상태 조회

```http
GET /api/classes/1/queue/me
X-USER-ID: 10
```

`READY` 상태의 `position`은 `0`입니다.

### 대기열 취소

```http
PATCH /api/classes/1/queue/cancel
X-USER-ID: 10
```

대기열 취소로 신청 가능 자리가 생기면 오래 기다린 `WAITING` 사용자가 먼저 `READY`로 승격됩니다.

### 수강 신청

```http
POST /api/enrollments
X-USER-ID: 10
Content-Type: application/json
```

```json
{
  "courseId": 1
}
```

`READY` 상태인 사용자만 신청할 수 있습니다. 성공 시 신청 상태는 `PENDING`으로 생성되고, 해당 대기열은 `COMPLETED`가 됩니다.

### 결제 확정

```http
PATCH /api/enrollments/1/confirm-payment
X-USER-ID: 10
```

`PENDING` 상태를 `CONFIRMED`로 변경합니다.

### 수강 취소

```http
PATCH /api/enrollments/1/cancel
X-USER-ID: 10
```

결제 확정 후 7일 이내에만 취소할 수 있습니다. 취소로 자리가 생기면 다음 대기자가 `READY`로 승격됩니다.

### 내 수강 신청 목록 조회

```http
GET /api/enrollments/me?page=0&size=20
X-USER-ID: 10
```

Spring Data `Page` 형식으로 응답합니다.

### 강의별 수강생 목록 조회

```http
GET /api/classes/1/enrollments
X-CREATOR-ID: 1
```

강의를 만든 크리에이터만 조회할 수 있습니다.

## 데이터 모델 설명

### ERD 요약

```text
courses 1 ─── N enrollments
courses 1 ─── N enrollment_queues
```

### courses

| 컬럼 | 설명 |
| --- | --- |
| id | 강의 ID |
| creator_id | 크리에이터 ID |
| title | 강의 제목 |
| description | 강의 설명 |
| price | 가격 |
| capacity | 최대 수강 인원 |
| start_date | 수강 시작일 |
| end_date | 수강 종료일 |
| status | `DRAFT`, `OPEN`, `CLOSED` |
| created_at | 생성 시각 |
| version | JPA 버전 컬럼 |

### enrollments

| 컬럼 | 설명 |
| --- | --- |
| id | 수강 신청 ID |
| course_id | 강의 ID |
| user_id | 수강생 ID |
| status | `PENDING`, `CONFIRMED`, `CANCELLED` |
| created_at | 신청 시각 |
| confirmed_at | 결제 확정 시각 |
| cancelled_at | 취소 시각 |

### enrollment_queues

| 컬럼 | 설명 |
| --- | --- |
| id | 대기열 ID |
| course_id | 강의 ID |
| user_id | 사용자 ID |
| status | `WAITING`, `READY`, `COMPLETED`, `EXPIRED`, `CANCELLED` |
| ready_at | 신청 가능 상태가 된 시각 |
| expires_at | 신청 가능 상태 만료 시각 |
| completed_at | 수강 신청 완료 시각 |
| cancelled_at | 대기열 취소 시각 |
| created_at | 대기열 진입 시각 |

## 테스트 실행 방법

```bash
mvn test
```

테스트는 `test` 프로필과 H2 인메모리 DB를 사용합니다.

현재 포함된 주요 테스트:

- `DRAFT` 강의 신청 불가
- `READY` 대기열 없이 수강 신청 불가
- 남은 정원만큼만 `READY` 발급
- 수강 신청 후 결제 확정
- 결제 확정 후 7일 초과 취소 불가
- 수강 취소 시 다음 대기자 `READY` 승격
