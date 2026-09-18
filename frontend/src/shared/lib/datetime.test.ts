import { normalizeFractionalSeconds, parseServerDate } from './datetime';

/**
 * 이 테스트는 V8(Node)에서 돌아서 "Hermes 가 6자리를 거부한다"는 것 자체를 재현할 수 없다.
 * 그래서 파싱 결과가 아니라 **문자열을 3자리로 줄이는지**를 검증한다 — 그게 Hermes 에
 * 넘어가는 실제 입력이고, 여기가 통과하면 실기기에서도 파싱된다.
 */
describe('normalizeFractionalSeconds', () => {
  // 서버가 OffsetDateTime 을 그대로 직렬화해 보내는 실제 형태
  it('마이크로초(6자리)를 3자리로 줄인다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T06:00:57.162979Z')).toBe(
      '2026-10-16T06:00:57.162Z',
    );
  });

  it('오프셋이 붙은 형태도 줄인다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T15:00:57.162979+09:00')).toBe(
      '2026-10-16T15:00:57.162+09:00',
    );
  });

  it('나노초(9자리)도 3자리로 줄인다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T06:00:57.162979123Z')).toBe(
      '2026-10-16T06:00:57.162Z',
    );
  });

  it('이미 3자리면 그대로 둔다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T06:00:57.162Z')).toBe('2026-10-16T06:00:57.162Z');
  });

  it('소수점이 없으면 그대로 둔다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T06:00:57Z')).toBe('2026-10-16T06:00:57Z');
  });

  // 3자리 미만은 명세상 비규격이지만 Hermes 가 받아들이므로 건드리지 않는다.
  it('3자리보다 짧으면 그대로 둔다', () => {
    expect(normalizeFractionalSeconds('2026-10-16T06:00:57.16Z')).toBe('2026-10-16T06:00:57.16Z');
  });
});

describe('parseServerDate', () => {
  it('마이크로초가 붙은 서버 시각을 읽는다', () => {
    const date = parseServerDate('2026-10-16T06:00:57.162979Z');

    expect(date).not.toBeNull();
    expect(date?.toISOString()).toBe('2026-10-16T06:00:57.162Z');
  });

  it('null 은 null 로 통과시킨다 — 만기 없는 상품이 있다', () => {
    expect(parseServerDate(null)).toBeNull();
  });

  it('undefined 도 null 이다 — 응답에 필드가 빠져도 터지지 않는다', () => {
    expect(parseServerDate(undefined)).toBeNull();
  });

  it('읽을 수 없는 문자열은 null 이다', () => {
    expect(parseServerDate('어제')).toBeNull();
  });
});
