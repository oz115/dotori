import type {
  ChoreCreateBody,
  ChoreDifficulty,
  ChoreRepeatType,
  ChoreUpdateBody,
} from '@/features/chore/model/types';

/**
 * 할 일 등록·수정 폼의 검증과 변환. 백엔드 ChoreCreateRequest·ChoreUpdateRequest 제약과 맞춘다.
 */

/** 서버 @Size(max = 100) 와 같은 값 */
export const TITLE_MAX = 100;
/** 서버는 255 까지 받지만 시안이 100 이라 좁게 쓴다 (좁히는 쪽은 안전하다) */
export const DESCRIPTION_MAX = 100;

export type ChoreFormInput = {
  /** 경로가 아이를 지목하므로 반드시 골라야 한다 */
  childId: number | null;
  title: string;
  description: string;
  difficulty: ChoreDifficulty;
  repeatType: ChoreRepeatType;
  /** 'YYYY-MM-DD'. 반복이면 쓰지 않는다 */
  endDate: string | null;
};

export type ChoreFormErrors = Partial<
  Record<'childId' | 'title' | 'description' | 'endDate', string>
>;

/** 반복이면 서버가 주기에서 기간을 계산한다. 일회성만 날짜가 필요하다 */
export function isRepeating(repeatType: ChoreRepeatType): boolean {
  return repeatType !== 'ONCE';
}

/**
 * 폼 검증.
 *
 * 일회성의 기간은 서버가 @AssertTrue 로 막지만, 거기까지 가면 아이 화면에 "언제까지인지
 * 아무도 모르는 할 일"을 만들 뻔한 것을 400 으로 알게 된다. 여기서 먼저 잡는다.
 */
export function validateChoreForm(input: ChoreFormInput, today: string): ChoreFormErrors {
  const errors: ChoreFormErrors = {};
  const title = input.title.trim();

  if (input.childId == null) {
    errors.childId = '할 일을 맡을 아이를 선택해주세요.';
  }

  if (title.length === 0) {
    errors.title = '할 일 제목을 입력해주세요.';
  } else if (title.length > TITLE_MAX) {
    errors.title = `제목은 ${TITLE_MAX}자 이내로 입력해주세요.`;
  }

  if (input.description.trim().length > DESCRIPTION_MAX) {
    errors.description = `설명은 ${DESCRIPTION_MAX}자 이내로 입력해주세요.`;
  }

  if (!isRepeating(input.repeatType)) {
    if (input.endDate == null) {
      errors.endDate = '언제까지 할지 정해주세요.';
    } else if (input.endDate < today) {
      // 'YYYY-MM-DD' 는 사전순 비교가 곧 날짜순 비교다.
      errors.endDate = '오늘 이후 날짜를 선택해주세요.';
    }
  }

  return errors;
}

/**
 * 등록 요청 본문.
 *
 * 보상은 담지 않는다 - 서버가 받지 않고 AI 가 등록 직후 산정한다.
 *
 * 일회성의 시작일은 등록하는 날로 둔다. 시안에 시작일 칸이 없는데 서버는 일회성에
 * 시작·종료를 모두 요구하므로, "오늘부터 그날까지"로 읽는 것이 자연스럽다.
 * 반복은 날짜를 아예 보내지 않는다 - 보내도 무시되지만 안 보내는 쪽이 의도가 분명하다.
 */
export function toCreateBody(input: ChoreFormInput, today: string): ChoreCreateBody {
  const base = {
    title: input.title.trim(),
    description: input.description.trim() || null,
    difficulty: input.difficulty,
    repeatType: input.repeatType,
  };

  return isRepeating(input.repeatType)
    ? base
    : { ...base, startDate: today, endDate: input.endDate as string };
}

/**
 * 수정 요청 본문 (PATCH). 바뀐 필드만 담는다.
 *
 * 담당 아이는 넣지 않는다 - 서버가 받지 않는다. 옮기면 이미 쌓인 완료 이력이 누구 것인지
 * 모호해지기 때문이다.
 */
export function toUpdateBody(
  input: ChoreFormInput,
  initial: ChoreFormInput,
  rewardPoints?: number,
): ChoreUpdateBody {
  const body: ChoreUpdateBody = {};
  const title = input.title.trim();
  const description = input.description.trim() || null;
  const initialDescription = initial.description.trim() || null;

  if (title !== initial.title.trim()) body.title = title;
  if (description !== initialDescription) body.description = description;
  if (input.difficulty !== initial.difficulty) body.difficulty = input.difficulty;
  if (input.repeatType !== initial.repeatType) body.repeatType = input.repeatType;
  if (!isRepeating(input.repeatType) && input.endDate !== initial.endDate && input.endDate) {
    body.endDate = input.endDate;
  }
  if (rewardPoints != null) body.rewardPoints = rewardPoints;

  return body;
}
