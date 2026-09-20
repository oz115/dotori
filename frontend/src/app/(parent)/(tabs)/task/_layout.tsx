import { Stack } from 'expo-router';

/**
 * 할 일 탭 안의 자체 스택. 탭 레이아웃 안에 중첩돼 있어서 이 안에서 화면을 넘겨도
 * 하단 탭바는 그 자리에 고정된다 (상점 탭과 같은 구조).
 *
 * initialRouteName을 index(목록)로 명시해 두는 이유도 상점 탭과 같다 — 탭 밖(알림함/푸시)에서
 * task/requests로 곧장 딥링크했을 때 그 아래에 목록 화면이 깔리게 한다.
 */
export const unstable_settings = { initialRouteName: 'index' };

export default function TaskStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
