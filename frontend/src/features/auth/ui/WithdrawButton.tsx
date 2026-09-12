import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { withdrawAccount } from '@/features/auth/model/session';
import { apiErrorCode, isNetworkError } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button, Input } from '@/shared/ui';

function errorMessage(e: unknown): string {
  if (isNetworkError(e)) return '서버에 연결할 수 없어요.';
  if (apiErrorCode(e) === 'PASSWORD_MISMATCH') return '비밀번호가 일치하지 않아요.';
  return '탈퇴에 실패했어요. 잠시 후 다시 시도해주세요.';
}

type WithdrawButtonProps = {
  /** 'link' 는 마이페이지 하단처럼 밑줄 친 작은 글씨로 (기본은 버튼 모양) */
  variant?: 'button' | 'link';
};

/**
 * 회원 탈퇴 트리거 + 비밀번호 확인 모달.
 * 성공하면 세션이 비워져 루트 가드가 로그인 화면으로 되돌린다.
 */
export function WithdrawButton({ variant = 'button' }: WithdrawButtonProps) {
  const theme = useTheme();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  const close = () => {
    setOpen(false);
    setPassword('');
    setError('');
  };

  const submit = async () => {
    if (password.length === 0 || pending) return;
    setPending(true);
    setError('');
    try {
      await withdrawAccount(password);
      // 성공 시 세션이 비워지며 화면이 전환된다 — close 불필요
    } catch (e) {
      setError(errorMessage(e));
      setPending(false);
    }
  };

  return (
    <>
      {variant === 'link' ? (
        <Pressable onPress={() => setOpen(true)} hitSlop={8} accessibilityRole="button">
          <Text style={[theme.typography.bodySm, styles.link, { color: theme.color.text.muted }]}>
            회원탈퇴
          </Text>
        </Pressable>
      ) : (
        <Button label="회원 탈퇴" variant="ghost" onPress={() => setOpen(true)} />
      )}

      <Modal visible={open} transparent animationType="fade" onRequestClose={close}>
        <Pressable style={styles.backdrop} onPress={close}>
          <Pressable
            style={[
              styles.sheet,
              {
                backgroundColor: theme.color.background.canvas,
                borderRadius: theme.radius.lg,
                padding: theme.space.xl,
                gap: theme.space.md,
              },
            ]}
            onPress={(e) => e.stopPropagation()}
          >
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              회원 탈퇴
            </Text>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              확인을 위해 비밀번호를 입력해주세요. 탈퇴하면 계정이 비활성화돼요.
            </Text>
            <Input
              variant="box"
              placeholder="비밀번호"
              secureTextEntry
              value={password}
              onChangeText={(v) => {
                setPassword(v);
                if (error) setError('');
              }}
              error={error || undefined}
            />
            <View style={[styles.actions, { gap: theme.space.sm }]}>
              <Button label="취소" variant="ghost" onPress={close} style={styles.flex} />
              <Button
                label="탈퇴하기"
                variant="primary"
                onPress={submit}
                loading={pending}
                disabled={password.length === 0}
                style={styles.flex}
              />
            </View>
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  sheet: { width: '100%' },
  actions: { flexDirection: 'row', marginTop: 4 },
  flex: { flex: 1 },
  link: { textDecorationLine: 'underline' },
});
