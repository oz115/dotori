import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { CodeInput, connectFamilyErrorMessage, useConnectFamily } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Screen } from '@/shared/ui';

const CODE_LENGTH = 6;

export default function ChildConnectScreen() {
  const theme = useTheme();
  const connect = useConnectFamily();

  const [code, setCode] = useState('');
  const [error, setError] = useState('');

  const canSubmit = code.length === CODE_LENGTH && !connect.isPending;

  const submit = () => {
    if (!canSubmit) return;
    setError('');
    connect.mutate(code, {
      onSuccess: () => router.replace('/'),
      onError: (e) => setError(connectFamilyErrorMessage(e)),
    });
  };

  return (
    <Screen
      scroll
      keyboardAvoiding
      header={<Header title="가족 연결하기" onBack={() => router.back()} />}
    >
      <View style={{ marginTop: theme.space['2xl'], gap: theme.space.sm }}>
        <Text style={[theme.typography.h1, { color: theme.color.text.strong }]}>초대코드 입력</Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          부모님께 받은{'\n'}
          <Text style={{ color: theme.color.brand.solid }}>초대코드</Text>를 입력해주세요.
        </Text>
      </View>

      <View style={{ marginTop: theme.space['4xl'], gap: theme.space.sm }}>
        <CodeInput
          value={code}
          onChangeText={(v) => {
            setCode(v);
            if (error) setError('');
          }}
          length={CODE_LENGTH}
          onFilled={submit}
          error={!!error}
          autoFocus
        />
        {error ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            {error}
          </Text>
        ) : null}

        <View style={{ marginTop: theme.space.lg }}>
          <Callout
            icon={<Ionicons name="help-circle-outline" size={20} color={theme.color.brand.solid} />}
            title="코드가 없나요?"
            description="부모님께 초대코드를 받아보세요!"
          />
        </View>
      </View>

      <View style={styles.spacer} />

      <Button
        label="연결하기"
        variant="primary"
        onPress={submit}
        loading={connect.isPending}
        disabled={!canSubmit}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  spacer: { flex: 1, minHeight: 24 },
});
