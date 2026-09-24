import type { ImageSourcePropType } from 'react-native';

import type { SavingsProductType } from '@/features/savings/model/types';

/**
 * 예적금 화면 자산·문구 — 상품 유형별 아이콘, 화면에 보여 줄 상품 이름·설명, 안내 배너 문구.
 * 아이콘은 앱 공용 아이콘 시트에서 잘라낸 것들이다.
 *
 * <p>여러 줄 문구는 줄바꿈을 직접 넣는다. 자동 줄바꿈에 맡기면 한글은 글자 단위로 끊겨서
 * "복리예금이 / 에요." 처럼 어미만 다음 줄에 남는다. 대신 각 줄이 실제 칸에 들어가야
 * 하므로 글자 수 상한(BANNER_LINE_MAX·DESCRIPTION_LINE_MAX)을 두고 테스트로 잠근다.
 */

const PRODUCT_IMAGES: Record<SavingsProductType, ImageSourcePropType> = {
  // 예금 3종
  ORDINARY: require('@/assets/images/savings/piggy.png'), // 보통예금 — 수시입출금 저금통
  SIMPLE_INTEREST: require('@/assets/images/invest/coin.png'), // 단리예금 — 동전
  COMPOUND_INTEREST: require('@/assets/images/invest/sprout.png'), // 복리예금 — 이자가 자라는 새싹
  // 적금 3종
  SMALL_INSTALLMENT: require('@/assets/images/acorn.png'), // 소액적금 — 도토리 한 알씩
  MID_TERM_INSTALLMENT: require('@/assets/images/savings/target.png'), // 중기적금 — 과녁(다음 목표 지점)
  LONG_TERM_INSTALLMENT: require('@/assets/images/savings/safe.png'), // 장기적금 — 금고
};

export function savingsProductImage(productType: SavingsProductType): ImageSourcePropType {
  return PRODUCT_IMAGES[productType];
}

/** 배너 캐릭터 — 도토리를 모으는 돼지저금통 */
export const SAVINGS_BANNER_IMAGE: ImageSourcePropType = require('@/assets/images/savings/piggy.png');

type BannerCopy = {
  title: string;
  /** 줄 단위 본문 (BANNER_LINE_MAX 주석 참고) */
  body: string;
  /** body 안에서 brand 색으로 강조할 구절들 */
  highlights: string[];
};

/**
 * 배너 본문 한 줄의 글자 수 상한.
 *
 * 배너 글은 마스코트 옆 좁은 칸에 들어간다 — 411dp 기기에서 화면 여백(20×2)·카드 패딩
 * (20×2)·마스코트(84)·간격(12)을 빼면 235dp 뿐이다. 14pt 한글은 한 자가 13dp 남짓이라
 * 20자를 넘기면 그 줄이 또 접힌다.
 */
export const BANNER_LINE_MAX = 20;

/**
 * 저축 안내 배너 — 예금·적금을 한 장으로 덮는다.
 *
 * 예전에는 세그먼트에서 고른 쪽만 설명했는데, 상단 선택이 "전체 · 예금 · 적금" 칩으로
 * 바뀌면서 전체를 고른 상태가 기본이 됐다. 그때 한쪽만 설명하면 반쪽짜리 안내가 된다.
 *
 * <p>둘의 차이는 "한 번에 맡겨서" ↔ "차곡차곡" 두 구절에 걸려 있으니 그 둘만 색으로
 * 강조한다. 한쪽만 강조하면 비교로 읽히지 않고 적금 이야기처럼 보인다.
 *
 * <p>"목돈" 은 뺐다. 좁은 칸에 넣으면 줄이 넘치고, 초등학생이 아는 말도 아니다.
 */
export const SAVINGS_BANNER_COPY: BannerCopy = {
  title: '저축을 시작해볼까요?',
  body:
    '한 번에 맡겨서 불리는 예금과,\n' +
    '매주 차곡차곡 모으는 적금이 있어요.\n' +
    '약속한 날까지 기다리면\n' +
    '도토리를 더 받아요!',
  highlights: ['한 번에 맡겨서', '차곡차곡'],
};

export type BannerSegment = { text: string; emphasis: boolean };

/**
 * 배너 본문을 강조 구간으로 쪼갠다 — 화면은 emphasis 인 조각만 brand 색으로 칠한다.
 *
 * 강조할 구절이 둘 이상이라 본문을 앞에서부터 훑는다. highlights 배열 순서가 아니라
 * "본문에서 먼저 나오는 것" 부터 잘라야 조각을 그대로 이어붙여 원문이 된다.
 */
export function savingsBannerSegments(copy: BannerCopy = SAVINGS_BANNER_COPY): BannerSegment[] {
  const segments: BannerSegment[] = [];
  let rest = copy.body;

  while (rest.length > 0) {
    const next = copy.highlights
      .map((phrase) => ({ phrase, index: rest.indexOf(phrase) }))
      .filter((hit) => hit.index >= 0)
      .sort((a, b) => a.index - b.index)[0];

    if (next == null) {
      segments.push({ text: rest, emphasis: false });
      break;
    }
    if (next.index > 0) {
      segments.push({ text: rest.slice(0, next.index), emphasis: false });
    }
    segments.push({ text: next.phrase, emphasis: true });
    rest = rest.slice(next.index + next.phrase.length);
  }

  return segments;
}

/**
 * 화면에 보여 줄 상품 이름.
 *
 * 서버 이름(보통예금·단리예금·복리예금…)은 어른 금융 용어다. 초등학생이 목록에서 여섯
 * 개를 훑을 때 "단리" 와 "복리" 는 한 글자 차이라 구분조차 되지 않는다.
 *
 * <p>그래서 이 앱의 세계(다람쥐가 도토리를 모으는)에서 이름을 가져왔다. 볼주머니는 넣고
 * 빼기가 자유롭고, 항아리는 담아 두고 기다리며, 나무는 심으면 불어나고, 겨울 창고는 가장
 * 길게 모은다 — 이름만 봐도 무엇이 다른지 짐작된다.
 *
 * <p>원래 용어를 버리지는 않는다. 설명 첫 줄에 "…예금이에요" 로 넣어 두었다. 금융 용어를
 * 가르치는 앱이라 단어 자체를 안 보여 주면 배울 기회가 사라진다 - 이름으로 끌고 와서
 * 설명에서 알려 주는 순서다.
 *
 * <p>서버는 그대로 둔다. 만기 푸시 알림과 지갑 거래내역이 서버 이름을 쓰므로, 화면과
 * 어긋나는 구간이 남는다 (별도 이슈로 서버 시더까지 맞춰야 한다).
 */
const PRODUCT_NAMES: Record<SavingsProductType, string> = {
  ORDINARY: '볼주머니 저금',
  SIMPLE_INTEREST: '도토리 항아리',
  COMPOUND_INTEREST: '도토리 나무',
  SMALL_INSTALLMENT: '몸풀기 적금',
  MID_TERM_INSTALLMENT: '도토리 곳간',
  LONG_TERM_INSTALLMENT: '겨울 대비 창고',
};

export function savingsProductName(productType: SavingsProductType): string {
  return PRODUCT_NAMES[productType];
}

/**
 * 상품 설명 한 줄의 글자 수 상한.
 *
 * 상세 히어로는 배너보다 넓다 — 411dp 에서 화면 여백(20×2)과 히어로 패딩(24×2)을 빼면
 * 323dp 다. 29자 줄이 접혀서 "보통예금이 / 에요." 가 됐고, 26자면 들어간다.
 */
export const DESCRIPTION_LINE_MAX = 26;

/**
 * 상품 상세 배너에 들어가는 설명.
 *
 * 서버의 `description` 을 쓰지 않는다. 그 값은 시더가 `educationGoal` 을 그대로 넣은
 * 것이고("자유로운 대신 이자가 적다는 점 학습"), 기획 문서에 쓰는 학습 목표 메모라
 * 화면에 내보낼 문장이 아니다. 초등학생이 읽을 말은 화면이 갖는다 — 그룹 안내 문구
 * (SAVINGS_BANNER_COPY)와 같은 자리에 두는 이유다.
 *
 * <p>쓰는 방식은 각 상품의 스펙에서 나온다. 무엇이 다른지를 한 문장으로 짚고, 그 대가가
 * 무엇인지(자유로운 대신 이자가 적다, 오래 기다리는 만큼 많다)를 설명하지 않고 느끼게 한다.
 * 숫자는 적지 않는다 - 이자율·기간·최대 금액은 바로 아래 정보 카드에 이미 있고, 가족
 * 설정에 따라 달라지는 값도 있어 문구에 박으면 어긋난다.
 *
 * <p>한 줄에 문장 하나를 담는다 (DESCRIPTION_LINE_MAX). 문장이 길어 줄을 넘기면 조사나
 * 어미만 다음 줄로 떨어져 읽기 전에 눈에 먼저 걸린다 — 문장을 쪼개는 쪽이 낫다.
 */
const PRODUCT_DESCRIPTIONS: Record<SavingsProductType, string> = {
  ORDINARY:
    '언제든 넣고 빼는 보통예금이에요.\n' +
    '다람쥐 볼주머니처럼 자유롭게 쓰고,\n' +
    '넣어 둔 만큼 매주 이자를 받아요.\n' +
    '자유로운 대신 도토리는 가장 적어요.',
  SIMPLE_INTEREST:
    '도토리를 한 번에 담는 단리예금이에요.\n' +
    '처음 담은 양에만 이자가 붙어요.\n' +
    '그래서 매주 같은 만큼 늘어나고,\n' +
    '나중에 받을 양을 미리 알 수 있어요.',
  COMPOUND_INTEREST:
    '한 알을 심으면 나무가 되는 복리예금이에요.\n' +
    '받은 이자에 또 이자가 붙어요.\n' +
    '그래서 주마다 늘어나는 양이 커져요.\n' +
    '도토리 항아리와 비교해 보세요.',
  SMALL_INSTALLMENT:
    '매주 같은 금액을 두 번 넣는 소액적금이에요.\n' +
    '2주면 끝나니까 처음이어도 괜찮아요.\n' +
    '작게 시작해 모으는 재미를 느껴봐요.',
  MID_TERM_INSTALLMENT:
    '매주 같은 금액을 네 번 넣는 중기적금이에요.\n' +
    '소액적금보다 길고 장기적금보다 짧아요.\n' +
    '기간이 길수록 이자율이 높아져요.',
  LONG_TERM_INSTALLMENT:
    '매주 같은 금액을 여섯 번 넣는 장기적금이에요.\n' +
    '오래 기다려야 하지만 도토리가 가장 많아요.\n' +
    '다람쥐가 겨울을 대비하듯 길게 모아요.',
};

export function savingsProductDescription(productType: SavingsProductType): string {
  return PRODUCT_DESCRIPTIONS[productType];
}
