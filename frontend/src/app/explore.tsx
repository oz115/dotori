import { SafeAreaView, StyleSheet, Text, useColorScheme, View } from 'react-native';

const LAYERS = [
  ['app', 'expo-router 라우트. (auth)/(child)/(parent) 같은 라우트 그룹으로 화면을 묶음.'],
  ['widgets', '여러 feature를 조합한 큰 UI 블록 (예: child-home, asset-summary).'],
  ['features', '사용자 행동/기능 단위. 자기 상태(model)까지 함께 소유 (예: auth, wallet, task).'],
  ['shared', 'api, ui, hooks, lib, constants, types, theme 등 레이어와 무관한 공용 코드.'],
] as const;

/**
 * 이 프로젝트의 폴더 구조를 팀원에게 설명하는 용도로만 존재하므로,
 * 실제 화면이 생기면 자유롭게 교체하세요.
 */
export default function ExploreScreen() {
  const isDark = useColorScheme() === 'dark';

  return (
    <SafeAreaView style={[styles.safeArea, isDark && styles.safeAreaDark]}>
      <View style={styles.container}>
        <Text style={[styles.title, isDark && styles.titleDark]}>폴더 구조 (FSD-lite)</Text>
        {LAYERS.map(([name, desc]) => (
          <View key={name}>
            <Text style={[styles.layerName, isDark && styles.layerNameDark]}>src/{name}</Text>
            <Text style={[styles.description, isDark && styles.descriptionDark]}>{desc}</Text>
          </View>
        ))}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  safeAreaDark: {
    backgroundColor: '#000000',
  },
  container: {
    flex: 1,
    gap: 16,
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  title: {
    color: '#111827',
    fontSize: 20,
    fontWeight: '700',
  },
  titleDark: {
    color: '#ffffff',
  },
  layerName: {
    color: '#2563eb',
    fontFamily: 'monospace',
    fontSize: 16,
  },
  layerNameDark: {
    color: '#60a5fa',
  },
  description: {
    color: '#6b7280',
  },
  descriptionDark: {
    color: '#9ca3af',
  },
});
