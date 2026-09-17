import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useState } from 'react';
import {
  Alert,
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import {
  cancelCompletionErrorMessage,
  requestCompletionErrorMessage,
} from '@/features/chore/lib/choreError';
import { isRewardReady } from '@/features/chore/lib/choreLabels';
import { photoError, photoErrorMessage, toPhotoFile } from '@/features/chore/lib/chorePhoto';
import type { TodayChoreStatus } from '@/features/chore/lib/choreStatus';
import { useCancelCompletion } from '@/features/chore/model/useCancelCompletion';
import { useChore } from '@/features/chore/model/useChore';
import { usePendingCompletionRequest } from '@/features/chore/model/usePendingCompletionRequest';
import { useRequestCompletion } from '@/features/chore/model/useRequestCompletion';
import type { ChorePhotoFile } from '@/features/chore/model/types';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

/** 서버 제약은 255자지만 아이가 쓰는 칸이라 시안대로 200자에서 끊는다 */
const DESCRIPTION_MAX = 200;

type ChildChoreDetailModalProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** null 이면 닫힌 상태 */
  choreId: number | null;
  status: TodayChoreStatus | undefined;
  onClose: () => void;
};

/**
 * 아이의 미션 상세. 상태에 따라 세 모습이다.
 *
 * - TODO: 한 일을 적고 사진을 찍어 완료 요청을 보낸다
 * - PENDING: 보낸 내용을 다시 보고 취소할 수 있다 (사진을 잘못 올렸을 때 되돌리는 유일한 길)
 * - DONE: 이번 주기에 이미 받았다. 읽기 전용
 *
 * 거절당한 요청은 TODO 와 같게 다룬다. 서버가 거절 사유를 저장하지 않아 보여 줄 것이 없고,
 * 같은 주기에 다시 요청할 수 있다.
 *
 * 쓰던 내용·사진은 이 컴포넌트가 들고 있다. 다른 할 일을 열었을 때 앞 내용이 남지 않도록
 * 부르는 쪽이 choreId 를 key 로 넘겨 리마운트시킨다 - effect 로 지우면 한 번 그린 뒤에
 * 다시 그리게 된다.
 */
export function ChildChoreDetailModal({
  familyId,
  childId,
  choreId,
  status,
  onClose,
}: ChildChoreDetailModalProps) {
  const theme = useTheme();
  const [description, setDescription] = useState('');
  const [photo, setPhoto] = useState<ChorePhotoFile | null>(null);

  const { data: chore } = useChore(familyId, childId, choreId ?? undefined);
  const { data: request } = usePendingCompletionRequest(familyId, childId, choreId ?? undefined);
  const requestCompletion = useRequestCompletion(familyId, childId);
  const cancelCompletion = useCancelCompletion(familyId, childId);

  const pickPhoto = async () => {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('카메라를 쓸 수 없어요', '설정에서 카메라 권한을 켜주세요.');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.7 });
    const asset = result.canceled ? undefined : result.assets[0];
    if (!asset) return;

    const invalid = photoError(asset);
    if (invalid) {
      Alert.alert('이 사진은 올릴 수 없어요', photoErrorMessage(invalid));
      return;
    }
    setPhoto(toPhotoFile(asset));
  };

  const submit = () => {
    if (choreId == null || photo == null) return;
    requestCompletion.mutate(
      { choreId, photo, description: description.trim() || null },
      { onSuccess: onClose },
    );
  };

  const confirmCancel = () => {
    if (request == null) return;
    Alert.alert('완료 요청을 취소할까요?', '취소하면 사진을 다시 찍어서 보내야 해요.', [
      { text: '그냥 둘래요', style: 'cancel' },
      {
        text: '취소하기',
        style: 'destructive',
        onPress: () =>
          cancelCompletion.mutate(request.choreCompletionRequestId, { onSuccess: onClose }),
      },
    ]);
  };

  const busy = requestCompletion.isPending || cancelCompletion.isPending;

  // 서버는 보상이 산정되지 않은 할 일의 완료 요청을 400(REWARD_NOT_EVALUATED)으로 막는다.
  // 열어 두면 아이가 글을 쓰고 사진까지 찍은 뒤에야 실패를 알게 되므로 미리 막고 이유를 말한다.
  const rewardPending = chore != null && !isRewardReady(chore.rewardPoints);

  return (
    <Modal visible={choreId != null} transparent animationType="fade" onRequestClose={onClose}>
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
              미션 상세
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

          <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
            <View style={{ gap: theme.space.md }}>
              <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
                {chore?.title ?? '불러오는 중…'}
              </Text>

              {chore ? (
                <View style={[styles.metaRow, { gap: theme.space.sm }]}>
                  <Ionicons name="calendar-outline" size={16} color={theme.color.text.muted} />
                  <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                    {chore.endDate}까지
                  </Text>
                </View>
              ) : null}

              <View style={styles.chipRow}>
                <RewardChip rewardPoints={chore?.rewardPoints ?? null} />
              </View>

              {chore?.description ? (
                <Text
                  style={[
                    theme.typography.bodySm,
                    {
                      backgroundColor: theme.color.background.surfaceSunken,
                      borderRadius: theme.radius.md,
                      padding: theme.space.md,
                      color: theme.color.text.default,
                    },
                  ]}
                >
                  {chore.description}
                </Text>
              ) : null}

              {status === 'DONE' ? (
                <Text style={[theme.typography.body, { color: theme.color.brand.subtleText }]}>
                  이미 다 하고 도토리도 받았어요!
                </Text>
              ) : status === 'PENDING' ? (
                <PendingBody
                  description={request?.description ?? null}
                  photoUrl={request?.photoUrl}
                />
              ) : (
                <>
                  {status === 'REJECTED' ? (
                    <View
                      style={[
                        styles.rejected,
                        {
                          backgroundColor: theme.color.status.danger.subtle,
                          borderRadius: theme.radius.md,
                          padding: theme.space.md,
                          gap: theme.space.xs,
                        },
                      ]}
                    >
                      <Text
                        style={[theme.typography.label, { color: theme.color.status.danger.text }]}
                      >
                        다시 해볼까요?
                      </Text>
                      <Text
                        style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}
                      >
                        부모님이 아직 확인해 주지 않았어요. 사진을 다시 찍어서 보내볼까요?
                      </Text>
                    </View>
                  ) : null}

                  <TodoBody
                    description={description}
                    onDescriptionChange={setDescription}
                    photo={photo}
                    onPickPhoto={pickPhoto}
                  />
                </>
              )}

              {requestCompletion.error ? (
                <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
                  {requestCompletionErrorMessage(requestCompletion.error)}
                </Text>
              ) : null}
              {cancelCompletion.error ? (
                <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
                  {cancelCompletionErrorMessage(cancelCompletion.error)}
                </Text>
              ) : null}
            </View>
          </ScrollView>

          {status === 'DONE' ? null : status === 'PENDING' ? (
            <Button
              label="요청 취소하기"
              variant="outline"
              loading={cancelCompletion.isPending}
              disabled={busy || request == null}
              onPress={confirmCancel}
              style={{ marginTop: theme.space.lg }}
            />
          ) : (
            <>
              {rewardPending ? (
                <Text
                  style={[
                    theme.typography.bodySm,
                    styles.notice,
                    { color: theme.color.text.muted, marginTop: theme.space.md },
                  ]}
                >
                  받을 도토리를 정하고 있어요. 정해지면 완료 요청을 보낼 수 있어요!
                </Text>
              ) : null}
              <Button
                label="완료 요청 보내기"
                loading={requestCompletion.isPending}
                disabled={busy || photo == null || rewardPending}
                onPress={submit}
                style={{ marginTop: theme.space.md }}
              />
            </>
          )}
        </View>
      </View>
    </Modal>
  );
}

/** 아직 요청 전 — 한 일을 적고 사진을 찍는다 */
function TodoBody({
  description,
  onDescriptionChange,
  photo,
  onPickPhoto,
}: {
  description: string;
  onDescriptionChange: (value: string) => void;
  photo: ChorePhotoFile | null;
  onPickPhoto: () => void;
}) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={{ gap: theme.space.xs }}>
        <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
          내가 일을 진행한 내용
        </Text>
        <View
          style={[
            styles.textBox,
            { borderColor: theme.color.border.default, borderRadius: theme.radius.md },
          ]}
        >
          <TextInput
            value={description}
            onChangeText={onDescriptionChange}
            maxLength={DESCRIPTION_MAX}
            multiline
            placeholder="어떻게 했는지 적어볼까요?"
            placeholderTextColor={theme.color.text.muted}
            style={[theme.typography.bodySm, styles.textInput, { color: theme.color.text.default }]}
          />
          <Text
            style={[theme.typography.caption, styles.counter, { color: theme.color.text.muted }]}
          >
            {description.length}/{DESCRIPTION_MAX}
          </Text>
        </View>
      </View>

      <View style={{ gap: theme.space.xs }}>
        <Text style={[theme.typography.label, { color: theme.color.text.label }]}>인증 사진</Text>
        <Pressable
          onPress={onPickPhoto}
          accessibilityRole="button"
          accessibilityLabel="사진 찍기"
          style={[
            styles.photoBox,
            {
              borderColor: photo ? theme.color.brand.solid : theme.color.border.strong,
              borderRadius: theme.radius.md,
              backgroundColor: theme.color.background.surface,
            },
          ]}
        >
          {photo ? (
            <Image source={{ uri: photo.uri }} style={styles.preview} resizeMode="cover" />
          ) : (
            <View style={styles.photoPlaceholder}>
              <Ionicons name="camera" size={28} color={theme.color.text.muted} />
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                완료한 모습을 사진으로 찍어 올려보세요!
              </Text>
            </View>
          )}
        </Pressable>
        {photo ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            사진을 다시 찍으려면 눌러주세요.
          </Text>
        ) : null}
      </View>
    </View>
  );
}

/** 요청을 보내 놓고 부모 확인을 기다리는 중 */
function PendingBody({ description, photoUrl }: { description: string | null; photoUrl?: string }) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <Text style={[theme.typography.body, { color: theme.color.brand.subtleText }]}>
        부모님이 확인하고 있어요.
      </Text>

      {description ? (
        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
            내가 보낸 내용
          </Text>
          <Text
            style={[
              theme.typography.bodySm,
              {
                backgroundColor: theme.color.background.surfaceSunken,
                borderRadius: theme.radius.md,
                padding: theme.space.md,
                color: theme.color.text.default,
              },
            ]}
          >
            {description}
          </Text>
        </View>
      ) : null}

      {photoUrl ? (
        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
            내가 올린 사진
          </Text>
          <Image
            source={{ uri: photoUrl }}
            style={[styles.preview, { borderRadius: theme.radius.md }]}
            resizeMode="cover"
          />
        </View>
      ) : null}
    </View>
  );
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
  metaRow: { flexDirection: 'row', alignItems: 'center' },
  chipRow: { flexDirection: 'row' },
  textBox: { borderWidth: 1, paddingHorizontal: 12, paddingTop: 10, paddingBottom: 6 },
  textInput: { minHeight: 64, textAlignVertical: 'top' },
  counter: { textAlign: 'right' },
  notice: { textAlign: 'center' },
  rejected: { width: '100%' },
  photoBox: { borderWidth: 1.5, borderStyle: 'dashed', overflow: 'hidden' },
  photoPlaceholder: { alignItems: 'center', justifyContent: 'center', gap: 8, paddingVertical: 28 },
  preview: { width: '100%', height: 160 },
});
