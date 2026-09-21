import {
  REWARD_MAX,
  isRepeating,
  toCreateBody,
  toUpdateBody,
  validateChoreForm,
  validateReward,
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
    expect(toUpdateBody(form(), form(), TODAY)).toEqual({});
  });

  it('바꾼 필드만 담는다', () => {
    expect(toUpdateBody(form({ title: '방 청소' }), form(), TODAY)).toEqual({ title: '방 청소' });
  });

  it('보상은 따로 넘길 때만 담는다', () => {
    expect(toUpdateBody(form(), form(), TODAY, 300)).toEqual({ rewardPoints: 300 });
  });

  it('담당 아이는 절대 담지 않는다', () => {
    // 서버가 받지 않는다. 담당을 옮기면 쌓인 완료 이력이 누구 것인지 모호해진다.
    expect(toUpdateBody(form({ childId: 99 }), form(), TODAY)).not.toHaveProperty('childId');
  });

  describe('반복 → 일회성', () => {
    const repeating = form({ repeatType: 'DAY' });

    it('날짜를 건드리지 않아도 시작일·종료일을 함께 보낸다', () => {
      // 서버는 일회성으로 바뀔 때 기간을 추론하지 않고, 둘 중 하나라도 비면 400 이다.
      // "바뀐 필드만" 규칙을 그대로 따르면 수정이 항상 실패했다.
      expect(toUpdateBody(form({ repeatType: 'ONCE' }), repeating, TODAY)).toEqual({
        repeatType: 'ONCE',
        startDate: TODAY,
        endDate: '2026-09-20',
      });
    });

    it('날짜까지 바꿨으면 바꾼 날짜로 보낸다', () => {
      expect(
        toUpdateBody(form({ repeatType: 'ONCE', endDate: '2026-09-30' }), repeating, TODAY),
      ).toEqual({
        repeatType: 'ONCE',
        startDate: TODAY,
        endDate: '2026-09-30',
      });
    });
  });

  describe('일회성 → 반복', () => {
    it('기간을 보내지 않는다 - 서버가 오늘 기준으로 직접 계산한다', () => {
      expect(toUpdateBody(form({ repeatType: 'WEEK' }), form(), TODAY)).toEqual({
        repeatType: 'WEEK',
      });
    });
  });

  describe('일회성 유지', () => {
    it('종료일을 바꿨을 때만 담고, 시작일은 건드리지 않는다', () => {
      expect(toUpdateBody(form({ endDate: '2026-09-25' }), form(), TODAY)).toEqual({
        endDate: '2026-09-25',
      });
    });
  });
});

describe('validateReward', () => {
  it('빈 칸은 "안 바꿈" 이라 통과시킨다', () => {
    // 수정은 PATCH 라 빈 칸이 곧 생략이다.
    expect(validateReward('')).toBeUndefined();
    expect(validateReward('   ')).toBeUndefined();
  });

  it('1 이상의 정수만 받는다', () => {
    expect(validateReward('1')).toBeUndefined();
    expect(validateReward('0')).toBe('1 이상의 숫자를 입력해주세요.');
    expect(validateReward('-5')).toBe('1 이상의 숫자를 입력해주세요.');
    expect(validateReward('1.5')).toBe('1 이상의 숫자를 입력해주세요.');
    expect(validateReward('abc')).toBe('1 이상의 숫자를 입력해주세요.');
  });

  it('상한을 넘으면 얼마까지 되는지 말해 준다', () => {
    // 전에는 서버가 400 을 주고 화면은 "입력한 내용을 다시 확인해주세요" 만 띄웠다.
    // 무엇이 문제인지 알 수 없어서, 한도를 문구에 담는다.
    expect(validateReward(String(REWARD_MAX))).toBeUndefined();
    expect(validateReward(String(REWARD_MAX + 1))).toBe('도토리는 100,000개까지 넣을 수 있어요.');
    expect(validateReward('999999999999')).toBe('도토리는 100,000개까지 넣을 수 있어요.');
  });
});
