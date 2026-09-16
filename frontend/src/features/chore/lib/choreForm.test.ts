import {
  isRepeating,
  toCreateBody,
  toUpdateBody,
  validateChoreForm,
  type ChoreFormInput,
} from './choreForm';

const TODAY = '2026-09-16';

const form = (overrides: Partial<ChoreFormInput> = {}): ChoreFormInput => ({
  childId: 2,
  title: '설거지 하기',
  description: '',
  difficulty: 'NORMAL',
  repeatType: 'ONCE',
  endDate: '2026-09-20',
  ...overrides,
});

describe('isRepeating', () => {
  it('ONCE 만 반복이 아니다', () => {
    expect(isRepeating('ONCE')).toBe(false);
    expect(isRepeating('DAY')).toBe(true);
    expect(isRepeating('MONTH')).toBe(true);
  });
});

describe('validateChoreForm', () => {
  it('제대로 채웠으면 오류가 없다', () => {
    expect(validateChoreForm(form(), TODAY)).toEqual({});
  });

  it('아이를 안 골랐으면 막는다', () => {
    // 경로가 아이를 지목하므로 고르지 않으면 요청 자체를 보낼 수 없다.
    expect(validateChoreForm(form({ childId: null }), TODAY).childId).toBeDefined();
  });

  it('제목이 공백뿐이면 막는다', () => {
    expect(validateChoreForm(form({ title: '   ' }), TODAY).title).toBeDefined();
  });

  it('제목이 100자를 넘으면 막는다', () => {
    expect(validateChoreForm(form({ title: 'ㄱ'.repeat(101) }), TODAY).title).toBeDefined();
  });

  it('설명이 100자를 넘으면 막는다', () => {
    expect(
      validateChoreForm(form({ description: 'ㄱ'.repeat(101) }), TODAY).description,
    ).toBeDefined();
  });

  it('일회성인데 날짜가 없으면 막는다', () => {
    expect(validateChoreForm(form({ endDate: null }), TODAY).endDate).toBeDefined();
  });

  it('일회성인데 지난 날짜면 막는다', () => {
    expect(validateChoreForm(form({ endDate: '2026-09-15' }), TODAY).endDate).toBeDefined();
  });

  it('오늘까지는 괜찮다', () => {
    expect(validateChoreForm(form({ endDate: TODAY }), TODAY).endDate).toBeUndefined();
  });

  it('반복이면 날짜를 묻지 않는다', () => {
    // 서버가 주기에서 계산하므로 날짜가 없어도 된다.
    expect(validateChoreForm(form({ repeatType: 'WEEK', endDate: null }), TODAY)).toEqual({});
  });
});

describe('toCreateBody', () => {
  it('보상을 담지 않는다', () => {
    // 서버가 받지 않는 필드다 - AI 가 등록 직후 산정한다.
    expect(toCreateBody(form(), TODAY)).not.toHaveProperty('rewardPoints');
  });

  it('일회성은 시작일을 오늘로 채운다', () => {
    expect(toCreateBody(form(), TODAY)).toEqual({
      title: '설거지 하기',
      description: null,
      difficulty: 'NORMAL',
      repeatType: 'ONCE',
      startDate: TODAY,
      endDate: '2026-09-20',
    });
  });

  it('반복은 날짜를 아예 보내지 않는다', () => {
    const body = toCreateBody(form({ repeatType: 'DAY' }), TODAY);

    expect(body).not.toHaveProperty('startDate');
    expect(body).not.toHaveProperty('endDate');
  });

  it('설명이 비면 null 이다', () => {
    expect(toCreateBody(form({ description: '  ' }), TODAY).description).toBeNull();
  });
});

describe('toUpdateBody', () => {
  it('안 바꾼 필드는 담지 않는다', () => {
    expect(toUpdateBody(form(), form())).toEqual({});
  });

  it('바꾼 필드만 담는다', () => {
    expect(toUpdateBody(form({ title: '방 청소' }), form())).toEqual({ title: '방 청소' });
  });

  it('보상은 따로 넘길 때만 담는다', () => {
    expect(toUpdateBody(form(), form(), 300)).toEqual({ rewardPoints: 300 });
  });

  it('담당 아이는 절대 담지 않는다', () => {
    // 서버가 받지 않는다. 담당을 옮기면 쌓인 완료 이력이 누구 것인지 모호해진다.
    expect(toUpdateBody(form({ childId: 99 }), form())).not.toHaveProperty('childId');
  });
});
