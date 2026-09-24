import { Image, StyleSheet, Text, View } from 'react-native';

import { useTodayChores } from '@/features/chore';
import { useTheme } from '@/shared/theme';

type UpdateBannerProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * 오늘 아직 할 일이 남아 있을 때만 보이는 안내 배너.
 * "새로 업데이트됐다"는 서버가 알려주는 개념이 아니라, 남은 할 일이 있다는 걸 알려주는 용도다.
 */
export function UpdateBanner({ familyId, childId }: UpdateBannerProps) {
  const theme = useTheme();
  const { remainingCount } = useTodayChores(familyId, childId);

  if (remainingCount === 0) return null;

  return (
    <View
      style={[
        styles.row,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.full,
          paddingVertical: theme.space.sm,
          paddingHorizontal: theme.space.lg,
          gap: theme.space.sm,
        },
      ]}
    >
      <Image
        source={require('@/assets/images/faces/face-basic.png')}
        style={styles.face}
        resizeMode="contain"
      />
      <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
        오늘 해야 할 미션이 있어요!
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  face: { width: 28, height: 28 },
});
