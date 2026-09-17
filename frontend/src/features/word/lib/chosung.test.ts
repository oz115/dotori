import { chosungGroupOf, chosungOf } from '@/features/word/lib/chosung';

describe('chosungOf', () => {
  it('완성형 한글의 초성을 찾는다', () => {
    expect(chosungOf('인플레이션')).toBe('ㅇ');
    expect(chosungOf('복리')).toBe('ㅂ');
    expect(chosungOf('환율')).toBe('ㅎ');
  });

  it('한글이 아니면 첫 글자를 그대로 돌려준다', () => {
    expect(chosungOf('ETF')).toBe('E');
  });
});

describe('chosungGroupOf', () => {
  it('겹자음은 기본 자음으로 합친다', () => {
    expect(chosungGroupOf('까치')).toBe('ㄱ');
    expect(chosungGroupOf('쌍둥이')).toBe('ㅅ');
  });

  it('홑자음은 그대로 돌려준다', () => {
    expect(chosungGroupOf('주식')).toBe('ㅈ');
  });
});
