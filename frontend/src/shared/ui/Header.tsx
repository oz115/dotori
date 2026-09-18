import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';

type HeaderProps = {
  title: string;
  /** 넘기면 좌측에 뒤로가기 화살표가 나온다 */
  onBack?: () => void;
  /** 넘기면 좌측에 닫기(X) 아이콘이 나온다 (onBack과 동시에 넘기면 이쪽이 우선한다) */
  onClose?: () => void;
  /** 우측 텍스트 액션 (Figma: "취소") */
  action?: { label: string; onPress: () => void };
  /** action 대신 임의의 우측 요소를 넣고 싶을 때 */
  right?: ReactNode;
  /** 헤더 배경색 override. 기본은 흰색 — 화면 배경과 이어붙이고 싶을 때만 넘긴다 */
  backgroundColor?: string;
};

const SIDE_WIDTH = 56;

/**
 * 화면 상단 헤더 (Figma: 흰 배경, 가운데 타이틀, 좌측 뒤로가기, 우측 텍스트 액션).
 * 상태바 높이는 safe-area inset으로 자동 확보한다.
 */
export function Header({ title, onBack, onClose, action, right, backgroundColor }: HeaderProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: insets.top,
          // 아래 콘텐츠(배너·카드 등)와 같은 화면 표준 여백을 써서 헤더만 가장자리에
          // 더 붙어 보이지 않게 한다. "구매 내역" 같은 우측 버튼이 화면 끝에 너무
          // 붙어 보인다는 피드백으로 space.md(12) → layout.screenX(20)로 넓힘.
          paddingHorizontal: theme.layout.screenX,
          backgroundColor: backgroundColor ?? theme.color.background.surface,
        },
      ]}
    >
      <View style={styles.row}>
        {/* 좌우 콘텐츠 너비가 다르면(예: "구매 내역" 알약 버튼) flex:1 제목이 밀려서 화면
            중앙과 안 맞는다. 그래서 제목은 행 전체 너비에 절대 위치로 겹쳐 항상 화면
            정중앙에 고정하고, 좌우 버튼은 그 아래에서 자기 자리로만 밀려나게 분리한다. */}
        <Text
          pointerEvents="none"
          numberOfLines={1}
          style={[theme.typography.body, styles.title, { color: theme.color.text.strong }]}
        >
          {title}
        </Text>

        <View style={styles.side}>
          {onClose ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="닫기"
              onPress={onClose}
              hitSlop={8}
            >
              <Ionicons name="close" size={24} color={theme.color.text.strong} />
            </Pressable>
          ) : onBack ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="뒤로가기"
              onPress={onBack}
              hitSlop={8}
            >
              <Ionicons name="chevron-back" size={24} color={theme.color.text.strong} />
            </Pressable>
          ) : null}
        </View>

        <View style={[styles.side, styles.sideRight]}>
          {right ??
            (action ? (
              <Pressable accessibilityRole="button" onPress={action.onPress} hitSlop={8}>
                <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
                  {action.label}
                </Text>
              </Pressable>
            ) : null)}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  row: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  side: {
    // width 고정이 아니라 minWidth — 짧은 콘텐츠(뒤로가기 화살표, "취소")는 예전과 똑같이
    // 56px로 보이고, "구매 내역" 같은 넓은 pill 버튼은 줄바꿈 없이 필요한 만큼 넓어진다.
    // 제목이 더 이상 이 두 slot 사이에서 크기를 계산하지 않아서(title 참고), 좌우
    // 너비가 달라도 제목 중앙 정렬에 영향을 안 준다.
    minWidth: SIDE_WIDTH,
    justifyContent: 'center',
  },
  sideRight: {
    alignItems: 'flex-end',
  },
  title: {
    position: 'absolute',
    left: 0,
    right: 0,
    textAlign: 'center',
  },
});
