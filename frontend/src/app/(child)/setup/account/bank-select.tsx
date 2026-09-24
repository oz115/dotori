import { router } from 'expo-router';

import { useLinkAccountStore } from '@/features/account';
import { BankSelectView } from '@/widgets/account-link';

export default function ChildBankSelectScreen() {
  const bankKey = useLinkAccountStore((s) => s.bankKey);
  const patch = useLinkAccountStore((s) => s.patch);

  return (
    <BankSelectView
      selected={bankKey}
      onSelect={(key) => patch({ bankKey: key })}
      onConfirm={() => router.back()}
      onClose={() => router.back()}
    />
  );
}
