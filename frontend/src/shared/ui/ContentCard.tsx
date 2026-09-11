import type { ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { useTheme } from '@/shared/theme';

type ContentCardProps = {
  /** 패딩·gap·flex 등 카드마다 다른 값은 여기로 넘긴다 */
  style?: StyleProp<ViewStyle>;
  children: ReactNode;
};

/**
 * 홈 화면 공통 흰 카드 (할 일·상품·활동 보드).
 *
 * 테두리(#F3F4F6)와 그림자(X0 Y4 Blur20 5%)가 한 세트로 묶여 있어 따로 쓰면
 * 카드마다 미묘하게 달라진다 — 이 컴포넌트를 통해서만 쓴다.
 */
export function ContentCard({ style, children }: ContentCardProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.background.surface,
          borderColor: theme.color.border.card,
          borderRadius: theme.radius.lg,
        },
        theme.shadow.contentCard,
        style,
      ]}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1 },
});
