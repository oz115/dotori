import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import type { CompletionPhoto } from '@/features/chore/lib/choreHistory';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import { useTheme } from '@/shared/theme';
import { Button, ChildBadge } from '@/shared/ui';

export type CompletionHistoryDetail = {
  choreTitle: string;
  /** 부모 화면에서만 넘긴다. 아이는 자기 것만 보므로 이름이 필요 없다 */
  childName?: string;
  processedAt: string;
  rewardPoints: number;
  /** 그때 보낸 사진·글. 짝을 못 찾으면 null (choreHistory 의 matchCompletionPhotos) */
  photo: CompletionPhoto | null;
};

/**
 * 완료 이력 한 건의 상세 — 그때 보낸 사진과 쓴 내용.
 *
 * 전에는 목록이 제목·시각·도토리만 보여 주고 눌러도 아무 일이 없었다. 무엇을 해서 받았는지
 * 다시 볼 방법이 아예 없었다 (QA).
 *
 * <p>사진은 목록(`/completions`)이 아니라 승인된 완료 요청에서 온다. 짝을 못 찾는 건도
 * 있어서(30일 밖의 오래된 요청 등) 그때는 사진 자리에 사정을 적는다 - 목록 행을 눌렀는데
 * 아무 반응이 없는 것보다 낫다.
 */
export function CompletionHistoryModal({
  detail,
  onClose,
}: {
  /** null 이면 닫힌 상태 */
  detail: CompletionHistoryDetail | null;
  onClose: () => void;
}) {
  const theme = useTheme();

  return (
    <Modal visible={detail != null} transparent animationType="fade" onRequestClose={onClose}>
      <View style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}>
        <View
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.surface,
              borderRadius: theme.radius['2xl'],
              padding: theme.space.xl,
            },
          ]}
        >
          <View style={styles.titleRow}>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              완료한 미션
            </Text>
            <Pressable
              onPress={onClose}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel="닫기"
            >
              <Ionicons name="close" size={24} color={theme.color.text.muted} />
            </Pressable>
          </View>

          <ScrollView showsVerticalScrollIndicator={false}>
            <View style={{ gap: theme.space.md }}>
              {detail?.photo ? (
                <Image
                  source={{
                    uri: detail.photo.photoUrl,
                    // 대기함 썸네일과 같은 키 — 부모가 승인할 때 이미 받아 둔 사진을 재사용한다
                    cacheKey: `chore-request-${detail.photo.requestId}`,
                  }}
                  style={[styles.photo, { borderRadius: theme.radius.md }]}
                  contentFit="cover"
                  transition={120}
                />
              ) : (
                <View
                  style={[
                    styles.noPhoto,
                    {
                      backgroundColor: theme.color.background.surfaceSunken,
                      borderRadius: theme.radius.md,
                    },
                  ]}
                >
                  <Ionicons name="image-outline" size={28} color={theme.color.text.muted} />
                  <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                    그때 보낸 사진을 찾지 못했어요.
                  </Text>
                </View>
              )}

              <View style={[styles.headRow, { gap: theme.space.sm }]}>
                <Text
                  numberOfLines={2}
                  style={[theme.typography.h3, styles.grow, { color: theme.color.text.strong }]}
                >
                  {detail?.choreTitle}
                </Text>
                {detail?.childName ? <ChildBadge name={detail.childName} /> : null}
              </View>

              <View style={[styles.headRow, { gap: theme.space.sm }]}>
                <Text
                  style={[theme.typography.caption, styles.grow, { color: theme.color.text.muted }]}
                >
                  {detail ? `${formatProcessedAt(detail.processedAt)}에 받았어요` : ''}
                </Text>
                {detail ? <RewardChip rewardPoints={detail.rewardPoints} compact /> : null}
              </View>

              <Text
                style={[
                  theme.typography.bodySm,
                  {
                    backgroundColor: theme.color.background.surfaceSunken,
                    borderRadius: theme.radius.md,
                    padding: theme.space.md,
                    color: detail?.photo?.description
                      ? theme.color.text.default
                      : theme.color.text.muted,
                  },
                ]}
              >
                {detail?.photo?.description ?? '남긴 말 없이 사진만 보냈어요.'}
              </Text>
            </View>
          </ScrollView>

          <Button label="닫기" variant="outline" onPress={onClose} style={styles.closeButton} />
        </View>
      </View>
    </Modal>
  );
}

/** '9월 12일 (토) 오후 6:23' — 목록 행과 같은 표기 */
function formatProcessedAt(processedAt: string): string {
  return new Date(processedAt).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
    hour: 'numeric',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
  sheet: { maxHeight: '85%' },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  photo: { width: '100%', height: 200 },
  noPhoto: { height: 120, alignItems: 'center', justifyContent: 'center', gap: 8 },
  headRow: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  closeButton: { marginTop: 16 },
});
