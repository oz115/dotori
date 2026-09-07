import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, View } from 'react-native';

import { SignupStepLayout, useSignupStore, validateAccountStep } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Input } from '@/shared/ui';

// TODO(BE): 아이디 중복확인 API 연동. 현재는 안내만.
const checkDuplicate = () => Alert.alert('준비 중', '아이디 중복 확인은 백엔드 작업 후 연결돼요.');

type Errors = ReturnType<typeof validateAccountStep>;

export default function SignupAccountScreen() {
  const theme = useTheme();
  const patch = useSignupStore((s) => s.patch);

  // 초기값만 스토어에서 읽는다 (뒤로 갔다 와도 입력 유지). 이후는 로컬 상태.
  const [loginId, setLoginId] = useState(() => useSignupStore.getState().loginId);
  const [password, setPassword] = useState(() => useSignupStore.getState().password);
  const [passwordConfirm, setPasswordConfirm] = useState(() => useSignupStore.getState().password);
  const [name, setName] = useState(() => useSignupStore.getState().name);
  const [errors, setErrors] = useState<Errors>({});

  const next = () => {
    const found = validateAccountStep({ loginId, password, passwordConfirm, name });
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
        <Input
          variant="box"
          label="아이디"
          placeholder="아이디를 입력하세요"
          autoCapitalize="none"
          autoCorrect={false}
          value={loginId}
          onChangeText={setLoginId}
          error={errors.loginId}
          rightSlot={
            <Button label="중복확인" variant="outline" size="sm" onPress={checkDuplicate} />
          }
        />
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
