// Created: 2026-09-19 13:19:56
import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Image, StyleSheet, Text, View } from 'react-native';

import {
  changePasswordErrorMessage,
  clearSessionAfterPasswordChange,
  updateProfileErrorMessage,
  useAuthStore,
  useChangePassword,
  useProfile,
  useUpdateProfile,
} from '@/features/auth';
import { fontFamily, useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

/**
 * 마이페이지 - 내 정보 수정. 이름과 비밀번호를 한 화면에서 바꾼다.
 * 비밀번호 칸 세 개를 비워두면 이름만 바뀐다 — 비밀번호 변경은 선택이다.
 *
 * 비밀번호를 바꾸면 서버가 기존 세션(현재 세션 포함)을 전부 폐기한다. 안내를 먼저 보여 주고
 * 확인을 누르면 로컬 세션을 정리해, root 가드가 로그인 화면으로 돌려보낸다.
 */
export function EditProfileView() {
  const theme = useTheme();
  const profile = useProfile();
  const updateProfile = useUpdateProfile();
  const changePassword = useChangePassword();
  const authRole = useAuthStore((s) => s.user?.role);
  const role = profile.data?.role ?? authRole;

  const mascotSource =
    role === 'PARENT'
      ? require('@/assets/images/faces/parent-mypage.png')
      : require('@/assets/images/faces/child-mypage.png');

  const originalName = profile.data?.name ?? '';
  // null이면 "아직 안 건드림" — 서버 값을 그대로 보여준다. 한 글자라도 입력하면 그 값을 따른다.
  const [nameInput, setNameInput] = useState<string | null>(null);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [nameError, setNameError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [submitError, setSubmitError] = useState('');

  const name = nameInput ?? originalName;
  const trimmedName = name.trim();
  const wantsPasswordChange =
    currentPassword.length > 0 || newPassword.length > 0 || newPasswordConfirm.length > 0;
  const hasChanges = trimmedName !== originalName || wantsPasswordChange;
  const pending = updateProfile.isPending || changePassword.isPending;

  const validate = (): boolean => {
    setNameError('');
    setPasswordError('');

    if (trimmedName.length === 0) {
      setNameError('이름을 입력해주세요.');
      return false;
    }

    if (wantsPasswordChange) {
      if (currentPassword.length === 0) {
        setPasswordError('현재 비밀번호를 입력해주세요.');
        return false;
      }
      if (newPassword.length < 8 || newPassword.length > 64) {
        setPasswordError('새 비밀번호는 공백 없이 8자 이상 64자 이하여야 해요.');
        return false;
      }
      if (newPassword !== newPasswordConfirm) {
        setPasswordError('새 비밀번호가 일치하지 않아요.');
        return false;
      }
    }

    return true;
  };

  const submit = () => {
    if (pending || !validate()) return;
    setSubmitError('');

    const afterNameStep = () => {
      if (!wantsPasswordChange) {
        Alert.alert('완료', '정보가 수정됐어요.', [{ text: '확인', onPress: () => router.back() }]);
        return;
      }
      changePassword.mutate(
        { currentPassword, newPassword },
        {
          onSuccess: () => {
            // 안내를 먼저 보여 주고, 확인을 누른 뒤에 세션을 정리한다. 먼저 정리해 버리면
            // root 가드가 그 자리에서 로그인 화면으로 트리를 갈아치워 이 안내가 뜨자마자
            // 사라졌다 (QA - 안내가 없다는 지적).
            //
            // 확인을 기다리는 동안 로컬 토큰은 이미 서버에서 폐기된 값이다. 알럿이 모달이라
            // 그 사이 다른 화면을 만질 수는 없지만, 뒤에서 도는 조회가 401 을 받으면 인터셉터가
            // 세션을 지운다 - 그때도 결국 로그인 화면이라 결과는 같다.
            Alert.alert('비밀번호를 바꿨어요', '새 비밀번호로 다시 로그인해주세요.', [
              { text: '확인', onPress: () => void clearSessionAfterPasswordChange() },
            ]);
          },
          onError: (e) => setPasswordError(changePasswordErrorMessage(e)),
        },
      );
    };

    if (trimmedName !== originalName) {
      updateProfile.mutate(trimmedName, {
        onSuccess: afterNameStep,
        onError: (e) => setSubmitError(updateProfileErrorMessage(e)),
      });
    } else {
      afterNameStep();
    }
  };

  const canSubmit = hasChanges && trimmedName.length > 0 && !pending;

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="내 정보 수정" onBack={() => router.back()} />}
    >
      <View style={[styles.mascotRow, { paddingTop: theme.space.lg }]}>
        <Image source={mascotSource} style={styles.mascot} resizeMode="contain" />
      </View>

      <View style={{ marginTop: theme.space['2xl'], gap: theme.space['2xl'] }}>
        <View style={{ gap: theme.space.md }}>
          <Input
            variant="box"
            label="아이디"
            labelStyle={styles.fieldLabel}
            value={profile.data?.loginId ?? ''}
            editable={false}
          />
          <Input
            variant="box"
            label="이메일"
            labelStyle={styles.fieldLabel}
            value={profile.data?.email ?? ''}
            editable={false}
          />
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            아이디와 이메일은 바꿀 수 없어요.
          </Text>
        </View>

        <Input
          variant="box"
          label="이름"
          labelStyle={styles.fieldLabel}
          placeholder="이름을 입력하세요"
          value={name}
          onChangeText={(v) => {
            setNameInput(v);
            if (nameError) setNameError('');
          }}
          error={nameError}
        />

        <View style={{ gap: theme.space.md }}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            비밀번호 변경 (선택)
          </Text>
          <Input
            variant="box"
            placeholder="현재 비밀번호"
            secureTextEntry
            value={currentPassword}
            onChangeText={(v) => {
              setCurrentPassword(v);
              if (passwordError) setPasswordError('');
            }}
          />
          <Input
            variant="box"
            placeholder="새 비밀번호"
            secureTextEntry
            value={newPassword}
            onChangeText={(v) => {
              setNewPassword(v);
              if (passwordError) setPasswordError('');
            }}
          />
          <Input
            variant="box"
            placeholder="새 비밀번호 확인"
            secureTextEntry
            value={newPasswordConfirm}
            onChangeText={(v) => {
              setNewPasswordConfirm(v);
              if (passwordError) setPasswordError('');
            }}
            error={passwordError}
          />
        </View>
      </View>

      {submitError ? (
        <Text
          style={[
            theme.typography.caption,
            { color: theme.color.status.danger.text, marginTop: theme.space.sm },
          ]}
        >
          {submitError}
        </Text>
      ) : null}

      <View style={[styles.spacer, { minHeight: theme.space['2xl'] }]} />

      <Button
        label="저장"
        variant="primary"
        onPress={submit}
        loading={pending}
        disabled={!canSubmit}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  spacer: { flex: 1 },
  mascotRow: { alignItems: 'center' },
  mascot: { width: 96, height: 96 },
  // 아이디·이메일·이름 라벨을 기본 Input label(medium 16)보다 조금 크고 굵게
  fieldLabel: { fontFamily: fontFamily.semibold, fontSize: 17 },
});
