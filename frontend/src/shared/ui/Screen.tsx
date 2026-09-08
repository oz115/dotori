import type { ReactNode } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  View,
  type ViewStyle,
} from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';

type ScreenProps = {
  children: ReactNode;
  /** 콘텐츠를 세로 스크롤로 감싼다 (폼·긴 화면) */
  scroll?: boolean;
  /** 키보드가 올라올 때 콘텐츠를 밀어올린다 (입력 화면) */
  keyboardAvoiding?: boolean;
  /** safe-area 적용 가장자리. 기본 top + bottom */
  edges?: readonly Edge[];
  /** 좌우 화면 여백 적용. 기본 true */
  padded?: boolean;
  /** 스크롤 콘텐츠에 flexGrow:1 (하단 고정 배치용 flex 스페이서가 동작하게). 기본 true */
  fill?: boolean;
  style?: ViewStyle;
};

/**
 * 화면 공통 레이아웃. 배경색·좌우 여백·safe-area·스크롤·키보드 회피를 한 곳에서 처리한다.
 * 화면은 raw padding 숫자 대신 이 컴포넌트와 theme 토큰으로 간격을 잡는다.
 */
export function Screen({
  children,
  scroll = false,
  keyboardAvoiding = false,
  edges = ['top', 'bottom'],
  padded = true,
  fill = true,
  style,
}: ScreenProps) {
  const theme = useTheme();

  const padding: ViewStyle = {
    paddingHorizontal: padded ? theme.layout.screenX : 0,
    paddingVertical: theme.layout.screenY,
  };

  let body: ReactNode = scroll ? (
    <ScrollView
      contentContainerStyle={[padding, fill && styles.grow, style]}
      keyboardShouldPersistTaps="handled"
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  ) : (
    <View style={[styles.flex, padding, style]}>{children}</View>
  );

  if (keyboardAvoiding) {
    body = (
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {body}
      </KeyboardAvoidingView>
    );
  }

  return (
    <SafeAreaView
      edges={edges}
      style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}
    >
      {body}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  grow: { flexGrow: 1 },
});
