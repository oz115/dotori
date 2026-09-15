import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { validateStoreProductForm } from '@/features/store/lib/storeProductForm';
import { useCreateStoreProduct } from '@/features/store/model/useCreateStoreProduct';
import type { StoreProductCategory } from '@/features/store/model/types';
import { StoreProductFormBanner } from '@/features/store/ui/StoreProductFormBanner';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Header, Input, Screen, SelectableCard } from '@/shared/ui';

const CATEGORIES: StoreProductCategory[] = ['GAME', 'SNACK', 'TOY', 'OUTING', 'PRIVILEGE', 'ETC'];

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

        <Input
          label="상품명"
          variant="box"
          placeholder="예: 게임 30분 이용권"
          value={name}
          onChangeText={setName}
          error={errors.name}
        />

        <Input
          label="설명 (선택)"
          variant="box"
          placeholder="예: 정해진 시간 외에 추가로 게임을 하고 싶을 때 구매"
          value={description}
          onChangeText={setDescription}
          multiline
          numberOfLines={3}
          error={errors.description}
        />

        <View style={{ gap: theme.space.sm }}>
          <Text style={[theme.typography.label, { color: theme.color.text.label }]}>카테고리</Text>
          <View style={styles.categoryGrid}>
            {CATEGORIES.map((key) => {
              const selected = category === key;
              return (
                <SelectableCard
                  key={key}
                  selected={selected}
                  onPress={() => setCategory(key)}
                  style={styles.categoryCard}
                >
                  <View style={{ alignItems: 'center', gap: theme.space.xs }}>
                    <Image
                      source={categoryImage(key)}
                      style={styles.categoryImage}
                      resizeMode="contain"
                    />
                    <Text
                      style={[
                        theme.typography.caption,
                        { color: selected ? theme.color.brand.solid : theme.color.text.default },
                      ]}
                    >
                      {categoryLabel(key)}
                    </Text>
                  </View>
                </SelectableCard>
              );
            })}
          </View>
          {errors.category ? (
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              {errors.category}
            </Text>
          ) : null}
        </View>

        <QuantityStepper value={quantity} onChange={setQuantity} error={errors.quantity} />

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

type QuantityStepperProps = {
  value: number;
  onChange: (value: number) => void;
  error?: string;
};

/** 재고 수량 입력. 타이핑 대신 -/+ 로만 조절한다 (오타로 이상한 값이 들어가는 걸 막는다). */
function QuantityStepper({ value, onChange, error }: QuantityStepperProps) {
  const theme = useTheme();

  return (
    <View style={{ gap: 6 }}>
      <Text style={[theme.typography.label, { color: theme.color.text.label }]}>재고 수량</Text>
      <View
        style={[
          styles.stepperBox,
          {
            minHeight: theme.layout.inputHeight,
            borderColor: error ? theme.color.status.danger.solid : theme.color.border.default,
            borderRadius: theme.radius.lg,
            backgroundColor: theme.color.background.surface,
            paddingHorizontal: theme.space.lg,
          },
        ]}
      >
        <Pressable
          onPress={() => onChange(Math.max(0, value - 1))}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="수량 줄이기"
        >
          <Ionicons name="remove" size={20} color={theme.color.text.strong} />
        </Pressable>
        <Text style={[theme.typography.body, { color: theme.color.text.strong }]}>{value}</Text>
        <Pressable
          onPress={() => onChange(value + 1)}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="수량 늘리기"
        >
          <Ionicons name="add" size={20} color={theme.color.text.strong} />
        </Pressable>
      </View>
      {error ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  stepperBox: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1.5,
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  categoryCard: {
    width: '31%',
  },
  categoryImage: {
    width: 36,
    height: 36,
  },
});
