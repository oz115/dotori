import type { ChoreRepeatType } from '@/features/chore/model/types';
import type { Theme } from '@/shared/theme';

export type RepeatBadgeColors = {
  bg: string;
  text: string;
};

/**
 * 반복 주기별 배지 색 (상점의 categoryBadgeColors 와 같은 자리).
 *
 * 넷이 같은 초록이라 목록에서 주기를 구분하려면 글자를 읽어야 했다. 색이 먼저 눈에
 * 들어오면 훑어볼 때 "매일 할 것"과 "한번만 할 것"이 바로 갈린다.
 *
 * <p>기존 semantic 쌍을 그대로 쓴다. 단 danger(빨강)는 뺐다 - 이 기능에서 빨강은 거절·
 * 오류를 뜻하므로, 주기 이름에 쓰면 "매월"이 문제처럼 보인다.
 *
 * <p>ONCE 만 palette 원본에서 옅은 배경을 계산한다. 도토리 브라운은 subtle/text 쌍이
 * 없는 색이어서다 (상점의 TOY 와 같은 이유·같은 값).
 */
export function repeatBadgeColors(theme: Theme, repeatType: ChoreRepeatType): RepeatBadgeColors {
  switch (repeatType) {
    case 'ONCE':
      return { bg: 'rgba(130, 84, 48, 0.12)', text: theme.color.accent.acorn };
    case 'DAY':
      return { bg: theme.color.brand.subtle, text: theme.color.brand.subtleText };
    case 'WEEK':
      return { bg: theme.color.status.info.subtle, text: theme.color.status.info.text };
    case 'MONTH':
      return { bg: theme.color.status.warning.subtle, text: theme.color.status.warning.text };
  }
}
