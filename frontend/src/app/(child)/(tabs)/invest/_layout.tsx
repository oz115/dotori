import { Stack } from 'expo-router';

/**
 * 투자 탭 안의 자체 스택 (목록 → 보유 상품 / 상품 상세). 탭 레이아웃 안에 중첩돼 있어서
 * 이 안에서 화면을 넘겨도 하단 탭바는 계속 그 자리에 고정돼 있다.
 *
 * initialRouteName은 홈에서 상세로 바로 들어왔을 때 그 아래에 목록이 깔리게 한다
 * (상점 탭과 같은 이유 — 없으면 하단 탭을 다시 눌러도 목록으로 못 돌아간다).
 */
export const unstable_settings = { initialRouteName: 'index' };

export default function InvestStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
