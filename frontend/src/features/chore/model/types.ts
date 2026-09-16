/**
 * 집안일(할 일) 관련 서버 계약. 경로가 `/families/{familyId}/children/{childId}/chores...`
 * 라서 wallet 과 마찬가지로 familyId·childId 를 함께 넘겨야 한다.
 */

export type ChoreDifficulty = 'EASY' | 'NORMAL' | 'HARD';
export type ChoreRepeatType = 'ONCE' | 'DAY' | 'WEEK' | 'MONTH';
export type ChoreCompletionStatus = 'PENDING' | 'CANCELLED' | 'SUCCEED' | 'REJECTED';

/** GET .../chores 목록의 한 건. 활성(현재 기간에 해당하는) 집안일만 온다 */
export type ChoreSummary = {
  choreId: number;
  childId: number;
  title: string;
  difficulty: ChoreDifficulty;
  /** AI 산정 전이면 null (= 보상 산정 중) */
  rewardPoints: number | null;
  repeatType: ChoreRepeatType;
  startDate: string;
  endDate: string;
  createdAt: string;
};

/** GET .../completion-requests 목록의 한 건 (부모 승인 대기함과 같은 모양) */
export type ChoreCompletionSummary = {
  choreCompletionRequestId: number;
  choreId: number;
  choreTitle: string;
  childName: string;
  photoUrl: string;
  description: string | null;
  status: ChoreCompletionStatus;
  requestedAt: string;
};

/** GET .../completions 목록의 한 건. 승인·지급까지 끝난 것만 온다 */
export type ChoreCompletionHistoryItem = {
  choreId: number;
  choreTitle: string;
  childName: string;
  rewardPoints: number;
  processedAt: string;
};

/**
 * GET .../chores/{choreId} 상세. 목록(ChoreSummary)에 없는 설명·가치·소속·삭제 여부가 온다.
 *
 * deletedAt 이 채워져 있으면 이미 지운 집안일이다. 목록에서는 빠지지만 상세 조회와 완료 요청
 * 처리는 계속되므로 화면이 구분할 수 있어야 한다.
 */
export type ChoreDetail = {
  choreId: number;
  familyId: number;
  parentId: number;
  childId: number;
  title: string;
  description: string | null;
  difficulty: ChoreDifficulty;
  /** AI 가 매긴 가치 가중치. rewardPoints 와 함께 채워진다 */
  value: number | null;
  /** AI 산정 전이면 null (= 보상 산정 중) */
  rewardPoints: number | null;
  repeatType: ChoreRepeatType;
  startDate: string;
  endDate: string;
  createdAt: string;
  deletedAt: string | null;
};

/**
 * POST .../chores 본문.
 *
 * 보상은 보내지 않는다 — 등록 직후 AI 가 산정한다. 부모가 직접 정하면 비현실적인 보상으로
 * 포인트 체계가 무너져서 서버가 아예 필드를 받지 않는다 (상점 상품의 가격과 같은 기준).
 *
 * 기간은 repeatType 에 달렸다. ONCE 면 startDate·endDate 가 **둘 다 필수**고,
 * 반복이면 서버가 주기에서 계산하므로 보내도 무시된다.
 */
export type ChoreCreateBody = {
  title: string;
  description: string | null;
  difficulty: ChoreDifficulty;
  repeatType: ChoreRepeatType;
  startDate?: string;
  endDate?: string;
};

/**
 * PATCH .../chores/{choreId} 본문. 보낸 필드만 바뀐다.
 *
 * 등록과 달리 rewardPoints 를 받는다 — AI 가 산정해 둔 값을 부모가 고칠 수 있다.
 * 담당 아이는 바꿀 수 없어서 childId 가 없다. 담당을 옮기면 이미 쌓인 완료 이력이
 * 누구 것인지 모호해지므로, 바꾸려면 기존 것을 지우고 새로 만든다.
 */
export type ChoreUpdateBody = {
  title?: string;
  description?: string | null;
  difficulty?: ChoreDifficulty;
  rewardPoints?: number;
  repeatType?: ChoreRepeatType;
  startDate?: string;
  endDate?: string;
};

/**
 * POST .../chores/{choreId}/completion-requests 본문.
 *
 * photoKey 는 사진 업로드가 돌려준 저장 키다. 사진은 **필수**다 — 버킷이 비공개라 URL 을
 * 받아도 저장해 둘 수 없어서 서버가 키로 받는다.
 */
export type ChoreCompletionCreateBody = {
  photoKey: string;
  description: string | null;
};

/**
 * 아이가 카메라·앨범에서 고른 사진. multipart 로 올릴 때 RN 의 FormData 가 이 모양을 받는다.
 * expo-image-picker 결과의 uri·fileName·mimeType 을 그대로 옮기면 된다.
 */
export type ChorePhotoFile = {
  uri: string;
  name: string;
  type: string;
};

/** 사진 업로드 결과. 이 키를 그대로 완료 요청 본문에 실어 보낸다 */
export type ChorePhotoUploadResult = { photoKey: string };

/**
 * 완료 요청 생성 결과.
 *
 * 보상 금액이 없다. 지급액은 승인 시점의 보상이라 요청 시점에 알려 주면, 그때까지 부모가
 * 금액을 고쳤을 경우 거짓말이 된다.
 */
export type ChoreCompletionResult = {
  choreCompletionRequestId: number;
  choreId: number;
  status: ChoreCompletionStatus;
  requestedAt: string;
};

/** 승인 결과. rewardPoints 는 이번에 **실제로 지급된** 금액이다 */
export type ChoreApprovalResult = {
  choreCompletionRequestId: number;
  status: ChoreCompletionStatus;
  rewardPoints: number;
  processedAt: string;
};

/** 거절 결과. 사유는 담지 않는다 — 서버가 사유를 저장하지 않는다 */
export type ChoreRejectionResult = {
  choreCompletionRequestId: number;
  status: ChoreCompletionStatus;
  processedAt: string;
};
