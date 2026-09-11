import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type NumericKeypadProps = {
  onDigit: (digit: string) => void;
  onBackspace: () => void;
};

const ROWS = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['00', '0', 'backspace'],
] as const;

/**
 * 금액 입력용 숫자 키패드 (충전·환전 화면 공용). OS 키보드 대신 화면에 직접 그려서
 * "얼마 보낼지"에만 집중하게 한다 — 은행 앱 송금 화면과 같은 패턴.
 */
export function NumericKeypad({ onDigit, onBackspace }: NumericKeypadProps) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md, marginBottom: theme.space.md }}>
      {ROWS.map((row, i) => (
        <View key={i} style={[styles.row, { gap: theme.space.md }]}>
          {row.map((key) =>
            key === 'backspace' ? (
              <Pressable
                key={key}
                onPress={onBackspace}
                style={styles.key}
                accessibilityRole="button"
                accessibilityLabel="지우기"
              >
                <Ionicons name="backspace-outline" size={28} color={theme.color.text.strong} />
              </Pressable>
            ) : (
              <Pressable
                key={key}
                onPress={() => onDigit(key)}
                style={styles.key}
                accessibilityRole="button"
                accessibilityLabel={key}
              >
                <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>{key}</Text>
              </Pressable>
            ),
          )}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  // 아이가 쓰는 화면이라 터치 영역을 넉넉히 — 키가 커지면서 키패드가 세로 공간도 더 차지한다
  key: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingVertical: 20 },
});
