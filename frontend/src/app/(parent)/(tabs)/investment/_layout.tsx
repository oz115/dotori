import { Stack } from 'expo-router';

/**
 * 부모 투자 탭 안의 자체 스택 (메인 → 종목 상세 / 뉴스 / 지난 뉴스 해설 / 전체 거래 내역).
 * 탭 레이아웃 안에 중첩돼 있어서 이 안에서 화면을 넘겨도 하단 탭바는 그 자리에 고정된다.
 * 자녀 투자 탭(`(child)/(tabs)/invest`)과는 보이는 내용이 달라 화면을 공유하지 않고
 * 라우트 이름도 다르게 뒀다 (invest ↔ investment).
 */
export default function ParentInvestmentStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
