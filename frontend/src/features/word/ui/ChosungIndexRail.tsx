import { Pressable, StyleSheet, Text, View } from 'react-native';

import { CHOSUNG_ORDER } from '@/features/word/lib/chosung';
import { useTheme } from '@/shared/theme';

type ChosungIndexRailProps = {
  /** 지금 목록에 실제로 존재하는 초성만 눌리게 한다 (검색·필터로 줄어들 수 있어서) */
  present: ReadonlySet<string>;
  onJump: (chosung: string) => void;
};

/** 목록 옆 세로 초성 인덱스. 국어사전·영단어장 앱의 A-Z 탭과 같은 자리다. */
export function ChosungIndexRail({ present, onJump }: ChosungIndexRailProps) {
  const theme = useTheme();

  return (
    <View style={[styles.rail, { paddingVertical: theme.space.sm, gap: theme.space.xs }]}>
      {CHOSUNG_ORDER.map((chosung) => {
        const active = present.has(chosung);
        return (
          <Pressable
            key={chosung}
            disabled={!active}
            accessibilityRole="button"
            accessibilityLabel={`${chosung}으로 이동`}
            onPress={() => onJump(chosung)}
            hitSlop={4}
          >
            <Text
              style={[
                theme.typography.caption,
                {
                  fontSize: 11 * theme.textScale,
                  color: active ? theme.color.brand.solidActive : theme.color.text.muted,
                  opacity: active ? 1 : 0.4,
                },
              ]}
            >
              {chosung}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  rail: { width: 20, alignItems: 'center' },
});
