import { Image, type ImageSourcePropType } from 'react-native';

import type { CashoutStatus } from '@/features/wallet/model/types';

/**
 * 환전 요청 상태 아이콘. 목록 행과 상세 모달이 같은 그림을 쓴다.
 *
 * 도토리가 아이에게 돌아간 셋(취소·거절·실패)은 같은 X 를 쓴다 — 결과가 같은 일에 서로
 * 다른 기호를 주면 읽는 사람이 차이를 찾게 된다. 무엇이 달랐는지는 옆의 상태 배지가 말한다.
 *
 * 상태는 늘 배지가 글자로 함께 말하므로 이 그림은 스크린리더에서 숨긴다 — 그러지 않으면
 * 같은 내용을 두 번 읽는다.
 */
const STATUS_ICONS: Record<CashoutStatus, ImageSourcePropType> = {
  PENDING: require('@/assets/images/pending.png'),
  PROGRESS: require('@/assets/images/pending.png'),
  CANCELLED: require('@/assets/images/declined.png'),
  DECLINED: require('@/assets/images/declined.png'),
  SUCCEED: require('@/assets/images/check.png'),
  FAILED: require('@/assets/images/declined.png'),
};

type CashoutStatusIconProps = {
  status: CashoutStatus;
  /** 목록 행은 28, 상세의 상태 줄처럼 글자 옆에 붙을 때는 더 작게 */
  size?: number;
};

export function CashoutStatusIcon({ status, size = 28 }: CashoutStatusIconProps) {
  return (
    <Image
      source={STATUS_ICONS[status]}
      style={{ width: size, height: size }}
      resizeMode="contain"
      accessibilityElementsHidden
      importantForAccessibility="no"
    />
  );
}
