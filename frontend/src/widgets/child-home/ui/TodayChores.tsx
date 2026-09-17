import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { mockTodayChores, useTodayChores, type TodayChore } from '@/features/chore';
import { useTheme } from '@/shared/theme';
import { ContentCard, MockBadge } from '@/shared/ui';

type TodayChoresProps = {
  familyId: number | undefined;
  childId: number | undefined;
};

/**
 * "오늘의 미션" 섹션. 집안일 완료 인증(사진 업로드 등)은 아직 이 화면 몫이 아니라
 * 목록은 읽기 전용이다 — 상태만 보여준다.
 *
 * 아직 등록된 집안일이 없으면(또는 조회에 실패하면) 예시 데이터로 자리를 채운다 —
 * 화면 배치를 확인하려는 것이라 "예시" 표시를 함께 달아 실제 데이터와 구분한다.
 */
export function TodayChores({ familyId, childId }: TodayChoresProps) {
  const theme = useTheme();
  const { items, remainingCount, isPending, isError } = useTodayChores(familyId, childId);

  const usingMock = !isPending && items.length === 0;
  const rows = usingMock ? mockTodayChores : items;
  const remaining = usingMock
    ? mockTodayChores.filter((c) => c.status === 'TODO').length
    : remainingCount;

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <View style={[styles.titleRow, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            오늘의 미션
          </Text>
          {usingMock ? <MockBadge /> : null}
        </View>
        {rows.length > 0 ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            {remaining}개 남음
          </Text>
        ) : null}
      </View>

      {isPending ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
          불러오는 중...
        </Text>
      ) : (
        <>
          {isError ? (
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              미션을 불러오지 못해 예시를 보여주고 있어요.
            </Text>
          ) : null}
          <View style={{ gap: theme.space.sm }}>
            {rows.map((chore) => (
              <ChoreRow key={chore.choreId} chore={chore} />
            ))}
          </View>
        </>
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
    <ContentCard
      style={[
        styles.row,
        {
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
    </ContentCard>
  );
}

function rewardLabel(rewardPoints: number | null): string {
  return rewardPoints == null ? '산정 중' : `+${rewardPoints}P`;
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  row: { flexDirection: 'row', alignItems: 'center' },
  iconBox: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
});
