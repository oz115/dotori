/** words 테이블 계약. 가족 범위가 아니라 전역 사전이라 familyId 가 없다. */
export type WordCategory =
  'INCOME' | 'CONSUMPTION' | 'SAVINGS' | 'INVESTMENT' | 'BANKING' | 'ECONOMY' | 'OTHER';

/**
 * GET .../words 목록 한 건. 뜻(definition)은 명세상 목록 응답에 없다 — 상세에서만 내려온다.
 * category 는 NULL 허용 컬럼이다.
 */
export type WordSummary = {
  wordId: number;
  word: string;
  /** 1~4. AI 서버 quiz 도메인의 TIER_TO_DIFFICULTY 와 같은 구간을 쓴다 */
  difficultyLevel: number;
  category: WordCategory | null;
};

/** GET .../words/{wordId} 상세. 목록 필드 + 뜻(definition). */
export type WordDetailInfo = WordSummary & {
  definition: string;
};

/**
 * GET .../words/search?keyword 결과 한 건. 목록/상세와 달리 category 가 없다 —
 * 검색 API 자체가 카테고리를 모른다(요청도 응답도).
 */
export type WordSearchSummary = {
  wordId: number;
  word: string;
  definition: string;
  difficultyLevel: number;
};

/**
 * 목록 화면(SectionList·초성 인덱스)이 실제로 쓰는 최소 필드. 무한 스크롤 목록(WordSummary)과
 * 검색 결과(WordSearchSummary) 둘 다 이 모양을 만족해서, 화면 쪽 컴포넌트가 두 출처를
 * 구분할 필요가 없다.
 */
export type WordListItem = {
  wordId: number;
  word: string;
};
