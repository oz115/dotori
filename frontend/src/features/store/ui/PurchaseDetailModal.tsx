import type { ReactNode } from 'react';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import type { PurchaseHistoryItem } from '@/features/store/model/types';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type PurchaseDetailModalProps = {
  item: PurchaseHistoryItem | null;
  onClose: () => void;
};

/**
 * 구매 내역 한 건의 상세 (SHOP-07). 목록 행이 이미 보여주는 정보를 그대로 크게 보여주는
 * 수준이다 — 백엔드 PurchaseHistoryItem 자체에 더 뽑아낼 필드가 없다(단가·카테고리 없음).
 */
export function PurchaseDetailModal({ item, onClose }: PurchaseDetailModalProps) {
  const theme = useTheme();

  return (
    <Modal visible={item != null} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.canvas,
              borderRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.lg,
            },
          ]}
          onPress={(e) => e.stopPropagation()}
        >
          {item ? (
            <>
              <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
                {item.productName}
              </Text>

              <View
                style={[
                  styles.infoCard,
                  {
                    backgroundColor: theme.color.background.surfaceSunken,
                    borderRadius: theme.radius.lg,
                    padding: theme.space.lg,
                    gap: theme.space.md,
                  },
                ]}
              >
                <InfoRow label="수량">
                  <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                    {item.quantity}개
                  </Text>
                </InfoRow>
                <InfoRow label="결제 금액">
                  <View style={styles.priceRow}>
                    <Image
                      source={require('@/assets/images/icons/icon-dotori.png')}
                      style={styles.acornIcon}
                      resizeMode="contain"
                    />
                    <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                      {item.totalPrice.toLocaleString('ko-KR')}
                    </Text>
                  </View>
                </InfoRow>
                <InfoRow label="구매 일시">
                  <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                    {formatDateTime(item.purchasedAt)}
                  </Text>
                </InfoRow>
              </View>

              <Button label="닫기" variant="outline" onPress={onClose} />
            </>
          ) : null}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  const theme = useTheme();
  return (
    <View style={styles.infoRow}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>{label}</Text>
      {children}
    </View>
  );
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  sheet: { width: '100%' },
  infoCard: {},
  infoRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
});
