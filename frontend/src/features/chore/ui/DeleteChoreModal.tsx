import { Ionicons } from '@expo/vector-icons';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type DeleteChoreModalProps = {
  /** 지울 할 일 제목. null 이면 닫힌 상태 */
  title: string | null;
  onCancel: () => void;
  onConfirm: () => void;
  loading: boolean;
  isError: boolean;
};

/**
 * 할 일 삭제 확인 (상품 삭제 모달과 같은 틀).
 *
 * 문구가 "안 보이게 된다"가 아니라 두 가지를 나눠 말한다. 서버가 소프트 삭제라서
 * 아이 목록에서는 사라지지만 이미 올라온 완료 요청은 계속 승인·거절할 수 있고, 지급된
 * 이력도 그대로 남는다. 부모가 "이력까지 지워진다"고 오해하면 지우기를 주저한다.
 */
export function DeleteChoreModal({
  title,
  onCancel,
  onConfirm,
  loading,
  isError,
}: DeleteChoreModalProps) {
  const theme = useTheme();

  return (
    <Modal visible={title != null} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable
        style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}
        onPress={onCancel}
      >
        <Pressable
          onPress={(event) => event.stopPropagation()}
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.canvas,
              borderRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.lg,
            },
          ]}
        >
          <Image
            source={require('@/assets/images/chore/squirrel-think.png')}
            style={styles.mascot}
            resizeMode="contain"
          />

          <View style={{ gap: theme.space.sm }}>
            <Text
              numberOfLines={2}
              style={[theme.typography.titleMd, styles.center, { color: theme.color.text.strong }]}
            >
              &lsquo;{title}&rsquo; 을(를) 삭제할까요?
            </Text>
            <Text style={[theme.typography.bodySm, styles.center, { color: theme.color.text.sub }]}>
              아이의 할 일 목록에서 사라져요. 이미 받은 도토리와 완료 이력은 그대로 남고, 올라와
              있는 완료 요청은 계속 확인할 수 있어요.
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
              이 작업은 되돌릴 수 없어요.
            </Text>
          </View>

          {isError ? (
            <Text
              style={[
                theme.typography.bodySm,
                styles.center,
                { color: theme.color.status.danger.text },
              ]}
            >
              삭제하지 못했어요. 잠시 후 다시 시도해주세요.
            </Text>
          ) : null}

          <View style={[styles.actions, { gap: theme.space.sm }]}>
            <Button
              label="그냥 둘래요"
              variant="outline"
              onPress={onCancel}
              disabled={loading}
              style={styles.flex}
            />
            <Button
              label="삭제하기"
              tone="danger"
              onPress={onConfirm}
              loading={loading}
              style={styles.flex}
            />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
  sheet: { width: '100%' },
  mascot: { width: 110, height: 110, alignSelf: 'center' },
  center: { textAlign: 'center' },
  warningRow: { flexDirection: 'row', alignItems: 'center' },
  actions: { flexDirection: 'row' },
  flex: { flex: 1 },
});
