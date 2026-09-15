import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { validateStoreProductForm } from '@/features/store/lib/storeProductForm';
import { useCreateStoreProduct } from '@/features/store/model/useCreateStoreProduct';
import type { StoreProductCategory } from '@/features/store/model/types';
import { StoreProductFields } from '@/features/store/ui/StoreProductFields';
import { StoreProductFormBanner } from '@/features/store/ui/StoreProductFormBanner';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Screen } from '@/shared/ui';

type StoreProductFormProps = {
  familyId: number | undefined;
};

/** 상품 등록 화면 (SHOP-03). 부모 전용 — 가격 입력란은 없고, 등록 후 AI가 비동기로 산정한다. */
export function StoreProductForm({ familyId }: StoreProductFormProps) {
  const theme = useTheme();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<StoreProductCategory | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [errors, setErrors] = useState<ReturnType<typeof validateStoreProductForm>>({});

  const createProduct = useCreateStoreProduct(familyId);

  const handleSubmit = () => {
    const nextErrors = validateStoreProductForm({
      name,
      description,
      category,
      quantity: String(quantity),
    });
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0 || category == null) return;

    createProduct.mutate(
      {
        name: name.trim(),
        description: description.trim().length > 0 ? description.trim() : null,
        category,
        quantity,
      },
      {
        onSuccess: (product) => {
          router.replace({
            pathname: '/store/[productId]',
            params: { productId: String(product.storeProductId) },
          });
        },
      },
    );
  };

  return (
    <Screen header={<Header title="상품 등록" onBack={() => router.back()} />} scroll>
      <View style={{ gap: theme.space.xl }}>
        <StoreProductFormBanner />

        <StoreProductFields
          name={name}
          onNameChange={setName}
          description={description}
          onDescriptionChange={setDescription}
          category={category}
          onCategoryChange={setCategory}
          quantity={quantity}
          onQuantityChange={setQuantity}
          errors={errors}
        />

        <Callout
          icon={<Ionicons name="sparkles" size={20} color={theme.color.brand.solid} />}
          title="가격은 AI가 정해요"
          description="등록하면 이름·설명·카테고리를 보고 AI가 적정 가격을 자동으로 산정해요."
        />

        {createProduct.isError ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
            등록에 실패했어요. 잠시 후 다시 시도해주세요.
          </Text>
        ) : null}

        <Button
          label="등록하기"
          variant="primary"
          loading={createProduct.isPending}
          onPress={handleSubmit}
        />
      </View>
    </Screen>
  );
}
