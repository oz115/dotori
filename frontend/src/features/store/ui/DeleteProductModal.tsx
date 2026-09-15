import { Ionicons } from '@expo/vector-icons';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type DeleteProductModalProps = {
  visible: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  loading: boolean;
};

/**
 * 상품 삭제 확인 모달 (SHOP-05). 네이티브 Alert 대신 WithdrawButton과 같은 커스텀 Modal을
 * 쓴다 — 마스코트 일러스트·경고 콜아웃까지 들어가는 시안이라 Alert.alert로는 못 만든다.
 */
export function DeleteProductModal({
  visible,
  onCancel,
  onConfirm,
  loading,
}: DeleteProductModalProps) {
  const theme = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.backdrop} onPress={onCancel}>
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
          <Image
            source={require('@/assets/images/store/mascot-delete.png')}
            style={styles.mascot}
            resizeMode="contain"
          />

          <View style={{ gap: theme.space.sm }}>
            <Text
              style={[theme.typography.titleMd, styles.center, { color: theme.color.text.strong }]}
            >
              이 상품을 삭제할까요?
            </Text>
            <Text style={[theme.typography.bodySm, styles.center, { color: theme.color.text.sub }]}>
              삭제된 상품은 상점에서 보이지 않지만, 이미 구매한 내역에는 영향을 주지 않아요. 이
              작업은 되돌릴 수 없어요.
            </Text>
          </View>

          <View
            style={[
              styles.warningRow,
              {
                backgroundColor: theme.color.status.warning.subtle,
                borderRadius: theme.radius.md,
                padding: theme.space.md,
                gap: theme.space.sm,
              },
            ]}
          >
            <Ionicons name="warning" size={18} color={theme.color.status.warning.text} />
            <Text style={[theme.typography.label, { color: theme.color.status.warning.text }]}>
              정말 삭제하시겠어요?
            </Text>
          </View>

          <View style={[styles.actions, { gap: theme.space.sm }]}>
            <Button
              label="취소"
              variant="outline"
              onPress={onCancel}
              disabled={loading}
              style={styles.flex}
            />
            <Button
              label="삭제하기"
              variant="primary"
              onPress={onConfirm}
              loading={loading}
              style={[styles.flex, { backgroundColor: theme.color.status.danger.solid }]}
            />
          </View>
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
  mascot: { width: 120, height: 120, alignSelf: 'center' },
  center: { textAlign: 'center' },
  warningRow: { flexDirection: 'row', alignItems: 'center' },
  actions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
