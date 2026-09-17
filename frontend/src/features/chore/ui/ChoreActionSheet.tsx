import { Ionicons } from '@expo/vector-icons';
import { Modal, Pressable, StyleSheet, Text } from 'react-native';

import { useTheme } from '@/shared/theme';

type ChoreActionSheetProps = {
  /** null 이면 닫힌 상태. 열려 있으면 어떤 할 일인지 제목으로 보여 준다 */
  title: string | null;
  onEdit: () => void;
  onDelete: () => void;
  onClose: () => void;
};

/**
 * 목록 행의 더보기(수정·삭제) 시트.
 *
 * 시안은 행에 붙는 드롭다운이지만 아래에서 올라오는 시트로 만들었다 - 드롭다운은 행 위치에
 * 맞춰 띄워야 해서 목록이 스크롤되면 어긋나고, 화면 끝 행에서는 잘린다. 손가락이 닿는
 * 면적도 이쪽이 넓어 초등학생 사용자에게 낫다.
 */
export function ChoreActionSheet({ title, onEdit, onDelete, onClose }: ChoreActionSheetProps) {
  const theme = useTheme();

  return (
    <Modal visible={title != null} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable
        style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}
        onPress={onClose}
        accessibilityRole="button"
        accessibilityLabel="닫기"
      >
        {/* 시트 안을 눌렀을 때 닫히지 않도록 이벤트를 여기서 멈춘다 */}
        <Pressable
          onPress={(event) => event.stopPropagation()}
          style={[
            styles.sheet,
            {
              backgroundColor: theme.color.background.surface,
              borderTopLeftRadius: theme.radius['2xl'],
              borderTopRightRadius: theme.radius['2xl'],
              paddingHorizontal: theme.space.xl,
              paddingTop: theme.space.lg,
              paddingBottom: theme.space['3xl'],
              gap: theme.space.xs,
            },
          ]}
        >
          <Text
            numberOfLines={1}
            style={[theme.typography.label, { color: theme.color.text.muted }]}
          >
            {title}
          </Text>

          <ActionRow icon="pencil" label="수정하기" onPress={onEdit} />
          <ActionRow icon="trash" label="삭제하기" onPress={onDelete} danger />
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function ActionRow({
  icon,
  label,
  onPress,
  danger = false,
}: {
  icon: 'pencil' | 'trash';
  label: string;
  onPress: () => void;
  danger?: boolean;
}) {
  const theme = useTheme();
  const color = danger ? theme.color.status.danger.text : theme.color.text.default;

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={({ pressed }) => [
        styles.row,
        {
          gap: theme.space.md,
          borderRadius: theme.radius.md,
          backgroundColor: pressed ? theme.color.background.surfaceSunken : 'transparent',
        },
      ]}
    >
      <Ionicons name={icon} size={20} color={color} />
      <Text style={[theme.typography.body, { color }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'flex-end' },
  sheet: { width: '100%' },
  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, paddingHorizontal: 8 },
});
