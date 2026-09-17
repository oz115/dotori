import { Stack } from 'expo-router';

/** 단어장 자체 스택 (목록 → 상세). 홈에서 push로 들어오는 독립 화면이라 탭 밖에 있다. */
export default function WordStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
