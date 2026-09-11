import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { useTodayChores, type TodayChore } from '@/features/chore';
import { useTheme } from '@/shared/theme';

type TodayChoresProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * "오늘 할 일" 섹션. 집안일 완료 인증(사진 업로드 등)은 아직 이 화면 몫이 아니라
 * 목록은 읽기 전용이다 — 상태만 보여준다.
 */
export function TodayChores({ familyId, childId }: TodayChoresProps) {
  const theme = useTheme();
  const { items, remainingCount, isPending, isError } = useTodayChores(familyId, childId);

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          오늘 할 일
        </Text>
        {items.length > 0 ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            {remainingCount}개 남음
          </Text>
        ) : null}
      </View>

      {isPending ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          불러오는 중...
        </Text>
      ) : isError ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
          할 일을 불러오지 못했어요.
        </Text>
      ) : items.length === 0 ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          오늘은 등록된 할 일이 없어요.
        </Text>
      ) : (
        <View style={{ gap: theme.space.sm }}>
          {items.map((chore) => (
            <ChoreRow key={chore.choreId} chore={chore} />
          ))}
        </View>
      )}
    </View>
  );
}

function ChoreRow({ chore }: { chore: TodayChore }) {
  const theme = useTheme();
  const done = chore.status === 'DONE';
  const pending = chore.status === 'PENDING';

  const icon = done ? 'checkmark-circle' : pending ? 'time-outline' : 'help-circle-outline';
  const trailing = done ? '완료' : pending ? '승인 대기' : rewardLabel(chore.rewardPoints);
  const trailingColor = done || pending ? theme.color.text.muted : theme.color.status.success.text;

  return (
    <View
      style={[
        styles.row,
        {
          borderRadius: theme.radius.lg,
          borderColor: theme.color.border.default,
          padding: theme.space.md,
          gap: theme.space.md,
          opacity: done ? 0.6 : 1,
        },
      ]}
    >
      <View
        style={[
          styles.iconBox,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
        ]}
      >
        <Ionicons name={icon} size={20} color={theme.color.brand.solid} />
      </View>
      <Text
        style={[theme.typography.body, { color: theme.color.text.strong, flex: 1 }]}
        numberOfLines={1}
      >
        {chore.title}
      </Text>
      <Text style={[theme.typography.label, { color: trailingColor }]}>{trailing}</Text>
    </View>
  );
}

function rewardLabel(rewardPoints: number | null): string {
  return rewardPoints == null ? '산정 중' : `+${rewardPoints}P`;
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  row: { flexDirection: 'row', alignItems: 'center', borderWidth: 1 },
  iconBox: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
