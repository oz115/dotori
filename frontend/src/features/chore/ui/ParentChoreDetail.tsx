import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import { difficultyLabel, isRewardReady, repeatLabel } from '@/features/chore/lib/choreLabels';
import type { TodayChoreStatus } from '@/features/chore/lib/choreStatus';
import { useChore } from '@/features/chore/model/useChore';
import { useChoreStatus } from '@/features/chore/model/useChoreStatus';
import { useDeleteChore } from '@/features/chore/model/useDeleteChore';
import { DeleteChoreModal } from '@/features/chore/ui/DeleteChoreModal';
import { useTheme } from '@/shared/theme';
import { Button, ContentCard, Header, Screen } from '@/shared/ui';

type ParentChoreDetailProps = {
  familyId: number | undefined;
  childId: number | undefined;
  choreId: number | undefined;
  /** 담당 아이 이름. 가족 슬라이스 값이라 라우트가 넘겨준다 */
  childName: string | undefined;
  /** 수정 화면으로 보내는 일은 라우트가 한다 (feature 가 경로를 알지 않도록) */
  onEdit?: () => void;
};

const STATUS_LABEL: Record<TodayChoreStatus, string> = {
  TODO: '진행 대기',
  PENDING: '확인 필요',
  // 부모 상세는 거절 목록을 받아 오지 않아 지금은 나오지 않는다. 상태 목록을 빠짐없이
  // 채워 둬야 나중에 거절을 겹치기 시작할 때 문구를 빠뜨리지 않는다.
  REJECTED: '거절함',
  DONE: '완료',
};

/**
 * 할 일 상세 (부모).
 *
 * 시안의 "언제까지 9월 12일 오전 9시"에서 시각을 뺐다. 서버 endDate 가 날짜(LocalDate)라
 * 시각이 아예 없다 - 없는 값을 지어내면 부모가 그 시각까지라고 믿는다.
 *
 * 히어로 아이콘은 고정이다. 집안일에는 상점 상품 같은 카테고리 필드가 없어서 제목마다
 * 다른 그림을 고를 근거가 없다.
 */
export function ParentChoreDetail({
  familyId,
  childId,
  choreId,
  childName,
  onEdit,
}: ParentChoreDetailProps) {
  const theme = useTheme();
  const { data: chore, isPending, isError } = useChore(familyId, childId, choreId);
  const status = useChoreStatus(familyId, childId, chore);
  const [confirming, setConfirming] = useState(false);
  const deleteChore = useDeleteChore(familyId, childId);

  // 지우고 나면 이 화면이 가리키던 할 일이 목록에 없다. 상세에 머무르지 않고 목록으로 돌아간다.
  const handleDelete = () => {
    if (choreId == null) return;
    deleteChore.mutate(choreId, {
      onSuccess: () => {
        setConfirming(false);
        if (router.canGoBack()) router.back();
        else router.replace('/task');
      },
    });
  };

  return (
    <Screen
      header={
        <Header
          title="할 일 상세"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/task'))}
          right={
            chore && !chore.deletedAt ? (
              <View style={[styles.actions, { gap: theme.space.sm }]}>
                {onEdit ? (
                  <Button label="수정" variant="outline" size="sm" onPress={onEdit} />
                ) : null}
                <Button
                  label="삭제"
                  variant="outline"
                  tone="danger"
                  size="sm"
                  onPress={() => setConfirming(true)}
                />
              </View>
            ) : undefined
          }
        />
      }
      scroll
    >
      {isPending ? (
        <Centered message="불러오는 중이에요." />
      ) : isError || !chore ? (
        <Centered message="할 일을 찾을 수 없어요." />
      ) : (
        <View style={{ gap: theme.space.lg }}>
          {chore.deletedAt ? (
            <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
              이미 지운 할 일이에요. 새 완료 요청은 들어오지 않아요.
            </Text>
          ) : null}

          <HeroCard title={chore.title} description={chore.description} status={status} />

          <ContentCard style={{ paddingHorizontal: theme.space.lg }}>
            <InfoRow icon="person-outline" label="담당 아이" value={childName ?? '—'} />
            <InfoRow icon="calendar-outline" label="언제까지" value={formatDate(chore.endDate)} />
            <InfoRow icon="leaf-outline" label="난이도" value={difficultyLabel(chore.difficulty)} />
            <InfoRow icon="sync-outline" label="반복 설정" value={repeatLabel(chore.repeatType)} />
            <InfoRow
              icon="cash-outline"
              label="보상 금액"
              value={
                isRewardReady(chore.rewardPoints)
                  ? `${chore.rewardPoints.toLocaleString('ko-KR')} 도토리`
                  : '산정 중'
              }
              last
            />
          </ContentCard>

          {chore.description ? (
            <View style={{ gap: theme.space.sm }}>
              <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
                상세 설명
              </Text>
              <Text
                style={[
                  theme.typography.bodySm,
                  {
                    backgroundColor: theme.color.background.surfaceSunken,
                    borderRadius: theme.radius.md,
                    padding: theme.space.lg,
                    color: theme.color.text.default,
                  },
                ]}
              >
                {chore.description}
              </Text>
            </View>
          ) : null}
        </View>
      )}

      <DeleteChoreModal
        title={confirming ? (chore?.title ?? null) : null}
        onCancel={() => setConfirming(false)}
        onConfirm={handleDelete}
        loading={deleteChore.isPending}
        isError={deleteChore.isError}
      />
    </Screen>
  );
}

function HeroCard({
  title,
  description,
  status,
}: {
  title: string;
  description: string | null;
  status: TodayChoreStatus | undefined;
}) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.hero,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.lg,
          padding: theme.space.lg,
          gap: theme.space.md,
        },
      ]}
    >
      <View
        style={[
          styles.heroIcon,
          { backgroundColor: theme.color.background.surface, borderRadius: theme.radius.full },
        ]}
      >
        <Image
          source={require('@/assets/images/chore/icon-clipboard.png')}
          style={styles.iconImage}
          resizeMode="contain"
        />
      </View>

      <View style={styles.heroBody}>
        <View style={[styles.titleRow, { gap: theme.space.sm }]}>
          <Text
            numberOfLines={2}
            style={[theme.typography.titleMd, styles.grow, { color: theme.color.text.strong }]}
          >
            {title}
          </Text>
          {status ? (
            <View
              style={[
                styles.badge,
                {
                  backgroundColor: theme.color.background.surface,
                  borderRadius: theme.radius.full,
                },
              ]}
            >
              <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
                {STATUS_LABEL[status]}
              </Text>
            </View>
          ) : null}
        </View>
        {description ? (
          <Text
            numberOfLines={2}
            style={[theme.typography.bodySm, { color: theme.color.text.default }]}
          >
            {description}
          </Text>
        ) : null}
      </View>

      <Image
        source={require('@/assets/images/chore/owl-smile.png')}
        style={styles.mascot}
        resizeMode="contain"
      />
    </View>
  );
}

type InfoIcon =
  'person-outline' | 'calendar-outline' | 'leaf-outline' | 'sync-outline' | 'cash-outline';

function InfoRow({
  icon,
  label,
  value,
  last = false,
}: {
  icon: InfoIcon;
  label: string;
  value: string;
  last?: boolean;
}) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.infoRow,
        {
          gap: theme.space.md,
          paddingVertical: theme.space.lg,
          borderBottomWidth: last ? 0 : 1,
          borderBottomColor: theme.color.border.card,
        },
      ]}
    >
      <View
        style={[
          styles.infoIcon,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
        ]}
      >
        <Ionicons name={icon} size={18} color={theme.color.brand.subtleText} />
      </View>
      <Text style={[theme.typography.body, styles.grow, { color: theme.color.text.default }]}>
        {label}
      </Text>
      <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>{value}</Text>
    </View>
  );
}

/** '2026-09-12' → '9월 12일'. 서버 endDate 는 날짜뿐이라 시각을 붙이지 않는다 */
function formatDate(date: string): string {
  const [, month, day] = date.split('-');
  return `${Number(month)}월 ${Number(day)}일`;
}

function Centered({ message }: { message: string }) {
  const theme = useTheme();

  return (
    <View style={styles.centered}>
      <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  actions: { flexDirection: 'row', alignItems: 'center' },
  hero: { flexDirection: 'row', alignItems: 'center' },
  heroIcon: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center' },
  iconImage: { width: 28, height: 28 },
  heroBody: { flex: 1, gap: 4 },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  badge: { paddingHorizontal: 10, paddingVertical: 3 },
  mascot: { width: 72, height: 72 },
  infoRow: { flexDirection: 'row', alignItems: 'center' },
  infoIcon: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center' },
  grow: { flex: 1 },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
