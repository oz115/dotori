import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { signOut, WithdrawButton } from '@/features/auth';
import { savedFamilyId, useDeleteFamily, useFamilyStatus } from '@/features/family';
import { Button } from '@/shared/ui';
import { useTheme } from '@/shared/theme';

/**
 * 부모 홈 자리 표시자. 실제 홈은 부모 홈 태스크에서 구현한다.
 *
 * 가족 유무에 따른 온보딩 분기는 라우트 가드로 옮길 예정이고,
 * 그전까지는 여기서 온보딩 진입점과 (테스트용) 가족 삭제를 제공한다.
 */
export default function ParentHomeScreen() {
  const theme = useTheme();
  const status = useFamilyStatus();
  const deleteFamily = useDeleteFamily();
  const [familyId, setFamilyId] = useState<number | null>(null);

  useEffect(() => {
    void savedFamilyId.get().then(setFamilyId);
  }, [status.data?.hasFamily]);

  const hasFamily = status.data?.hasFamily ?? false;

  const confirmDelete = () => {
    if (familyId == null || deleteFamily.isPending) return;
    Alert.alert('가족 삭제 (임시)', '이 가족과 온보딩 설정이 모두 삭제돼요. 계속할까요?', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: () =>
          deleteFamily.mutate(familyId, {
            onSuccess: () => {
              setFamilyId(null);
              Alert.alert('삭제됐어요', '온보딩을 다시 진행할 수 있어요.');
            },
            onError: () => Alert.alert('삭제 실패', '잠시 후 다시 시도해주세요.'),
          }),
      },
    ]);
  };

  return (
    <SafeAreaView
      edges={['top']}
      style={[styles.safe, { backgroundColor: theme.color.background.canvas }]}
    >
      <View style={styles.center}>
        <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>부모 홈</Text>

        {hasFamily ? (
          <>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              가족이 연결돼 있어요
            </Text>
            {familyId != null ? (
              <Button
                label="아이 초대 코드"
                variant="gradient"
                onPress={() =>
                  router.push({
                    pathname: '/onboarding/invite',
                    params: { familyId: String(familyId) },
                  })
                }
              />
            ) : null}
            <Button
              label="가족 삭제 (임시)"
              variant="outline"
              onPress={confirmDelete}
              loading={deleteFamily.isPending}
              disabled={familyId == null}
            />
          </>
        ) : (
          <Button
            label="우리 가족 만들기"
            variant="gradient"
            onPress={() => router.push('/onboarding')}
          />
        )}

        <Button label="로그아웃" variant="secondary" onPress={signOut} />
        <WithdrawButton />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    paddingHorizontal: 24,
  },
});
