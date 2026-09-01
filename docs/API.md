# API 명세

> 엔드포인트별로 아래 형식을 채웁니다.

## 공통 사항

- Base URL:
- 인증 방식:
- 공통 응답 포맷:
- 공통 에러 포맷:

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
| POST | `/api/v1/families/{familyId}/children/{childId}/wallet/charge` | 포인트 충전 (WLT-01) |

---

## 지갑

### `POST /api/v1/families/{familyId}/children/{childId}/wallet/charge`

- **설명**: 아이가 실제 금액을 부모 계좌로 송금하고 같은 금액(1:1)을 포인트로 받는다
- **인증 필요 여부**: O (아이)
- **기능 ID**: WLT-01 · MVP1

**Request**

`Idempotency-Key` 헤더(선택)를 보내면 같은 요청이 중복 전송돼도 한 번만 반영된다.
네트워크 재시도가 이중 충전으로 이어지지 않으려면 클라이언트가 보내야 한다.

```json
{ "amount": 10000 }
```

**Response (201)**

```json
{
  "pointTransactionId": 100,
  "transactionType": "CHARGE",
  "amount": 10000,
  "balanceAfter": 15000,
  "createdAt": "2026-08-28T12:00:00+09:00"
}
```

**Error Cases**

| 상태 코드 | 코드 | 사유 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | amount 누락 또는 0 이하 |
| 400 | `INSUFFICIENT_ACCOUNT_BALANCE` | 아이의 실제 계좌 잔액 부족 |
| 404 | `ACCOUNT_NOT_LINKED` | 아이 또는 부모의 연동 계좌 없음 |
| 404 | `WALLET_NOT_FOUND` | 포인트 지갑 미생성 |
| 409 | `POINT_BALANCE_EXCEEDED` | 보유 가능한 최대 포인트 초과 |
| 502 | `BANK_TRANSFER_FAILED` | 송금 실패 |

> 송금은 계좌 연동(ACC-01·02)이 완료될 때까지 목업으로 동작한다.
> 목업도 계좌 API 명세와 같은 형식·같은 에러 코드를 사용하므로, 실제 연동으로 교체할 때
> 충전 API 의 계약은 바뀌지 않는다.
