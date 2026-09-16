import {
  cancelCompletionErrorMessage,
  requestCompletionErrorMessage,
  reviewCompletionErrorMessage,
  saveChoreErrorMessage,
} from './choreError';

function apiError(code: string) {
  return { response: { data: { code } } };
}

describe('requestCompletionErrorMessage', () => {
  it('보상이 아직 없으면 기다리라고 알린다', () => {
    // 다시 눌러도 결과가 같은 경우라 "잠시 후 다시"로 뭉뚱그리면 안 된다.
    expect(requestCompletionErrorMessage(apiError('REWARD_NOT_EVALUATED'))).toContain(
      '도토리가 정해지지 않았어요',
    );
  });

  it('이미 보낸 요청이 있으면 그 사실을 알린다', () => {
    expect(requestCompletionErrorMessage(apiError('PENDING_REQUEST_EXISTS'))).toContain(
      '이미 보낸 요청',
    );
  });

  it('이번 주기에 이미 받았으면 그 사실을 알린다', () => {
    expect(requestCompletionErrorMessage(apiError('ALREADY_REWARDED'))).toContain('이미 도토리를');
  });

  it('사진 문제는 다시 찍도록 안내한다', () => {
    expect(requestCompletionErrorMessage(apiError('INVALID_FILE'))).toContain('다시 찍어');
  });

  it('모르는 코드와 네트워크 오류는 기본 문구로 떨어진다', () => {
    expect(requestCompletionErrorMessage(apiError('SOMETHING_ELSE'))).toContain(
      '완료 요청을 보내지 못했어요',
    );
    expect(requestCompletionErrorMessage({})).toContain('서버에 연결할 수 없어요');
  });
});

describe('cancelCompletionErrorMessage', () => {
  it('부모가 이미 확인했으면 취소할 수 없다고 알린다', () => {
    expect(cancelCompletionErrorMessage(apiError('INVALID_STATUS'))).toContain('취소할 수 없어요');
  });

  it('기본 문구로 떨어진다', () => {
    expect(cancelCompletionErrorMessage(apiError('SOMETHING_ELSE'))).toContain('취소하지 못했어요');
  });
});

describe('reviewCompletionErrorMessage', () => {
  it('이미 처리된 요청을 구분해 알린다', () => {
    // 부모 둘이 동시에 처리하면 나는 경우다.
    expect(reviewCompletionErrorMessage(apiError('INVALID_STATUS'))).toContain('이미 처리된 요청');
  });

  it('기본 문구로 떨어진다', () => {
    expect(reviewCompletionErrorMessage(apiError('SOMETHING_ELSE'))).toContain('처리하지 못했어요');
  });
});

describe('saveChoreErrorMessage', () => {
  it('지워진 할 일을 구분해 알린다', () => {
    expect(saveChoreErrorMessage(apiError('CHORE_NOT_FOUND'))).toContain('지워졌을 수 있어요');
  });

  it('기본 문구로 떨어진다', () => {
    expect(saveChoreErrorMessage(apiError('SOMETHING_ELSE'))).toContain('저장하지 못했어요');
  });
});
