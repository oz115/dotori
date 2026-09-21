import { Stack } from 'expo-router';

/** 부모용 단어장 스택 (목록 → 상세). 아이 단어장과 같은 화면을 읽기 전용으로 본다. 홈에서 push로 들어오는 독립 화면이라 탭 밖에 있다. */
export default function ParentWordStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
