# API 명세

[← README로 돌아가기](../ReadMe.md)

- 공개 API 99개 (내부 QA용 5개 제외), 모든 경로는 `/api/v1` 프리픽스를 사용합니다.
- 인증은 `Authorization: Bearer <access token>` 헤더를 사용합니다. 회원가입, 로그인, 토큰 재발급, 이메일 인증, 아이디 찾기, 비밀번호 재설정만 인증 없이 호출할 수 있습니다.
- 에러 응답은 `{ code, message, timestamp }` 형식으로 통일합니다. `code`는 `ErrorCode` 상수 이름입니다.

| 도메인 | 대표 엔드포인트 | 설명 |
| --- | --- | --- |
| 인증 | `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh` | 회원가입, 로그인, 토큰 재발급(회전) |
| 가족 | `POST /families`, `POST /families/{familyId}/invitations`, `POST /families/connect` | 가족 생성, 초대 코드 발급, 초대 코드로 연결 |
| 계좌 | `POST /accounts/auth`, `POST /accounts/auth/verify` | SSAFY 금융망 1원 인증으로 계좌 연동 |
| 지갑 | `POST .../wallet/charge`, `POST .../wallet/grant`, `POST .../wallet/cashout-requests` | 포인트 충전, 도토리 주기, 환전 요청 |
| 할 일 | `POST .../chores`, `POST .../chores/{choreId}/completion-requests`, `POST .../completion-requests/{requestId}/approve` | 할 일 등록, 완료 인증 요청, 부모 승인 |
| 예적금 | `POST .../savings/subscriptions`, `POST .../subscriptions/{id}/contributions` | 가입, 적금 납입 |
| 상점 | `POST /families/{familyId}/store/products`, `POST .../store/products/{productId}/purchases` | 상품 등록, 구매 |
| 투자 | `GET /investments/news`, `POST /investments/orders/buy`, `POST /investments/orders/sell` | 오늘의 뉴스, 매수, 매도 |
| 학습 | `GET .../quizzes/today`, `POST .../quizzes/{quizId}/submit`, `GET /words/search` | 오늘의 퀴즈, 제출, 단어 검색 |
| 리포트 | `GET .../reports/income`, `GET .../reports/spending`, `GET .../reports/assets` | 수입, 소비, 자산 리포트 |

> 표의 경로는 `/api/v1`을 생략했고, `...`은 `/families/{familyId}/children/{childId}`를 줄인 것입니다.
