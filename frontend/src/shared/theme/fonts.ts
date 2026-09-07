import { useFonts } from 'expo-font';

/**
 * Pretendard 폰트 로딩. `_layout.tsx`에서 `useAppFonts()`를 호출해
 * 폰트가 준비될 때까지 스플래시를 유지합니다.
 *
 * 여기서 등록하는 키(`Pretendard-Regular` 등)가 곧 `fontFamily` 값이며,
 * typography 토큰의 `fontFamily`와 1:1로 대응합니다.
 */
export const fontAssets = {
  'Pretendard-Regular': require('@/assets/fonts/Pretendard-Regular.otf'),
  'Pretendard-Medium': require('@/assets/fonts/Pretendard-Medium.otf'),
  'Pretendard-SemiBold': require('@/assets/fonts/Pretendard-SemiBold.otf'),
  'Pretendard-Bold': require('@/assets/fonts/Pretendard-Bold.otf'),
};

export const fontFamily = {
  regular: 'Pretendard-Regular',
  medium: 'Pretendard-Medium',
  semibold: 'Pretendard-SemiBold',
  bold: 'Pretendard-Bold',
} as const;

/** 폰트 로딩 상태. `[loaded, error]` 튜플을 그대로 돌려준다. */
export function useAppFonts() {
  return useFonts(fontAssets);
}
