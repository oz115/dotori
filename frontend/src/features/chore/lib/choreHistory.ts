import type {
  ChoreCompletionHistoryItem,
  ChoreCompletionSummary,
} from '@/features/chore/model/types';

/**
 * 완료 내역 화면의 계산. 서버는 기간 안의 지급 기록만 주므로 "이번 주"는 여기서 가른다.
 */

/**
 * 이번 주가 시작된 순간 (월요일 0시, 기기 로컬 시간).
 *
 * 일요일을 한 주의 시작으로 보는 `getDay()`(0=일)를 그대로 쓰면 일요일에 이번 주가
 * 비어 버린다. 월요일 기준으로 옮겨서 센다.
 */
export function weekStart(now: Date): Date {
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  const daysSinceMonday = (start.getDay() + 6) % 7;
  start.setDate(start.getDate() - daysSinceMonday);
  return start;
}

/** 이번 주에 끝낸 할 일 건수와 받은 도토리 합계 */
export function summarizeWeek(
  completions: ChoreCompletionHistoryItem[],
  now: Date,
): { count: number; total: number } {
  const from = weekStart(now).getTime();

  return completions.reduce(
    (acc, completion) =>
      Date.parse(completion.processedAt) >= from
        ? { count: acc.count + 1, total: acc.total + completion.rewardPoints }
        : acc,
    { count: 0, total: 0 },
  );
}

/** 완료 이력 한 건에 붙는 "그때 보낸 것" */
export type CompletionPhoto = {
  requestId: number;
  photoUrl: string;
  description: string | null;
};

type Completion = { choreId: number; processedAt: string; childId?: number };
type ApprovedRequest = Pick<
  ChoreCompletionSummary,
  'choreCompletionRequestId' | 'choreId' | 'photoUrl' | 'description' | 'requestedAt'
> & { childId?: number };

/** 자녀가 섞인 목록(부모 화면)에서 남의 요청이 붙지 않도록 아이까지 묶어서 센다 */
function groupKey(item: { choreId: number; childId?: number }): string {
  return `${item.childId ?? ''}:${item.choreId}`;
}

/**
 * 완료 이력에 그때 보낸 사진·글을 짝지어 붙인다.
 *
 * 두 목록은 출처가 다르다. 이력은 포인트 원장(지급액·지급 시각)에서, 사진은 완료 요청에서
 * 온다. 둘을 잇는 id 가 없어 같은 미션끼리 시각으로 짝짓는다 - 요청은 승인보다 먼저
 * 일어나므로 "지급 시각보다 앞선 요청 중 가장 최근 것" 이 그 지급의 원인이다.
 *
 * <p>최근 것부터 짝지으며 쓴 요청은 뺀다. 매주 반복하는 미션은 같은 choreId 로 여러 번
 * 지급되는데, 빼지 않으면 가장 최근 사진이 모든 회차에 붙는다.
 *
 * <p>짝을 못 찾으면 null 이다 (30일 밖의 오래된 요청, 사진 없이 승인된 건 등).
 */
export function matchCompletionPhotos<T extends Completion>(
  completions: T[],
  approved: ApprovedRequest[],
): (T & { photo: CompletionPhoto | null })[] {
  const pool = new Map<string, ApprovedRequest[]>();
  for (const request of approved) {
    const key = groupKey(request);
    const list = pool.get(key);
    if (list == null) {
      pool.set(key, [request]);
    } else {
      list.push(request);
    }
  }
  for (const list of pool.values()) {
    list.sort((a, b) => Date.parse(b.requestedAt) - Date.parse(a.requestedAt));
  }

  const photos = new Map<number, CompletionPhoto>();
  const newestFirst = completions
    .map((completion, index) => ({ completion, index }))
    .sort((a, b) => Date.parse(b.completion.processedAt) - Date.parse(a.completion.processedAt));

  for (const { completion, index } of newestFirst) {
    const list = pool.get(groupKey(completion));
    if (list == null) {
      continue;
    }
    const processedAt = Date.parse(completion.processedAt);
    const hit = list.findIndex((request) => Date.parse(request.requestedAt) <= processedAt);
    if (hit < 0) {
      continue;
    }
    const [request] = list.splice(hit, 1);
    photos.set(index, {
      requestId: request.choreCompletionRequestId,
      photoUrl: request.photoUrl,
      description: request.description,
    });
  }

  return completions.map((completion, index) => ({
    ...completion,
    photo: photos.get(index) ?? null,
  }));
}
