import { EMAIL_CODE_LENGTH, isCompleteEmailCode, normalizeEmailCode } from './emailCode';

describe('normalizeEmailCode', () => {
  it('소문자를 대문자로 바꾼다', () => {
    expect(normalizeEmailCode('ab3k')).toBe('AB3K');
  });

  it('영문·숫자 외의 문자(공백·기호·한글)는 걷어낸다', () => {
    expect(normalizeEmailCode(' A-B 3가K ')).toBe('AB3K');
  });

  it('최대 길이(6자)에서 자른다', () => {
    expect(normalizeEmailCode('ABCDEFGH')).toHaveLength(EMAIL_CODE_LENGTH);
    expect(normalizeEmailCode('ABCDEFGH')).toBe('ABCDEF');
  });
});

describe('isCompleteEmailCode', () => {
  it('6자가 모두 채워졌을 때만 true', () => {
    expect(isCompleteEmailCode('ABC23')).toBe(false);
    expect(isCompleteEmailCode('ABC234')).toBe(true);
  });
});
