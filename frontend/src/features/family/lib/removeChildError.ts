import { apiErrorCode, isNetworkError } from '@/shared/api';

/**
 * 자녀 제외 실패 원인 → 사용자 문구.
 *
 * 가족 삭제(deleteFamilyError)와 막히는 이유가 같다 - 서버가 두 경우 모두 남은 자산을
 * 먼저 본다. 다만 "가족이 사라진다" 와 "이 아이만 나간다" 는 다른 일이라 문구는 따로 쓴다.
 */
export function removeChildErrorMessage(error: unknown): { title: string; message: string } {
  if (isNetworkError(error)) {
    return { title: '제외 실패', message: '서버에 연결할 수 없어요. 네트워크를 확인해주세요.' };
  }
  switch (apiErrorCode(error)) {
    case 'CHILD_HAS_ACTIVE_ASSETS':
      // 자산이 남은 채로 내보내면 아이가 도토리를 정리할 길이 끊긴다. 서버가 막는다.
      return {
        title: '아직 제외할 수 없어요',
        message:
          '이 아이에게 도토리, 예적금, 투자 보유분이나 환전 대기 중인 요청이 남아 있어요.\n' +
          '아이가 모두 정리한 뒤에 제외할 수 있어요.',
      };
    case 'MEMBER_NOT_FOUND':
      return { title: '제외 실패', message: '이미 나간 아이예요. 목록을 새로고침해주세요.' };
    case 'FORBIDDEN':
      return { title: '제외 실패', message: '부모만 아이를 제외할 수 있어요.' };
    default:
      return { title: '제외 실패', message: '잠시 후 다시 시도해주세요.' };
  }
}
