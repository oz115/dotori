import { router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import {
  deleteFamilyErrorMessage,
  removeChildErrorMessage,
  useDeleteFamily,
  useFamily,
  useFamilyId,
  useFamilyMembers,
  useRemoveChild,
  useUpdateFamily,
} from '@/features/family';
import { isNetworkError } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button, ChildBadge, Header, Input, Screen } from '@/shared/ui';

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
  const removeChild = useRemoveChild(familyId);
  /** 제외 요청 중인 아이. 그 줄만 비활성화한다 (여러 아이가 동시에 눌리지 않게) */
  const [removingChildId, setRemovingChildId] = useState<number | null>(null);

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

  /** 세 박스가 같은 모양이어야 안쪽 폭이 어긋나 보이지 않는다 (테마 값이라 StyleSheet 밖) */
  const cardStyle = {
    borderRadius: theme.radius.lg,
    backgroundColor: theme.color.background.surfaceSunken,
    padding: theme.space.lg,
    gap: theme.space.md,
  };

  const children = members.data?.filter((member) => member.role === 'CHILD') ?? [];

  /**
   * 아이를 가족에서 제외한다.
   *
   * 되돌릴 수 없다 - 다시 넣으려면 초대 코드를 새로 발급해 아이가 합류해야 한다. 그래서
   * 이름을 문구에 넣어 누구를 내보내는지 한 번 더 확인시킨다.
   */
  const confirmRemoveChild = (childId: number, name: string) => {
    if (familyId == null || removeChild.isPending) return;
    Alert.alert(
      `${name}을(를) 제외할까요?`,
      '연결이 끊어져서 이 아이의 미션·상점·저축을 더 이상 관리할 수 없어요.\n' +
        '다시 넣으려면 초대 코드를 새로 발급해야 해요.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '제외',
          style: 'destructive',
          onPress: () => {
            setRemovingChildId(childId);
            removeChild.mutate(childId, {
              onSuccess: () => Alert.alert('제외했어요', `${name}의 연결이 끊어졌어요.`),
              onError: (e) => {
                const { title, message } = removeChildErrorMessage(e);
                Alert.alert(title, message);
              },
              onSettled: () => setRemovingChildId(null),
            });
          },
        },
      ],
    );
  };

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="가족 관리" onBack={() => router.back()} />}
    >
      {/* 가족 이름도 아래 두 박스와 같은 상자에 담는다. 박스 밖에 두면 입력칸·버튼이 화면
          여백까지 꽉 차서, 박스 패딩만큼 안으로 들어간 아래쪽 버튼들과 폭이 어긋나 보였다 */}
      <View style={[cardStyle, { marginTop: theme.space['2xl'] }]}>
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

      {/* 초대 박스 — 버튼을 맨 위에 두고 설명을 아래에 붙인다. 이 박스가 하는 일은 하나(초대
          코드 발급)라, 제목을 따로 얹는 대신 버튼이 제목 노릇을 한다 */}
      <View style={[cardStyle, { marginTop: theme.space.md }]}>
        <Button
          label="초대 코드 발급"
          variant="gradient"
          onPress={openInvite}
          disabled={familyId == null}
        />
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          아이를 초대하려면 코드를 발급해 알려주세요. 코드는 10분간만 쓸 수 있어요.
        </Text>
      </View>

      {/* 연결된 아이 박스 — 초대와 다른 일(누가 있는지 보고 내보내기)이라 박스를 나눈다.
          아이 수만큼 줄이 늘어난다 */}
      <View style={[cardStyle, { marginTop: theme.space.md }]}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          연결된 아이 {children.length}명
        </Text>

        {members.isPending ? (
          <ActivityIndicator color={theme.color.brand.solid} />
        ) : children.length === 0 ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
            아직 연결된 아이가 없어요. 초대 코드를 발급해 알려주세요.
          </Text>
        ) : (
          <View style={{ gap: theme.space.md }}>
            {children.map((child) => (
              <View key={child.userId} style={styles.childRow}>
                <ChildBadge name={child.name} />
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`${child.name} 제외`}
                  onPress={() => confirmRemoveChild(child.userId, child.name)}
                  disabled={removeChild.isPending}
                  hitSlop={8}
                >
                  {removingChildId === child.userId ? (
                    <ActivityIndicator size="small" color={theme.color.status.danger.text} />
                  ) : (
                    <Text
                      style={[
                        theme.typography.bodySm,
                        {
                          color: removeChild.isPending
                            ? theme.color.text.muted
                            : theme.color.status.danger.text,
                        },
                      ]}
                    >
                      제외
                    </Text>
                  )}
                </Pressable>
              </View>
            ))}
          </View>
        )}
      </View>

      <View style={styles.spacer} />

      {/* 되돌릴 수 없는 동작이라 설정들과 떼어 화면 맨 아래에 둔다. 다만 제스처바에 붙지
          않게 아래 여백을 준다 - 붙어 있으면 화면 밖으로 흘러내린 것처럼 보인다 */}
      <View style={{ gap: theme.space.sm, paddingBottom: theme.space['2xl'] }}>
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
  childRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  spacer: { flex: 1, minHeight: 32 },
});
