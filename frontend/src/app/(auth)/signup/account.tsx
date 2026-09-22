import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import {
  LOGIN_ID_MAX,
  SignupStepLayout,
  useCheckLoginId,
  useSignupStore,
  validateAccountStep,
  validateLoginId,
} from '@/features/auth';
import { apiErrorCode, isNetworkError } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button, Input } from '@/shared/ui';

type Errors = ReturnType<typeof validateAccountStep>;

export default function SignupAccountScreen() {
  const theme = useTheme();
  const patch = useSignupStore((s) => s.patch);
  const check = useCheckLoginId();

  const [loginId, setLoginId] = useState(() => useSignupStore.getState().loginId);
  const [password, setPassword] = useState(() => useSignupStore.getState().password);
  const [passwordConfirm, setPasswordConfirm] = useState(() => useSignupStore.getState().password);
  const [name, setName] = useState(() => useSignupStore.getState().name);
  const [errors, setErrors] = useState<Errors>({});
  const [availableId, setAvailableId] = useState(''); // 중복확인 통과한 아이디 값

  const idChecked = availableId !== '' && availableId === loginId.trim();

  const onChangeLoginId = (v: string) => {
    setLoginId(v);
    // 값이 바뀌면 이전에 통과한 중복확인은 그 값에 대한 것이라 더는 유효하지 않다.
    // (같은 아이디로 되돌려 써도 다시 확인받는다 — 그새 다른 사람이 쓸 수 있다)
    setAvailableId('');
    // 길이 초과는 입력하는 즉시 알려준다 (서버 상한 50자). 그 외 에러는 다시 검사할 때까지 지운다.
    const tooLong = v.trim().length > LOGIN_ID_MAX;
    setErrors((e) => ({ ...e, loginId: tooLong ? validateLoginId(v) : undefined }));
  };

  const checkDuplicate = () => {
    const value = loginId.trim();
    if (check.isPending) return;
    const lengthError = validateLoginId(value);
    if (lengthError) {
      setErrors((e) => ({ ...e, loginId: lengthError }));
      return;
    }
    check.mutate(value, {
      onSuccess: () => {
        setAvailableId(value);
        setErrors((e) => ({ ...e, loginId: undefined }));
      },
      onError: (err) => {
        setAvailableId('');
        setErrors((e) => ({
          ...e,
          loginId:
            apiErrorCode(err) === 'DUPLICATE_LOGIN_ID'
              ? '이미 사용 중인 아이디예요.'
              : isNetworkError(err)
                ? '서버에 연결할 수 없어요.'
                : '중복 확인에 실패했어요. 다시 시도해주세요.',
        }));
      },
    });
  };

  const next = () => {
    const found = validateAccountStep({ loginId, password, passwordConfirm, name });
    if (!idChecked) {
      found.loginId = found.loginId ?? '아이디 중복확인을 해주세요.';
    }
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    patch({ loginId: loginId.trim(), password, name: name.trim() });
    router.push('/signup/role');
  };

  return (
    <SignupStepLayout
      step={2}
      title="회원가입"
      subtitle="기본정보를 입력해주세요."
      footer={<Button label="다음" variant="primary" onPress={next} />}
    >
      <View style={{ gap: theme.space.xl }}>
        <View style={styles.field}>
          <Input
            variant="box"
            label="아이디"
            placeholder="아이디를 입력하세요"
            autoCapitalize="none"
            autoCorrect={false}
            value={loginId}
            onChangeText={onChangeLoginId}
            error={errors.loginId}
            rightSlot={
              <Button
                label="중복확인"
                variant="outline"
                size="sm"
                onPress={checkDuplicate}
                loading={check.isPending}
              />
            }
          />
          {idChecked ? (
            <Text style={[theme.typography.caption, { color: theme.color.brand.solid }]}>
              사용 가능한 아이디예요.
            </Text>
          ) : null}
        </View>

        <Input
          variant="box"
          label="비밀번호"
          placeholder="비밀번호를 입력하세요"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
          error={errors.password}
        />
        <Input
          variant="box"
          label="비밀번호 확인"
          placeholder="비밀번호를 한번 더 입력하세요"
          secureTextEntry
          value={passwordConfirm}
          onChangeText={setPasswordConfirm}
          error={errors.passwordConfirm}
        />
        <Input
          variant="box"
          label="이름"
          placeholder="이름을 입력하세요"
          value={name}
          onChangeText={setName}
          error={errors.name}
        />
      </View>
    </SignupStepLayout>
  );
}

const styles = StyleSheet.create({
  field: { gap: 6 },
});
