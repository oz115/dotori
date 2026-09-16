import type { NotificationType } from '@/features/notification/model/types';

/**
 * 알림 타입별 한글 라벨. 화면에 타입 코드를 그대로 노출하지 않기 위한 용도(디버그/접근성
 * 라벨 등) — 목록 카드 자체는 title/body를 그대로 보여줘서 이 라벨을 직접 쓰진 않는다.
 */
const NOTIFICATION_TYPE_LABELS: Record<NotificationType, string> = {
  INVITATION: '가족 초대',
  PURCHASE: '상점',
  CHARGE: '포인트 충전',
  CASHOUT: '환전',
  CHORE: '할 일',
  SAVINGS: '예적금',
  INVESTMENT: '투자',
};

/**
 * 알림 타입별 대표 이미지. 지금은 색만 채운 placeholder이니, 실제 일러스트가
 * assets/images/notification/ 에 같은 파일명({type 소문자}.png)으로 들어오면
 * 이 매핑은 코드 변경 없이 자동으로 교체된다.
 */
const NOTIFICATION_TYPE_IMAGES: Record<NotificationType, number> = {
  INVITATION: require('@/assets/images/notification/invitation.png'),
  PURCHASE: require('@/assets/images/notification/purchase.png'),
  CHARGE: require('@/assets/images/notification/charge.png'),
  CASHOUT: require('@/assets/images/notification/cashout.png'),
  CHORE: require('@/assets/images/notification/chore.png'),
  SAVINGS: require('@/assets/images/notification/savings.png'),
  INVESTMENT: require('@/assets/images/notification/investment.png'),
};

export function notificationTypeLabel(type: NotificationType): string {
  return NOTIFICATION_TYPE_LABELS[type];
}

export function notificationTypeImage(type: NotificationType) {
  return NOTIFICATION_TYPE_IMAGES[type];
}
