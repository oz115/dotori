# API 명세

> 엔드포인트별로 아래 형식을 채웁니다.

## 공통 사항

- Base URL: `/api/v1` (전체 경로에 포함)
- 인증 방식: `Authorization: Bearer {accessToken}` (인증 필요 API에 한함)
- 공통 응답 포맷: `application/json`, 날짜/시간은 ISO 8601 (`YYYY-MM-DDTHH:mm:ss+09:00`)
- 공통 에러 포맷:

```json
{
  "code": "DUPLICATE_LOGIN_ID",
  "message": "이미 사용 중인 아이디입니다",
  "timestamp": "2026-09-01T10:45:11+09:00"
}
```

## 템플릿

### `METHOD /path`

- **설명**:
- **인증 필요 여부**:

**Request**

```json
{}
```

**Response (200)**

```json
{}
```

**Error Cases**

| 상태 코드 | 사유 |
| --- | --- |
|  |  |

---

## 엔드포인트 목록

| Method | Path | 설명 |
| --- | --- | --- |
| POST | /api/v1/auth/signup | 회원가입 (인증 불필요) |

---

### `POST /api/v1/auth/signup`

- **설명**: 이메일 계정으로 신규 사용자를 등록한다 (AUTH-01)
- **인증 필요 여부**: X

**Request**

```json
{
  "loginId": "dotori123",
  "password": "P@ssw0rd!",
  "name": "김도토",
  "email": "dotori@example.com",
  "role": "PARENT"
}
```

**Response (201)**

```json
{
  "userId": 1,
  "loginId": "dotori123",
  "name": "김도토",
  "email": "dotori@example.com",
  "role": "PARENT",
  "createdAt": "2026-09-01T10:45:11+09:00"
}
```

**Error Cases**

| 상태 코드 | 사유 |
| --- | --- |
| 400 | INVALID_REQUEST — 필수 값 누락, 형식 오류 |
| 409 | DUPLICATE_LOGIN_ID — 이미 사용 중인 아이디 |
| 409 | DUPLICATE_EMAIL — 이미 사용 중인 이메일 |
