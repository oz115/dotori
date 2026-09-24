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

import { useTabOverlap } from './TabScene';

type ScreenProps = {
  children: ReactNode;
  /** 스크롤 영역 위에 고정으로 붙는 헤더 (Header 컴포넌트). 넘기면 top safe-area 는 헤더가 처리 */
  header?: ReactNode;
  /** 콘텐츠를 세로 스크롤로 감싼다 (폼·긴 화면) */
  scroll?: boolean;
  /** 키보드가 올라올 때 콘텐츠를 밀어올린다 (입력 화면) */
  keyboardAvoiding?: boolean;
  /** safe-area 적용 가장자리. 기본 top + bottom (header 있으면 bottom 만) */
  edges?: readonly Edge[];
  /** 좌우 화면 여백 적용. 기본 true */
  padded?: boolean;
  /** 스크롤 콘텐츠에 flexGrow:1 (하단 고정 배치용 flex 스페이서가 동작하게). 기본 true */
  fill?: boolean;
  /** 화면 배경색 override. 기본은 theme.color.background.canvas — 특정 화면만 다른 배경을 쓸 때만 넘긴다 */
  backgroundColor?: string;
  style?: ViewStyle;
};

/**
 * 화면 공통 레이아웃. 배경색·좌우 여백·safe-area·스크롤·키보드 회피를 한 곳에서 처리한다.
 * 화면은 raw padding 숫자 대신 이 컴포넌트와 theme 토큰으로 간격을 잡는다.
 */
export function Screen({
  children,
  header,
  scroll = false,
  keyboardAvoiding = false,
  edges,
  padded = true,
  fill = true,
  backgroundColor,
  style,
}: ScreenProps) {
  const theme = useTheme();
  const tabOverlap = useTabOverlap();
  const safeEdges = edges ?? (header ? (['bottom'] as const) : (['top', 'bottom'] as const));

  const padding: ViewStyle = {
    paddingHorizontal: padded ? theme.layout.screenX : 0,
    paddingVertical: theme.layout.screenY,
  };
  const scrollBottomPadding =
    style?.paddingBottom ?? style?.paddingVertical ?? style?.padding ?? theme.layout.screenY;

  let body: ReactNode = scroll ? (
    <ScrollView
      contentContainerStyle={[
        padding,
        fill && styles.grow,
        style,
        // 탭 내부 폼은 마지막 버튼까지 올릴 수 있게 스크롤 콘텐츠 안에 여백을 둔다.
        tabOverlap > 0 && {
          paddingBottom:
            tabOverlap +
            Math.max(
              typeof scrollBottomPadding === 'number' ? scrollBottomPadding : 0,
              theme.space.lg,
            ),
        },
      ]}
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
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        {body}
      </KeyboardAvoidingView>
    );
  }

  return (
    <SafeAreaView
      edges={safeEdges}
      style={[
        styles.flex,
        {
          backgroundColor: backgroundColor ?? theme.color.background.canvas,
          // 직접 스크롤하는 화면은 콘텐츠 패딩으로, 고정 버튼이 있는 화면은 바깥에서 보호한다.
          paddingBottom: scroll ? 0 : tabOverlap,
        },
      ]}
    >
      {header}
      {body}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  grow: { flexGrow: 1 },
});
