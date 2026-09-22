import { Image } from 'expo-image';
import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

/** 쉬운 설명을 들려주는 화자. 미션·퀴즈에서 쓰는 것과 같은 다람쥐다 */
const SQUIRREL = require('@/assets/images/chore/squirrel-face.png');

type EasyDefinitionBubbleProps = {
  /** 아이 눈높이로 풀어 쓴 설명. 아직 채우지 못한 단어가 있어 null 로도 온다 */
  easyDefinition: string | null;
};

/**
 * 단어 상세의 "쉽게 말하면" 말풍선.
 *
 * <p>다람쥐가 건네는 말로 그린다. 같은 화면에 글이 두 덩이 있으면 어느 쪽을 읽어야 할지
 * 아이가 헷갈리는데, 화자를 세워 두면 "사전이 적은 뜻"과 "누가 풀어 준 말"로 역할이 갈린다.
 *
 * <p>설명이 없으면 아무것도 그리지 않는다 — 빈 말풍선이 뜨면 다람쥐가 할 말을 잃은 것처럼
 * 보이고, 아직 채우지 못한 단어가 실제로 있다.
 */
export function EasyDefinitionBubble({ easyDefinition }: EasyDefinitionBubbleProps) {
  const theme = useTheme();

  if (!easyDefinition) {
    return null;
  }

  return (
    <View
      accessible
      accessibilityLabel={`쉽게 말하면, ${easyDefinition}`}
      style={[styles.row, { gap: theme.space.sm }]}
    >
      <Image source={SQUIRREL} style={styles.mascot} contentFit="contain" />

      <View
        style={[
          styles.bubble,
          {
            backgroundColor: theme.color.brand.subtle,
            borderRadius: theme.radius.lg,
            padding: theme.space.lg,
            gap: theme.space.xs,
          },
        ]}
      >
        {/* 말풍선 꼬리 — 테두리 삼각형 트릭. 다람쥐 쪽(왼쪽)을 가리킨다 */}
        <View style={[styles.tail, { borderRightColor: theme.color.brand.subtle }]} />

        <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
          쉽게 말하면
        </Text>
        {/* 아이가 실제로 읽는 본문이라 줄간격을 넉넉히 준다 (퀴즈 해설과 같은 값) */}
        <Text style={[theme.typography.body, styles.text, { color: theme.color.text.default }]}>
          {easyDefinition}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  // 다람쥐는 말풍선 첫 줄에 맞춰 위쪽 정렬. 가운데 정렬하면 긴 설명에서 저 혼자 아래로 내려간다
  row: { flexDirection: 'row', alignItems: 'flex-start' },
  mascot: { width: 40, height: 40, marginTop: 4 },
  bubble: { flex: 1 },
  tail: {
    position: 'absolute',
    left: -7,
    top: 18,
    width: 0,
    height: 0,
    backgroundColor: 'transparent',
    borderTopWidth: 7,
    borderBottomWidth: 7,
    borderRightWidth: 8,
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
  },
  text: { lineHeight: 28 },
});
