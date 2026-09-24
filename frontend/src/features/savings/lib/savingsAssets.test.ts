import {
  BANNER_LINE_MAX,
  DESCRIPTION_LINE_MAX,
  SAVINGS_BANNER_COPY,
  savingsBannerSegments,
  savingsProductDescription,
} from './savingsAssets';
import type { SavingsProductType } from '@/features/savings/model/types';

const PRODUCT_TYPES: SavingsProductType[] = [
  'ORDINARY',
  'SIMPLE_INTEREST',
  'COMPOUND_INTEREST',
  'SMALL_INSTALLMENT',
  'MID_TERM_INSTALLMENT',
  'LONG_TERM_INSTALLMENT',
];

/**
 * 문구는 줄바꿈을 직접 넣어 끊는다. 그 줄이 화면 폭보다 길면 한 번 더 접히면서 한글이
 * 글자 단위로 끊기고("있어 / 요."), 손으로 끊은 의미가 사라진다. 눈으로만 확인하면 다음
 * 사람이 문구를 고칠 때 다시 깨지므로 글자 수로 잠근다 (상한 근거는 상수 주석 참고).
 */
describe('문구 줄 길이', () => {
  // 넘친 줄을 모아서 비교한다 — 어느 상품의 어느 줄이 문제인지 실패 메시지에 그대로 나온다
  const overflowing = (text: string, max: number) =>
    text
      .split('\n')
      .filter((line) => line.length > max)
      .map((line) => `${line} (${line.length}자)`);

  it('배너 본문은 마스코트 옆 칸에 들어가는 길이다', () => {
    expect(overflowing(SAVINGS_BANNER_COPY.body, BANNER_LINE_MAX)).toEqual([]);
  });

  it('상품 설명은 상세 히어로 폭에 들어가는 길이다', () => {
    const tooLong = PRODUCT_TYPES.flatMap((productType) =>
      overflowing(savingsProductDescription(productType), DESCRIPTION_LINE_MAX).map(
        (line) => `${productType} — ${line}`,
      ),
    );

    expect(tooLong).toEqual([]);
  });

  it('상품 설명 첫 줄은 원래 금융 용어를 알려 준다', () => {
    // 이름을 아이 말로 바꿨으니(도토리 항아리) 용어를 배울 자리는 설명 첫 줄뿐이다
    const terms: Record<SavingsProductType, string> = {
      ORDINARY: '보통예금',
      SIMPLE_INTEREST: '단리예금',
      COMPOUND_INTEREST: '복리예금',
      SMALL_INSTALLMENT: '소액적금',
      MID_TERM_INSTALLMENT: '중기적금',
      LONG_TERM_INSTALLMENT: '장기적금',
    };

    for (const productType of PRODUCT_TYPES) {
      const [firstLine] = savingsProductDescription(productType).split('\n');
      expect(firstLine).toContain(terms[productType]);
    }
  });
});

describe('savingsBannerSegments', () => {
  it('강조 구절만 emphasis 로 쪼갠다', () => {
    const segments = savingsBannerSegments({
      title: '제목',
      body: '한 번에 맡겨서 불리는 예금과,\n차곡차곡 모으는 적금.',
      highlights: ['한 번에 맡겨서', '차곡차곡'],
    });

    expect(segments).toEqual([
      { text: '한 번에 맡겨서', emphasis: true },
      { text: ' 불리는 예금과,\n', emphasis: false },
      { text: '차곡차곡', emphasis: true },
      { text: ' 모으는 적금.', emphasis: false },
    ]);
  });

  it('배열 순서와 무관하게 본문에 나온 순서로 잘라 원문을 그대로 이어붙인다', () => {
    const copy = {
      title: '제목',
      body: '앞 차곡차곡 가운데 한 번에 맡겨서 뒤',
      highlights: ['한 번에 맡겨서', '차곡차곡'],
    };
    const segments = savingsBannerSegments(copy);

    expect(segments.map((segment) => segment.text).join('')).toBe(copy.body);
    expect(segments.filter((segment) => segment.emphasis).map((segment) => segment.text)).toEqual([
      '차곡차곡',
      '한 번에 맡겨서',
    ]);
  });

  it('강조할 구절이 없으면 본문 한 덩어리다', () => {
    expect(savingsBannerSegments({ title: '제목', body: '강조 없음', highlights: [] })).toEqual([
      { text: '강조 없음', emphasis: false },
    ]);
  });

  it('실제 배너 문구도 조각을 이어붙이면 원문이다', () => {
    expect(
      savingsBannerSegments()
        .map((segment) => segment.text)
        .join(''),
    ).toBe(SAVINGS_BANNER_COPY.body);
  });
});
