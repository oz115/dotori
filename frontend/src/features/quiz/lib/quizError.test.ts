import { parentQuizFailure, parentQuizFailureMessage } from './quizError';

const apiError = (code: string, status: number) => ({
  response: { status, data: { code, message: '' } },
});

describe('parentQuizFailure', () => {
  it('QUIZ_NOT_READY 는 오류가 아니라 준비 전 상태로 읽는다', () => {
    const failure = parentQuizFailure(apiError('QUIZ_NOT_READY', 404));
    expect(failure).toBe('NOT_READY');
    expect(parentQuizFailureMessage(failure)).toBe('오늘의 퀴즈가 아직 준비되지 않았어요');
  });

  it('FORBIDDEN 은 권한 없음이다', () => {
    expect(parentQuizFailure(apiError('FORBIDDEN', 403))).toBe('FORBIDDEN');
  });

  it('응답이 없으면 네트워크 오류다', () => {
    expect(parentQuizFailure(new Error('Network Error'))).toBe('NETWORK');
  });

  it('그 밖의 서버 오류는 일반 오류다', () => {
    expect(parentQuizFailure(apiError('INTERNAL_SERVER_ERROR', 500))).toBe('ERROR');
  });
});
