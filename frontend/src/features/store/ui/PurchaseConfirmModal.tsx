import { Ionicons } from '@expo/vector-icons';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import type { StorePurchaseResult } from '@/features/store/model/types';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type PurchaseConfirmModalProps = {
  visible: boolean;
  productName: string;
  price: number;
  loading: boolean;
  error: string | null;
  /** null 이면 확인 단계, 값이 있으면 구매 완료 단계 (SHOP-06 "구매 완료" 화면). */
  result: StorePurchaseResult | null;
  onCancel: () => void;
  onConfirm: () => void;
  onClose: () => void;
};

/**
 * 구매 확인 → 완료 모달 (SHOP-06). 수량은 항상 1 이라 수량 선택 스텝이 없다.
 * DeleteProductModal과 같은 커스텀 Modal 패턴 — 확인/완료를 한 모달 안에서 전환한다.
 */
export function PurchaseConfirmModal({
  visible,
  productName,
  price,
  loading,
  error,
  result,
  onCancel,
  onConfirm,
  onClose,
}: PurchaseConfirmModalProps) {
  const theme = useTheme();

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={result ? onClose : onCancel}
    >
      <Pressable style={styles.backdrop} onPress={result ? onClose : onCancel}>
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
          {result ? (
            <>
              <Ionicons
                name="checkmark-circle"
                size={72}
                color={theme.color.brand.solid}
                style={styles.center}
              />
              <View style={{ gap: theme.space.sm }}>
                <Text
                  style={[
                    theme.typography.titleMd,
                    styles.center,
                    { color: theme.color.text.strong },
                  ]}
                >
                  구매 완료!
                </Text>
                <Text
                  style={[theme.typography.bodySm, styles.center, { color: theme.color.text.sub }]}
                >
                  {result.productName}을(를) {result.totalPrice.toLocaleString('ko-KR')}P에
                  구매했어요.
                </Text>
              </View>

              <View
                style={[
                  styles.balanceRow,
                  {
                    backgroundColor: theme.color.background.surfaceSunken,
                    borderRadius: theme.radius.md,
                    padding: theme.space.md,
                  },
                ]}
              >
                <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
                  구매 후 남은 도토리
                </Text>
                <View style={styles.priceRow}>
                  <Image
                    source={require('@/assets/images/icons/icon-dotori.png')}
                    style={styles.acornIcon}
                    resizeMode="contain"
                  />
                  <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                    {result.pointBalanceAfter.toLocaleString('ko-KR')}
                  </Text>
                </View>
              </View>

              <Button label="확인" variant="primary" onPress={onClose} />
            </>
          ) : (
            <>
              <Image
                source={require('@/assets/images/icons/icon-dotori.png')}
                style={styles.mascot}
                resizeMode="contain"
              />

              <View style={{ gap: theme.space.sm }}>
                <Text
                  style={[
                    theme.typography.titleMd,
                    styles.center,
                    { color: theme.color.text.strong },
                  ]}
                >
                  {productName}을(를) 구매할까요?
                </Text>
                <View style={styles.priceRow}>
                  <Image
                    source={require('@/assets/images/icons/icon-dotori.png')}
                    style={styles.acornIcon}
                    resizeMode="contain"
                  />
                  <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                    {price.toLocaleString('ko-KR')}
                  </Text>
                </View>
              </View>

              {error ? (
                <Text
                  style={[
                    theme.typography.bodySm,
                    styles.center,
                    { color: theme.color.status.danger.text },
                  ]}
                >
                  {error}
                </Text>
              ) : null}

              {/* 실패(예: 오늘 이미 구매)는 재시도해도 소용없으니 확인 버튼 하나만 둔다 */}
              {error ? (
                <Button label="확인" variant="primary" onPress={onCancel} />
              ) : (
                <View style={[styles.actions, { gap: theme.space.sm }]}>
                  <Button
                    label="취소"
                    variant="outline"
                    onPress={onCancel}
                    disabled={loading}
                    style={styles.flex}
                  />
                  <Button
                    label="구매하기"
                    variant="primary"
                    onPress={onConfirm}
                    loading={loading}
                    style={styles.flex}
                  />
                </View>
              )}
            </>
          )}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  sheet: { width: '100%' },
  mascot: { width: 64, height: 64, alignSelf: 'center' },
  center: { textAlign: 'center' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4, justifyContent: 'center' },
  balanceRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  acornIcon: { width: 16, height: 16 },
  actions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
