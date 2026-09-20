import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { useTheme } from '@/shared/theme';

type Props = {
  /** 좌측 3D 아이콘 (Figma 아이콘 시트에서 추출한 이미지). 생략하면 글자만 나온다 */
  icon?: ImageSourcePropType;
  title: string;
  /** 우측 텍스트 액션 (예: "보유한 주식 보기") */
  action?: { label: string; onPress: () => void };
};

/**
 * 투자 화면의 섹션 제목 줄 (아이콘 + 제목 + 우측 액션).
 */
export function SectionTitle({ icon, title, action }: Props) {
  const theme = useTheme();

  return (
    <View style={styles.row}>
      <View style={[styles.left, { gap: theme.space.sm }]}>
        {icon ? <Image source={icon} style={styles.icon} resizeMode="contain" /> : null}
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{title}</Text>
      </View>
      {action ? (
        <Pressable
          accessibilityRole="button"
          onPress={action.onPress}
          hitSlop={8}
          style={styles.action}
        >
          <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
            {action.label}
          </Text>
          <Ionicons name="chevron-forward" size={16} color={theme.color.text.default} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    width: 28,
    height: 28,
  },
  action: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
});
