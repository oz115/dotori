import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { saveChoreErrorMessage } from '@/features/chore/lib/choreError';
import {
  toUpdateBody,
  validateChoreForm,
  type ChoreFormErrors,
  type ChoreFormInput,
} from '@/features/chore/lib/choreForm';
import { useChore } from '@/features/chore/model/useChore';
import { useUpdateChore } from '@/features/chore/model/useUpdateChore';
import type { ChoreDetail } from '@/features/chore/model/types';
import { ChoreFormFields } from '@/features/chore/ui/ChoreFormFields';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

type ParentChoreEditFormProps = {
  familyId: number | undefined;
  childId: number | undefined;
  choreId: number | undefined;
};

/** 오늘 (YYYY-MM-DD), 기기 로컬 시간 기준 */
function todayDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function toFormInput(chore: ChoreDetail): ChoreFormInput {
  return {
    childId: chore.childId,
    title: chore.title,
    description: chore.description ?? '',
    difficulty: chore.difficulty,
    repeatType: chore.repeatType,
    endDate: chore.endDate,
  };
}

/**
 * 할 일 수정 (부모 전용).
 *
 * 등록과 달리 보상을 고칠 수 있다 - PATCH 는 rewardPoints 를 받는다. AI 가 매긴 값이
 * 마음에 안 들 때 부모가 조정하는 자리다. 시안의 "AI 가격 추천" 버튼은 뺐다. 다시
 * 산정해 달라고 부탁할 엔드포인트가 없어서 눌러도 할 일이 없다.
 *
 * 담당 아이는 바꿀 수 없어 고르는 칸을 두지 않는다 - 서버가 받지 않는다. 옮기면 이미
 * 쌓인 완료 이력이 누구 것인지 모호해진다.
 *
 * 저장은 바뀐 필드만 보낸다(PATCH). 아무것도 안 바꿨으면 요청 없이 그냥 돌아간다.
 */
export function ParentChoreEditForm({ familyId, childId, choreId }: ParentChoreEditFormProps) {
  const today = todayDate();
  const { data: chore, isPending, isError } = useChore(familyId, childId, choreId);

  return (
    <Screen
      header={
        <Header
          title="미션 수정"
          onBack={() => (router.canGoBack() ? router.back() : router.replace('/task'))}
        />
      }
      scroll
      keyboardAvoiding
    >
      {isPending ? (
        <Centered message="불러오는 중이에요." />
      ) : isError || !chore ? (
        <Centered message="미션을 찾을 수 없어요." />
      ) : (
        // 초기값이 서버에서 오므로, 도착하고 나서야 폼을 만든다 (key 로 한 번만 세운다).
        <EditBody
          key={chore.choreId}
          chore={chore}
          familyId={familyId}
          childId={childId}
          today={today}
        />
      )}
    </Screen>
  );
}

function EditBody({
  chore,
  familyId,
  childId,
  today,
}: {
  chore: ChoreDetail;
  familyId: number | undefined;
  childId: number | undefined;
  today: string;
}) {
  const theme = useTheme();
  const initial = useMemo(() => toFormInput(chore), [chore]);
  const [form, setForm] = useState<ChoreFormInput>(initial);
  const [reward, setReward] = useState(
    chore.rewardPoints != null ? String(chore.rewardPoints) : '',
  );
  const [errors, setErrors] = useState<ChoreFormErrors>({});
  const [rewardError, setRewardError] = useState<string | undefined>();

  const updateChore = useUpdateChore(familyId, childId, chore.choreId);

  const handleSubmit = () => {
    const nextErrors = validateChoreForm(form, today);
    // 서버 @Min(1) 과 맞춘다. 빈 칸은 "안 바꿈"이라 통과시킨다.
    const parsedReward = reward.trim().length === 0 ? undefined : Number(reward.trim());
    const nextRewardError =
      parsedReward != null && (!Number.isInteger(parsedReward) || parsedReward < 1)
        ? '1 이상의 숫자를 입력해주세요.'
        : undefined;

    setErrors(nextErrors);
    setRewardError(nextRewardError);
    if (Object.keys(nextErrors).length > 0 || nextRewardError) return;

    const changedReward = parsedReward !== chore.rewardPoints ? parsedReward : undefined;
    const body = toUpdateBody(form, initial, today, changedReward);

    if (Object.keys(body).length === 0) {
      router.back();
      return;
    }

    updateChore.mutate(body, { onSuccess: () => router.back() });
  };

  return (
    <View style={{ gap: theme.space.xl }}>
      <ChoreFormFields value={form} onChange={setForm} errors={errors} today={today} />

      <Input
        label="보상 금액 (도토리)"
        variant="box"
        value={reward}
        onChangeText={setReward}
        keyboardType="number-pad"
        placeholder={chore.rewardPoints == null ? 'AI가 산정하는 중이에요' : '보상 금액'}
        error={rewardError}
      />

      {updateChore.error ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
          {saveChoreErrorMessage(updateChore.error)}
        </Text>
      ) : null}

      <Button label="저장하기" loading={updateChore.isPending} onPress={handleSubmit} />
    </View>
  );
}

function Centered({ message }: { message: string }) {
  const theme = useTheme();

  return (
    <View style={styles.centered}>
      <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
