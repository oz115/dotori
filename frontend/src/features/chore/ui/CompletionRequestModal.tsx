import { Ionicons } from '@expo/vector-icons';
import { Image, Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { reviewCompletionErrorMessage } from '@/features/chore/lib/choreError';
import { useApproveCompletion } from '@/features/chore/model/useApproveCompletion';
import { useRejectCompletion } from '@/features/chore/model/useRejectCompletion';
import type { ParentCompletionRequest } from '@/features/chore/model/useChildrenPendingRequests';
import { ChildBadge } from '@/features/chore/ui/ChildBadge';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type CompletionRequestModalProps = {
  familyId: number | undefined;
  /** null 이면 닫힌 상태 */
  request: ParentCompletionRequest | null;
  onClose: () => void;
  /** 승인으로 도토리가 지급되면 알린다. 지갑은 다른 슬라이스라 라우트가 갱신한다 */
  onApproved?: (childId: number, rewardPoints: number) => void;
};

/**
 * 완료 요청 상세 · 승인/거절 (부모).
 *
 * 사진이 이 화면의 본체다. 부모가 판단할 근거가 사진과 아이가 쓴 몇 줄뿐이라 크게 띄운다.
 *
 * 거절에 사유를 받지 않는다. 서버에 담을 자리가 없어서 입력받아 봐야 버려진다 - 받는 척
 * 하느니 안 받는 편이 낫다. 대신 거절해도 아이가 다시 요청할 수 있다는 것을 알린다.
 */
export function CompletionRequestModal({
  familyId,
  request,
  onClose,
  onApproved,
}: CompletionRequestModalProps) {
  const theme = useTheme();
  const approve = useApproveCompletion(familyId, request?.childId);
  const reject = useRejectCompletion(familyId, request?.childId);

  const busy = approve.isPending || reject.isPending;

  const handleApprove = () => {
    if (request == null) return;
    approve.mutate(request.choreCompletionRequestId, {
      onSuccess: (result) => {
        onApproved?.(request.childId, result.rewardPoints);
        onClose();
      },
    });
  };

  const handleReject = () => {
    if (request == null) return;
    reject.mutate(request.choreCompletionRequestId, { onSuccess: onClose });
  };

  return (
    <Modal visible={request != null} transparent animationType="fade" onRequestClose={onClose}>
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
              완료 요청
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
              {request?.photoUrl ? (
                <Image
                  source={{ uri: request.photoUrl }}
                  style={[styles.photo, { borderRadius: theme.radius.md }]}
                  resizeMode="cover"
                />
              ) : null}

              <View style={[styles.headRow, { gap: theme.space.sm }]}>
                <Text
                  numberOfLines={2}
                  style={[theme.typography.h3, styles.grow, { color: theme.color.text.strong }]}
                >
                  {request?.choreTitle}
                </Text>
                {request ? <ChildBadge name={request.childName} /> : null}
              </View>

              {request ? (
                <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                  {formatRequestedAt(request.requestedAt)}에 보냈어요
                </Text>
              ) : null}

              <Text
                style={[
                  theme.typography.bodySm,
                  {
                    backgroundColor: theme.color.background.surfaceSunken,
                    borderRadius: theme.radius.md,
                    padding: theme.space.md,
                    color: request?.description ? theme.color.text.default : theme.color.text.muted,
                  },
                ]}
              >
                {request?.description ?? '남긴 말 없이 사진만 보냈어요.'}
              </Text>

              {approve.error || reject.error ? (
                <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
                  {reviewCompletionErrorMessage(approve.error ?? reject.error)}
                </Text>
              ) : null}
            </View>
          </ScrollView>

          <Text
            style={[
              theme.typography.caption,
              styles.note,
              { color: theme.color.text.muted, marginTop: theme.space.md },
            ]}
          >
            거절해도 아이가 다시 요청할 수 있어요.
          </Text>

          <View style={[styles.actions, { gap: theme.space.sm, marginTop: theme.space.sm }]}>
            <Button
              label="거절하기"
              variant="outline"
              tone="danger"
              loading={reject.isPending}
              disabled={busy}
              onPress={handleReject}
              style={styles.flex}
            />
            <Button
              label="승인하기"
              loading={approve.isPending}
              disabled={busy}
              onPress={handleApprove}
              style={styles.flex}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
}

/** '9월 15일 오후 6:23' — 기기 로컬 시간 */
function formatRequestedAt(requestedAt: string): string {
  return new Date(requestedAt).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
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
  headRow: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  note: { textAlign: 'center' },
  actions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
