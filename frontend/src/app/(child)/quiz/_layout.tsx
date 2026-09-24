import { Stack } from 'expo-router';

/** 퀴즈 화면 하나짜리 스택. 홈에서 push로 들어오는 독립 화면이라 탭 밖에 있다. */
export default function QuizStackLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
