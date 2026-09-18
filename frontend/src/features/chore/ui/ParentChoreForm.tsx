import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import { saveChoreErrorMessage } from '@/features/chore/lib/choreError';
import {
  toCreateBody,
  validateChoreForm,
  type ChoreFormErrors,
  type ChoreFormInput,
} from '@/features/chore/lib/choreForm';
import { useCreateChore } from '@/features/chore/model/useCreateChore';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import { ChoreFormFields } from '@/features/chore/ui/ChoreFormFields';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Screen } from '@/shared/ui';

type ParentChoreFormProps = {
  familyId: number | undefined;
  childList: ChoreChild[];
};

/** 오늘 (YYYY-MM-DD), 기기 로컬 시간 기준 */
function todayDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * 할 일 등록 (부모 전용).
 *
 * 시안에 있던 "보상 금액" 입력과 "AI 가격 추천" 버튼은 뺐다. 서버가 등록 요청에서 보상을
 * 아예 받지 않고 AI 가 등록 직후 산정하기 때문이다 - 칸을 두면 입력한 값이 조용히 버려진다.
 * 대신 그 사실을 Callout 으로 알린다 (상점 상품 등록과 같은 방식).
 *
 * 시안에 없던 "담당 아이"를 넣었다. 경로가 아이를 지목해서 고르지 않으면 요청 자체를
 * 만들 수 없다. 아이가 하나뿐이면 미리 골라 둔다.
 */
export function ParentChoreForm({ familyId, childList }: ParentChoreFormProps) {
  const theme = useTheme();
  const today = todayDate();
  const [form, setForm] = useState<ChoreFormInput>({
    childId: childList.length === 1 ? childList[0].childId : null,
    title: '',
    description: '',
    difficulty: 'NORMAL',
    repeatType: 'ONCE',
    endDate: null,
  });
  const [errors, setErrors] = useState<ChoreFormErrors>({});

  const createChore = useCreateChore(familyId, form.childId ?? undefined);

  const handleSubmit = () => {
    const nextErrors = validateChoreForm(form, today);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    createChore.mutate(toCreateBody(form, today), {
      onSuccess: () => (router.canGoBack() ? router.back() : router.replace('/task')),
    });
  };

  return (
    <Screen
      header={<Header title="미션 등록" onBack={() => router.back()} />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      scroll
      keyboardAvoiding
    >
      <View style={{ gap: theme.space.xl }}>
        <View style={[styles.intro, { gap: theme.space.md }]}>
          <Text style={[theme.typography.body, styles.grow, { color: theme.color.text.default }]}>
            우리 아이와 함께할{'\n'}새로운 미션을 만들어보세요!
          </Text>
          <Image
            source={require('@/assets/images/chore/owl-smile.png')}
            style={styles.mascot}
            resizeMode="contain"
          />
        </View>

        <ChoreFormFields
          value={form}
          onChange={setForm}
          errors={errors}
          childList={childList}
          today={today}
        />

        <Callout
          icon={<Ionicons name="sparkles" size={20} color={theme.color.brand.solid} />}
          title="보상은 AI가 정해요"
          description="등록하면 제목·설명·난이도를 보고 AI가 알맞은 도토리를 매겨요. 마음에 안 들면 수정에서 바꿀 수 있어요."
        />

        {createChore.error ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
            {saveChoreErrorMessage(createChore.error)}
          </Text>
        ) : null}

        <Button label="등록하기" loading={createChore.isPending} onPress={handleSubmit} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  intro: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  mascot: { width: 96, height: 96 },
});
