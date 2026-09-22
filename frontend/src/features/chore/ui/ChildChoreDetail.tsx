import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image as RNImage,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import {
  cancelCompletionErrorMessage,
  requestCompletionErrorMessage,
} from '@/features/chore/lib/choreError';
import { deadlineLabel, isRewardReady } from '@/features/chore/lib/choreLabels';
import {
  compressPhoto,
  photoError,
  photoErrorMessage,
  toPhotoFile,
} from '@/features/chore/lib/chorePhoto';
import type { ChorePhotoFile } from '@/features/chore/model/types';
import { useCancelCompletion } from '@/features/chore/model/useCancelCompletion';
import { useChore } from '@/features/chore/model/useChore';
import { useChoreStatus } from '@/features/chore/model/useChoreStatus';
import { usePendingCompletionRequest } from '@/features/chore/model/usePendingCompletionRequest';
import { useRequestCompletion } from '@/features/chore/model/useRequestCompletion';
import { ChoreStatusBadge } from '@/features/chore/ui/ChoreStatusBadge';
import { RepeatBadge } from '@/features/chore/ui/RepeatBadge';
import { RewardChip } from '@/features/chore/ui/RewardChip';
import { useTheme } from '@/shared/theme';
import { Button, ContentCard, Header, Screen } from '@/shared/ui';

/** 서버 제약은 255자지만 아이가 쓰는 칸이라 시안대로 200자에서 끊는다 */
const DESCRIPTION_MAX = 200;

type ChildChoreDetailProps = {
  familyId: number | undefined;
  childId: number | undefined;
  choreId: number | undefined;
  /**
   * 뒤로가기·완료 요청을 보낸 뒤 갈 곳. 라우트가 반드시 정한다 - 어디서 들어왔는지는
   * 라우트만 알고(목록·홈·알림), 이 화면은 경로를 몰라야 한다.
   */
  onBack: () => void;
};

/**
 * 아이의 미션 상세 (화면). 상태에 따라 세 모습이다.
 *
 * - TODO·REJECTED: 한 일을 적고 사진을 찍어 완료 요청을 보낸다
 * - PENDING: 보낸 내용을 다시 보고 취소할 수 있다 (사진을 잘못 올렸을 때 되돌리는 유일한 길)
 * - DONE: 이번 주기에 이미 받았다. 읽기 전용 — 반복 미션이라 다음 주기가 오면 다시 열린다
 *
 * <p>모달이었다가 화면이 됐다 (QA). 팝업 안에서 글을 쓰면 키보드가 입력칸을 덮었고, 바깥을
 * 잘못 누르거나 뒤로가기를 누르면 쓰던 내용과 찍은 사진이 통째로 날아갔다. 화면이면 그 사고가
 * 구조적으로 사라지고, 부모 상세와 같은 모양이 된다.
 *
 * <p>지워진 미션은 아무것도 받지 않는다. 서버도 막지만(INVALID_REQUEST) 글을 쓰고 사진까지
 * 찍은 뒤에 실패를 알려 주게 되므로 여기서 먼저 끊고 이유를 말한다.
 */
export function ChildChoreDetail({ familyId, childId, choreId, onBack }: ChildChoreDetailProps) {
  const theme = useTheme();
  const [description, setDescription] = useState('');
  const [photo, setPhoto] = useState<ChorePhotoFile | null>(null);
  const [preparingPhoto, setPreparingPhoto] = useState(false);

  const { data: chore, isPending, isError } = useChore(familyId, childId, choreId);
  const status = useChoreStatus(familyId, childId, chore);
  const { data: request } = usePendingCompletionRequest(familyId, childId, choreId);
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

    // 검사는 줄인 사진으로 한다. 원본이 10MB 를 넘어도 줄이면 통과하는 경우가 대부분이라,
    // 원본으로 먼저 막으면 올릴 수 있는 사진을 돌려보내게 된다.
    setPreparingPhoto(true);
    const compressed = await compressPhoto(asset).finally(() => setPreparingPhoto(false));

    const invalid = photoError(compressed);
    if (invalid) {
      Alert.alert('이 사진은 올릴 수 없어요', photoErrorMessage(invalid));
      return;
    }
    setPhoto(toPhotoFile(compressed));
  };

  const submit = () => {
    if (choreId == null || photo == null) return;
    requestCompletion.mutate(
      { choreId, photo, description: description.trim() || null },
      { onSuccess: onBack },
    );
  };

  const confirmCancel = () => {
    if (request == null) return;
    Alert.alert('완료 요청을 취소할까요?', '취소하면 사진을 다시 찍어서 보내야 해요.', [
      { text: '그냥 둘래요', style: 'cancel' },
      {
        text: '취소하기',
        style: 'destructive',
        onPress: () => cancelCompletion.mutate(request.choreCompletionRequestId),
      },
    ]);
  };

  const busy = requestCompletion.isPending || cancelCompletion.isPending || preparingPhoto;

  // 서버는 보상이 산정되지 않은 미션의 완료 요청을 400(REWARD_NOT_EVALUATED)으로 막는다.
  const rewardPending = chore != null && !isRewardReady(chore.rewardPoints);
  const deleted = chore?.deletedAt != null;
  const canSubmit = !deleted && (status === 'TODO' || status === 'REJECTED');

  return (
    // 사진 미리보기·설명 입력까지 들어가면 한 화면을 쉽게 넘어 끝의 버튼에 손이 닿지 않았다.
    <Screen header={<Header title="미션 상세" onBack={onBack} />} scroll keyboardAvoiding>
      {isPending ? (
        <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
          미션을 불러오는 중이에요.
        </Text>
      ) : isError || chore == null ? (
        <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>
          미션을 불러오지 못했어요.
        </Text>
      ) : (
        <View style={{ gap: theme.space.lg }}>
          <View style={{ gap: theme.space.md }}>
            <View style={[styles.row, { gap: theme.space.xs }]}>
              <RepeatBadge repeatType={chore.repeatType} />
              {status ? <ChoreStatusBadge status={status} /> : null}
            </View>

            <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>
              {chore.title}
            </Text>

            {/* 기한은 한번짜리에만 보여 준다. 반복 미션은 주기가 돌면 endDate 가 다음 기간으로
                밀려서 지금 날짜가 마감을 뜻하지 않는다 (목록과 같은 규칙). */}
            {chore.repeatType === 'ONCE' ? (
              <View style={[styles.row, { gap: theme.space.sm }]}>
                <Ionicons name="calendar-outline" size={16} color={theme.color.text.muted} />
                <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                  {deadlineLabel(chore.endDate)}
                </Text>
              </View>
            ) : null}

            <RewardChip rewardPoints={chore.rewardPoints} />
          </View>

          {/* 부모가 남긴 설명. 무엇을 어떻게 해야 하는지가 여기 적혀 있어서 아이가 가장
              먼저 읽어야 하는 글이다 (QA - 상세에서 안 보인다고 올라왔다). */}
          <ContentCard style={{ padding: theme.space.lg, gap: theme.space.xs }}>
            <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
              부모님이 남긴 설명
            </Text>
            <Text
              style={[
                theme.typography.body,
                {
                  color: chore.description ? theme.color.text.default : theme.color.text.muted,
                },
              ]}
            >
              {chore.description ?? '따로 남긴 설명은 없어요.'}
            </Text>
          </ContentCard>

          {deleted ? (
            <Notice
              tone="danger"
              title="지워진 미션이에요"
              body={'부모님이 이 미션을 지우셨어요.\n완료 요청은 보낼 수 없어요.'}
            />
          ) : status === 'DONE' ? (
            <Notice
              tone="brand"
              title="이미 다 했어요!"
              body={
                chore.repeatType === 'ONCE'
                  ? '도토리도 받았어요.'
                  : '도토리도 받았어요. 다음 주기가 되면 다시 할 수 있어요.'
              }
            />
          ) : status === 'PENDING' ? (
            <PendingBody description={request?.description ?? null} photoUrl={request?.photoUrl} />
          ) : (
            <>
              {/* 거절은 부모가 "아직 안 본" 것이 아니라 보고 나서 돌려보낸 것이다. 이유는
                  서버가 저장하지 않아 보여 줄 수 없으므로 무엇을 하면 되는지만 알려 준다. */}
              {status === 'REJECTED' ? (
                <Notice
                  tone="danger"
                  title="다시 해볼까요?"
                  body={'부모님이 확인하고 다시 해보자고 하셨어요.\n사진을 새로 찍어서 보내주세요.'}
                />
              ) : null}

              <TodoBody
                description={description}
                onDescriptionChange={setDescription}
                photo={photo}
                preparingPhoto={preparingPhoto}
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

          {status === 'PENDING' && !deleted ? (
            <Button
              label="요청 취소하기"
              variant="outline"
              loading={cancelCompletion.isPending}
              disabled={busy || request == null}
              onPress={confirmCancel}
            />
          ) : canSubmit ? (
            <>
              {rewardPending ? (
                <Text
                  style={[
                    theme.typography.bodySm,
                    styles.notice,
                    { color: theme.color.text.muted },
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
              />
            </>
          ) : null}
        </View>
      )}
    </Screen>
  );
}

/** 상태 안내 상자 (지워짐·완료·거절) */
function Notice({ tone, title, body }: { tone: 'danger' | 'brand'; title: string; body: string }) {
  const theme = useTheme();
  const color =
    tone === 'danger'
      ? { bg: theme.color.status.danger.subtle, text: theme.color.status.danger.text }
      : { bg: theme.color.brand.subtle, text: theme.color.brand.subtleText };

  return (
    <View
      style={{
        backgroundColor: color.bg,
        borderRadius: theme.radius.md,
        padding: theme.space.lg,
        gap: theme.space.xs,
      }}
    >
      <Text style={[theme.typography.label, { color: color.text }]}>{title}</Text>
      <Text style={[theme.typography.bodySm, { color: color.text }]}>{body}</Text>
    </View>
  );
}

/** 아직 요청 전 — 한 일을 적고 사진을 찍는다 */
function TodoBody({
  description,
  onDescriptionChange,
  photo,
  preparingPhoto,
  onPickPhoto,
}: {
  description: string;
  onDescriptionChange: (value: string) => void;
  photo: ChorePhotoFile | null;
  /** 찍은 사진을 줄이는 중. 몇 초 걸려서 아무 말이 없으면 안 찍힌 줄 안다 */
  preparingPhoto: boolean;
  onPickPhoto: () => void;
}) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.lg }}>
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
          {preparingPhoto ? (
            <View style={styles.photoPlaceholder}>
              <ActivityIndicator color={theme.color.brand.solid} />
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                사진을 준비하고 있어요…
              </Text>
            </View>
          ) : photo ? (
            <RNImage source={{ uri: photo.uri }} style={styles.preview} resizeMode="cover" />
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
    <View style={{ gap: theme.space.lg }}>
      <Notice tone="brand" title="부모님이 확인하고 있어요" body="조금만 기다려 주세요!" />

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
            contentFit="cover"
            transition={120}
          />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  textBox: { borderWidth: 1, paddingHorizontal: 12, paddingTop: 10, paddingBottom: 6 },
  textInput: { minHeight: 96, textAlignVertical: 'top' },
  counter: { textAlign: 'right' },
  notice: { textAlign: 'center' },
  photoBox: { borderWidth: 1.5, borderStyle: 'dashed', overflow: 'hidden' },
  photoPlaceholder: { alignItems: 'center', justifyContent: 'center', gap: 8, paddingVertical: 36 },
  preview: { width: '100%', height: 200 },
});
