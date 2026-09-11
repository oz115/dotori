import { Image, StyleSheet } from 'react-native';

import type { Bank } from '@/features/account/lib/banks';

type Props = {
  bank: Bank;
  size?: number;
};

/** 은행 로고. */
export function BankBadge({ bank, size = 40 }: Props) {
  return (
    <Image
      source={bank.logo}
      style={[styles.logo, { width: size, height: size }]}
      resizeMode="contain"
      accessibilityLabel={bank.name}
    />
  );
}

const styles = StyleSheet.create({
  logo: { borderRadius: 6 },
});
