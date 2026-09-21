import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import {
  deleteFamilyErrorMessage,
  useDeleteFamily,
  useFamily,
  useFamilyId,
  useFamilyMembers,
  useUpdateFamily,
} from '@/features/family';
import { isNetworkError } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

/**
 * 가족 관리 (부모 전용). 가족 이름을 고치고, 필요하면 가족을 지운다.
 * 삭제는 되돌릴 수 없어 화면 맨 아래에 따로 떼어 두고 한 번 더 확인받는다.
 */
export default function FamilyManageScreen() {
  const theme = useTheme();
  const familyId = useFamilyId().data;
  const family = useFamily(familyId);
  const members = useFamilyMembers(familyId);
  const updateFamily = useUpdateFamily(familyId);
  const deleteFamily = useDeleteFamily();

  const [name, setName] = useState<string | null>(null);
  const [error, setError] = useState('');

  // 서버 값을 기본으로 두고, 사용자가 고치기 시작하면 그 입력을 보여준다
  const value = name ?? family.data?.name ?? '';
  const changed = value.trim().length > 0 && value.trim() !== family.data?.name;

  const save = () => {
    if (!changed || updateFamily.isPending) return;
    setError('');
    updateFamily.mutate(
      { name: value.trim() },
      {
        onSuccess: () => {
          setName(null);
          Alert.alert('저장했어요', '가족 이름이 바뀌었어요.');
        },
        onError: (e) =>
          setError(
            isNetworkError(e)
              ? '서버에 연결할 수 없어요.'
              : '이름을 바꾸지 못했어요. 잠시 후 다시 시도해주세요.',
          ),
      },
    );
  };

  const openInvite = () => {
    if (familyId == null) return;
    router.push({ pathname: '/onboarding/invite', params: { familyId: String(familyId) } });
  };

  const confirmDelete = () => {
    if (familyId == null || deleteFamily.isPending) return;
    Alert.alert(
      '가족을 삭제할까요?',
      '가족과 온보딩 설정이 모두 사라지고, 아이의 연결도 끊어져요. 되돌릴 수 없어요.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () =>
            deleteFamily.mutate(familyId, {
              onSuccess: () => {
                router.replace('/');
                Alert.alert('삭제됐어요', '가족을 다시 만들 수 있어요.');
              },
              onError: (e) => {
                const { title, message } = deleteFamilyErrorMessage(e);
                Alert.alert(title, message);
              },
            }),
        },
      ],
    );
  };

  const childCount = members.data?.filter((m) => m.role === 'CHILD').length ?? 0;

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="가족 관리" onBack={() => router.back()} />}
    >
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.md }}>
        <Input
          variant="box"
          label="가족 이름"
          placeholder="우리 가족"
          value={value}
          onChangeText={(v) => {
            setName(v);
            if (error) setError('');
          }}
          maxLength={100}
          error={error || undefined}
        />
        <Button
          label="이름 저장"
          variant="primary"
          onPress={save}
          disabled={!changed}
          loading={updateFamily.isPending}
        />
      </View>

      <View
        style={[
          styles.infoBox,
          {
            marginTop: theme.space['2xl'],
            borderRadius: theme.radius.lg,
            backgroundColor: theme.color.background.surfaceSunken,
            padding: theme.space.lg,
            gap: theme.space.md,
          },
        ]}
      >
        <View style={{ gap: theme.space.xs }}>
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            연결된 아이 {childCount}명
          </Text>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            아이를 초대하려면 코드를 발급해 알려주세요. 코드는 10분간만 쓸 수 있어요.
          </Text>
        </View>
        <Button
          label="초대 코드 발급"
          variant="gradient"
          onPress={openInvite}
          disabled={familyId == null}
        />
      </View>

      <View style={styles.spacer} />

      <View style={{ gap: theme.space.sm }}>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          가족을 삭제하면 아이와의 연결과 설정이 모두 사라져요.
        </Text>
        <Button
          label="가족 삭제"
          variant="outline"
          onPress={confirmDelete}
          disabled={familyId == null}
          loading={deleteFamily.isPending}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  infoBox: {},
  spacer: { flex: 1, minHeight: 32 },
});
