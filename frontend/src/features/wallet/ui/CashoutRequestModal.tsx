import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { cashoutResultNote, isPendingCashout } from '@/features/wallet/lib/cashoutStatus';
import { reviewCashoutErrorMessage } from '@/features/wallet/lib/walletError';
import { useApproveCashout } from '@/features/wallet/model/useApproveCashout';
import { useDeclineCashout } from '@/features/wallet/model/useDeclineCashout';
import type { ParentCashoutRequest } from '@/features/wallet/model/useChildrenCashoutRequests';
import { CashoutStatusBadge } from '@/features/wallet/ui/CashoutStatusBadge';
import { CashoutStatusIcon } from '@/features/wallet/ui/CashoutStatusIcon';
import { formatPoint, formatWon, pointToWon } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type CashoutRequestModalProps = {
  familyId: number | undefined;
  /** null 이면 닫힌 상태 */
  request: ParentCashoutRequest | null;
  /**
   * 지금 보고 있는 부모의 userId. 출금 계좌는 `request.parentId` 의 것이라 로그인한
   * 부모와 다를 수 있어(가족에 보호자가 둘이면 서버가 첫 번째 부모를 출금자로 잡는다),
   * "내 계좌"라고 단정하기 전에 이 값과 맞춰 본다.
   */
  viewerId?: number;
  onClose: () => void;
  /** 승인으로 부모 계좌에서 돈이 나가면 알린다. 계좌는 다른 슬라이스라 라우트가 갱신한다 */
  onApproved?: (childId: number, pointAmount: number) => void;
};

/**
 * 환전 요청 상세 · 승인/거절 (부모).
 *
 * 화면은 위에서 아래로 "얼마가 → 누구 사이에서 → 어떻게 됐나" 순으로 읽히게 짰다.
 * 금액을 가장 크게 한 번만 보여주고(도토리와 원을 나란히), 나머지 사실은 라벨-값 행으로
 * 내려 둔다 — 승인은 이 앱에서 부모가 누를 수 있는 가장 무거운 버튼이라(누르는 순간 실제
 * 계좌가 움직이고 되돌릴 수 없다) 금액이 한눈에 안 들어오면 안 된다.
 *
 * 지난 내역의 행도 이 모달로 열린다. 이미 끝난 요청은 버튼 대신 결과 한 줄만 보여준다 —
 * 처리된 요청을 다시 누를 수 있으면 부모는 두 번 나갔는지 의심하게 된다.
 */
export function CashoutRequestModal({
  familyId,
  request,
  viewerId,
  onClose,
  onApproved,
}: CashoutRequestModalProps) {
  const theme = useTheme();
  const approve = useApproveCashout(familyId, request?.childId);
  const decline = useDeclineCashout(familyId, request?.childId);

  const busy = approve.isPending || decline.isPending;
  const pending = request != null && isPendingCashout(request.status);
  const cash = request == null ? 0 : pointToWon(request.pointAmount);
  const fromMyAccount = request != null && viewerId != null && request.parentId === viewerId;
  const error = approve.error ?? decline.error;

  const handleApprove = () => {
    if (request == null) return;
    approve.mutate(request.cashoutRequestId, {
      onSuccess: () => {
        onApproved?.(request.childId, request.pointAmount);
        onClose();
      },
    });
  };

  const handleDecline = () => {
    if (request == null) return;
    decline.mutate(request.cashoutRequestId, { onSuccess: onClose });
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
              padding: theme.space['2xl'],
            },
          ]}
        >
          <View style={styles.titleRow}>
            <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>환전 요청</Text>
            <Pressable
              onPress={onClose}
              hitSlop={8}
              accessibilityRole="button"
              accessibilityLabel="닫기"
            >
              <Ionicons name="close" size={26} color={theme.color.text.muted} />
            </Pressable>
          </View>

          <ScrollView showsVerticalScrollIndicator={false}>
            {/* 금액 — 도토리에서 원으로 바뀐다는 것이 한 줄로 읽히게 나란히 놓는다 */}
            <View style={[styles.amountBlock, { paddingVertical: theme.space.xl }]}>
              <View style={[styles.convertRow, { gap: theme.space.xs }]}>
                <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
                  {request == null ? '' : formatPoint(request.pointAmount)}
                </Text>
                <Ionicons name="arrow-forward" size={18} color={theme.color.text.muted} />
              </View>
              <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>
                {formatWon(cash)}
              </Text>
            </View>

            <View style={[styles.divider, { borderTopColor: theme.color.border.default }]} />

            <View style={{ paddingVertical: theme.space.lg, gap: theme.space.md }}>
              <InfoRow label="보내는 사람" value={fromMyAccount ? '나 (내 계좌)' : '보호자 계좌'} />
              <InfoRow label="받는 사람" value={request?.childName ?? ''} />
              <InfoRow
                label="요청 시각"
                value={request == null ? '' : formatDateTime(request.requestedAt)}
              />
              {request?.processedAt ? (
                <InfoRow label="처리 시각" value={formatDateTime(request.processedAt)} />
              ) : null}
              <InfoRow
                label="상태"
                value={
                  request ? (
                    <View style={[styles.statusValue, { gap: theme.space.sm }]}>
                      <CashoutStatusIcon status={request.status} size={22} />
                      <CashoutStatusBadge status={request.status} />
                    </View>
                  ) : null
                }
              />
            </View>

            <View style={[styles.divider, { borderTopColor: theme.color.border.default }]} />

            <View style={{ paddingTop: theme.space.lg, gap: theme.space.sm }}>
              {pending ? (
                <Text style={[theme.typography.body, { color: theme.color.text.default }]}>
                  승인하면 {fromMyAccount ? '내 계좌' : '보호자 계좌'}에서 {formatWon(cash)}이{' '}
                  {request?.childName}의 계좌로 바로 이체돼요.
                </Text>
              ) : (
                <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
                  {request ? cashoutResultNote(request.status) : ''}
                </Text>
              )}

              {error ? (
                <Text style={[theme.typography.body, { color: theme.color.status.danger.text }]}>
                  {reviewCashoutErrorMessage(error)}
                </Text>
              ) : null}
            </View>
          </ScrollView>

          {pending ? (
            <>
              <Text
                style={[
                  theme.typography.caption,
                  styles.note,
                  { color: theme.color.text.muted, marginTop: theme.space.lg },
                ]}
              >
                거절하면 아이에게 도토리를 그대로 돌려줘요.
              </Text>

              <View style={[styles.actions, { gap: theme.space.md, marginTop: theme.space.md }]}>
                <Button
                  label="거절하기"
                  variant="outline"
                  tone="danger"
                  loading={decline.isPending}
                  disabled={busy}
                  onPress={handleDecline}
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
            </>
          ) : (
            <View style={{ marginTop: theme.space.lg }}>
              <Button label="닫기" variant="ghost" onPress={onClose} />
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
}

/** 라벨은 왼쪽, 값은 오른쪽. 값 자리에는 배지 같은 요소도 들어간다 */
function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  const theme = useTheme();

  return (
    <View style={[styles.infoRow, { gap: theme.space.md }]}>
      <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{label}</Text>
      {typeof value === 'string' ? (
        <Text
          numberOfLines={1}
          style={[theme.typography.body, styles.infoValue, { color: theme.color.text.strong }]}
        >
          {value}
        </Text>
      ) : (
        value
      )}
    </View>
  );
}

/** '9월 15일 오후 6:23' — 기기 로컬 시간 */
function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'center', paddingHorizontal: 20 },
  sheet: { maxHeight: '85%' },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  amountBlock: { alignItems: 'center', gap: 4 },
  convertRow: { flexDirection: 'row', alignItems: 'center' },
  divider: { borderTopWidth: 1 },
  infoRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  statusValue: { flexDirection: 'row', alignItems: 'center' },
  infoValue: { flexShrink: 1, textAlign: 'right' },
  note: { textAlign: 'center' },
  actions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
