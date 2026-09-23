import { Stack } from 'expo-router';

/**
 * 할 일 탭 안의 자체 스택 (목록 → 완료 이력 / 미션 상세). 탭 레이아웃 안에 중첩돼 있어서
 * 이 안에서 화면을 넘겨도 하단 탭바는 그 자리에 고정된다 (상점 탭과 같은 구조).
 *
 * initialRouteName을 index(목록)로 명시해야, 홈·알림에서 task/[choreId]로 바로
 * 들어왔을 때 그 아래에 목록 화면이 자동으로 깔린다 — 이게 없으면 상세 하나만
 * 스택에 쌓여서 하단 탭을 다시 눌러도 목록으로 못 돌아간다 (상점 탭과 같은 이유).
 */
export const unstable_settings = { initialRouteName: 'index' };

export default function TaskStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
