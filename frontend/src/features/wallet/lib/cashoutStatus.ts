/**
 * 환전 요청 상태를 화면 문구·색과 목록 순서로 옮기는 규칙.
 *
 * 상태 이름(SUCCEED·DECLINED…)은 서버 enum 그대로라 부모에게 그대로 보여줄 수 없고,
 * "누가 무엇을 했는지"로 바꿔 말해야 한다 — CANCELLED 는 아이가 스스로 물린 것이고
 * DECLINED 는 부모가 거절한 것이라 부모 화면에서는 전혀 다른 사건이다.
 */

import type { CashoutRequest, CashoutStatus } from '@/features/wallet/model/types';

/** 배지 색. 화면이 theme.color.status 의 같은 이름으로 받아 쓴다 */
export type CashoutStatusTone = 'warning' | 'success' | 'danger' | 'neutral';

// 배지는 '대기 중'이다. 목록의 '승인 대기' 묶음 제목과 같은 말을 쓰면 화면에 같은 문구가
// 두 번 나와 어느 쪽이 이 건의 상태인지 흐려진다.
const LABELS: Record<CashoutStatus, string> = {
  PENDING: '대기 중',
  PROGRESS: '처리 중',
  CANCELLED: '아이가 취소',
  DECLINED: '거절',
  SUCCEED: '송금 완료',
  FAILED: '송금 실패',
};

// 대기 중은 회색으로 물러나 있는다 — 아직 아무 일도 안 일어난 상태라 색으로 재촉할 것이
// 없고, 목록에서 눈에 걸려야 하는 것은 이미 끝나 버린 일 쪽이다.
const TONES: Record<CashoutStatus, CashoutStatusTone> = {
  PENDING: 'neutral',
  PROGRESS: 'warning',
  CANCELLED: 'neutral',
  DECLINED: 'warning',
  SUCCEED: 'success',
  FAILED: 'danger',
};

export function cashoutStatusLabel(status: CashoutStatus): string {
  return LABELS[status];
}

export function cashoutStatusTone(status: CashoutStatus): CashoutStatusTone {
  return TONES[status];
}

/**
 * 끝난 요청이 어떻게 끝났는지 한 줄로. 상세 모달이 상태 배지 아래에 덧붙인다.
 *
 * 부모가 알고 싶은 것은 상태 이름이 아니라 "그래서 돈과 도토리가 어떻게 됐나" 다 —
 * DECLINED 와 FAILED 는 둘 다 도토리가 아이에게 돌아갔다는 점에서 결과가 같고,
 * SUCCEED 만 실제로 계좌가 움직였다.
 */
export function cashoutResultNote(status: CashoutStatus): string | null {
  switch (status) {
    case 'SUCCEED':
      return '아이 계좌로 송금이 끝났어요.';
    case 'FAILED':
      return '송금이 실패해서 도토리를 아이에게 돌려줬어요.';
    case 'DECLINED':
    case 'CANCELLED':
      return '도토리는 아이에게 돌려줬어요.';
    default:
      return null;
  }
}

/** 부모가 지금 처리해야 하는 건지. PROGRESS 는 서버가 쓰지 않지만 온다면 처리 중이라 대기가 아니다 */
export function isPendingCashout(status: CashoutStatus): boolean {
  return status === 'PENDING';
}

/**
 * 대기 중과 지난 내역으로 가른다.
 *
 * 대기 중은 오래 기다린 것이 위로 온다 — 부모는 밀린 것부터 처리한다.
 * 지난 내역은 반대로 최근 것이 위다 — 방금 처리한 결과를 바로 확인하게 된다.
 */
export function splitCashoutRequests<T extends CashoutRequest>(items: T[]) {
  const pending = items
    .filter((item) => isPendingCashout(item.status))
    .sort((a, b) => a.requestedAt.localeCompare(b.requestedAt));

  const history = items
    .filter((item) => !isPendingCashout(item.status))
    .sort((a, b) => (b.processedAt ?? b.requestedAt).localeCompare(a.processedAt ?? a.requestedAt));

  return { pending, history };
}
