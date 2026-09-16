import type { ChoreDifficulty, ChoreRepeatType } from '@/features/chore/model/types';

/**
 * 집안일 열거형을 화면 문구로 옮긴다. 목록·상세·등록·수정이 같은 말을 쓰도록 한곳에 모았다
 * (서버 값은 영어 상수라 화면에 그대로 내보낼 수 없다).
 */

const REPEAT_LABEL: Record<ChoreRepeatType, string> = {
  ONCE: '한번',
  DAY: '매일',
  WEEK: '매주',
  MONTH: '매월',
};

const DIFFICULTY_LABEL: Record<ChoreDifficulty, string> = {
  EASY: '쉬움',
  NORMAL: '보통',
  HARD: '어려움',
};

export function repeatLabel(repeatType: ChoreRepeatType): string {
  return REPEAT_LABEL[repeatType];
}

export function difficultyLabel(difficulty: ChoreDifficulty): string {
  return DIFFICULTY_LABEL[difficulty];
}

/** 필터·선택지 순서. '전체'는 칩 컴포넌트가 앞에 붙인다 */
export const REPEAT_TYPES: ChoreRepeatType[] = ['ONCE', 'DAY', 'WEEK', 'MONTH'];
export const DIFFICULTIES: ChoreDifficulty[] = ['EASY', 'NORMAL', 'HARD'];

/**
 * 보상 산정이 끝났는지. rewardPoints 가 null 이면 AI 가 아직 값을 못 채운 것이다
 * (상점의 isPriceReady 와 같은 자리).
 */
export function isRewardReady(rewardPoints: number | null): rewardPoints is number {
  return rewardPoints != null;
}

/**
 * 남은 할 일을 다 하면 받는 보상 합계.
 *
 * 산정 중인 것(null)은 더할 수 없어 빼고 센다. 뺀 건수를 함께 돌려주는 이유는, 합계만
 * 주면 화면이 실제보다 적은 금액을 확정처럼 보여 주기 때문이다.
 */
export function sumRewards(rewards: (number | null)[]): { total: number; pendingCount: number } {
  return rewards.reduce(
    (acc, reward) =>
      isRewardReady(reward)
        ? { total: acc.total + reward, pendingCount: acc.pendingCount }
        : { total: acc.total, pendingCount: acc.pendingCount + 1 },
    { total: 0, pendingCount: 0 },
  );
}
