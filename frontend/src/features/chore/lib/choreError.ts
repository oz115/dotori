import { apiErrorCode, isNetworkError } from '@/shared/api';

/**
 * 집안일 API 실패 → 사용자 문구.
 *
 * 뭉뚱그려 "잠시 후 다시" 라고만 하면 안 되는 경우가 많다. 보상이 아직 산정되지 않았거나
 * 이미 요청을 보냈거나 이번 주기에 이미 받았으면, 다시 눌러도 결과가 같다 - 무엇을 하면
 * 되는지 알려줘야 한다.
 */

/** 완료 요청 보내기 실패 (아이) */
export function requestCompletionErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'REWARD_NOT_EVALUATED':
      return '아직 도토리가 정해지지 않았어요. 잠시 뒤에 다시 보내주세요.';
    case 'PENDING_REQUEST_EXISTS':
      return '이미 보낸 요청이 있어요. 부모님이 확인하고 계세요.';
    case 'ALREADY_REWARDED':
      return '이번에는 이미 도토리를 받았어요.';
    case 'INVALID_FILE':
      return '사진을 올릴 수 없어요. 다시 찍어볼까요?';
    case 'FILE_UPLOAD_FAILED':
      return '사진 저장에 실패했어요. 잠시 후 다시 시도해주세요.';
    case 'CHORE_NOT_FOUND':
      return '이 할 일을 찾을 수 없어요. 목록을 새로고침해주세요.';
    case 'INVALID_REQUEST':
      // 지워진 할 일이거나 수행 기간이 아닌 경우. 서버가 둘을 같은 코드로 준다.
      return '지금은 완료 요청을 보낼 수 없는 할 일이에요.';
    default:
      return '완료 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 완료 요청 취소 실패 (아이) */
export function cancelCompletionErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'CHORE_COMPLETION_REQUEST_NOT_FOUND':
      return '이미 처리된 요청이에요. 목록을 새로고침해주세요.';
    case 'INVALID_STATUS':
      return '부모님이 이미 확인한 요청이라 취소할 수 없어요.';
    default:
      return '요청을 취소하지 못했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 승인·거절 실패 (부모) */
export function reviewCompletionErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INVALID_STATUS':
      return '이미 처리된 요청이에요. 목록을 새로고침해주세요.';
    case 'CHORE_COMPLETION_REQUEST_NOT_FOUND':
      return '요청을 찾을 수 없어요. 목록을 새로고침해주세요.';
    case 'ALREADY_REWARDED':
      return '이번 주기에 이미 보상을 준 할 일이에요.';
    default:
      return '처리하지 못했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 등록·수정 실패 (부모) */
export function saveChoreErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'CHORE_NOT_FOUND':
      return '이 할 일을 찾을 수 없어요. 이미 지워졌을 수 있어요.';
    case 'INVALID_REQUEST':
      return '입력한 내용을 다시 확인해주세요.';
    default:
      return '저장하지 못했어요. 잠시 후 다시 시도해주세요.';
  }
}
