import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 가족 삭제 실패 원인 → 사용자 문구. */
export function deleteFamilyErrorMessage(error: unknown): { title: string; message: string } {
  if (isNetworkError(error)) {
    return { title: '삭제 실패', message: '서버에 연결할 수 없어요. 네트워크를 확인해주세요.' };
  }
  switch (apiErrorCode(error)) {
    case 'CHILD_HAS_ACTIVE_ASSETS':
      // 자산이 남은 채로 가족이 사라지면 아이가 도토리를 정리할 길이 끊긴다. 서버가 막는다.
      return {
        title: '아직 삭제할 수 없어요',
        message:
          '아이에게 도토리, 예적금, 투자 보유분이나 환전 대기 중인 요청이 남아 있어요.\n' +
          '아이가 모두 정리한 뒤에 가족을 삭제할 수 있어요.',
      };
    case 'FORBIDDEN':
      return { title: '삭제 실패', message: '가족을 만든 부모만 가족을 삭제할 수 있어요.' };
    default:
      return { title: '삭제 실패', message: '잠시 후 다시 시도해주세요.' };
  }
}
