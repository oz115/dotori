const CHOSUNG_LIST = [
  'ㄱ',
  'ㄲ',
  'ㄴ',
  'ㄷ',
  'ㄸ',
  'ㄹ',
  'ㅁ',
  'ㅂ',
  'ㅃ',
  'ㅅ',
  'ㅆ',
  'ㅇ',
  'ㅈ',
  'ㅉ',
  'ㅊ',
  'ㅋ',
  'ㅌ',
  'ㅍ',
  'ㅎ',
];

/** 인덱스 레일에서 겹자음은 기본 자음으로 합쳐서 보여준다 (ㄲ→ㄱ 등). */
const COMPLEX_TO_BASE: Record<string, string> = {
  ㄲ: 'ㄱ',
  ㄸ: 'ㄷ',
  ㅃ: 'ㅂ',
  ㅆ: 'ㅅ',
  ㅉ: 'ㅈ',
};

/** 목록 인덱스 레일에 실제로 노출하는 14개 자음 순서. */
export const CHOSUNG_ORDER = [
  'ㄱ',
  'ㄴ',
  'ㄷ',
  'ㄹ',
  'ㅁ',
  'ㅂ',
  'ㅅ',
  'ㅇ',
  'ㅈ',
  'ㅊ',
  'ㅋ',
  'ㅌ',
  'ㅍ',
  'ㅎ',
] as const;

const HANGUL_BASE = 0xac00;
const HANGUL_LAST = 0xd7a3;
const CHOSUNG_UNIT = 588; // 중성(21) * 종성(28)

/** 완성형 한글 음절의 초성을 반환한다. 한글이 아니면 첫 글자를 그대로 돌려준다(영문 약어 등 대비). */
export function chosungOf(word: string): string {
  const code = word.charCodeAt(0);
  if (code < HANGUL_BASE || code > HANGUL_LAST) return word.charAt(0).toUpperCase();
  return CHOSUNG_LIST[Math.floor((code - HANGUL_BASE) / CHOSUNG_UNIT)];
}

/** 인덱스 레일(ㄱㄴㄷ… 14개)에서 쓰는 그룹 키. 겹자음은 기본 자음으로 합친다. */
export function chosungGroupOf(word: string): string {
  const raw = chosungOf(word);
  return COMPLEX_TO_BASE[raw] ?? raw;
}
