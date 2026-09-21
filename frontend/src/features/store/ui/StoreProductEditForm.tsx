import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import { validatePrice, validateStoreProductForm } from '@/features/store/lib/storeProductForm';
import { useStoreProduct } from '@/features/store/model/useStoreProduct';
import { useUpdateStoreProduct } from '@/features/store/model/useUpdateStoreProduct';
import type { StoreProductCategory, StoreProductDetail } from '@/features/store/model/types';
import { StoreProductFields } from '@/features/store/ui/StoreProductFields';
import { useTheme } from '@/shared/theme';
import { Button, Header, Input, Screen } from '@/shared/ui';

type StoreProductEditFormProps = {
  familyId: number | undefined;
  productId: number | undefined;
};

/**
 * 상품 수정 화면 (SHOP-04). 부모 전용. 현재 값으로 폼을 채워서 보여주고, 항상 전체 필드를
 * 다시 보내는 방식이라 실질적으로는 "덮어쓰기"다 (백엔드 자체는 부분 수정도 받는다).
 */
export function StoreProductEditForm({ familyId, productId }: StoreProductEditFormProps) {
  const theme = useTheme();
  const { data: product, isPending, isError } = useStoreProduct(familyId, productId);

  return (
    // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
    <Screen
      header={<Header title="상품 수정" onBack={() => router.back()} />}
      scroll={!!product}
      edges={[]}
    >
      {isPending ? (
        <Centered>
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
            불러오는 중이에요.
          </Text>
        </Centered>
      ) : isError || !product ? (
        <Centered>
          <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
            상품을 찾을 수 없어요.
          </Text>
        </Centered>
      ) : (
        <StoreProductEditFormBody familyId={familyId as number} product={product} />
      )}
    </Screen>
  );
}

function Centered({ children }: { children: ReactNode }) {
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>{children}</View>
  );
}

type BodyProps = {
  familyId: number;
  product: StoreProductDetail;
};

/**
 * product 가 이미 있는 상태로만 마운트되는 자식이라, 초기값을 useState 초기화 인자로
 * 바로 넣을 수 있다 — useEffect 로 "한 번만 채우기"를 흉내 낼 필요가 없다.
 */
function StoreProductEditFormBody({ familyId, product }: BodyProps) {
  const theme = useTheme();
  const updateProduct = useUpdateStoreProduct(familyId, product.storeProductId);

  const [name, setName] = useState(product.name);
  const [description, setDescription] = useState(product.description ?? '');
  const [category, setCategory] = useState<StoreProductCategory | null>(product.category);
  const [quantity, setQuantity] = useState(product.quantity);
  const [price, setPrice] = useState(product.price != null ? String(product.price) : '');
  const [errors, setErrors] = useState<ReturnType<typeof validateStoreProductForm>>({});
  const [priceError, setPriceError] = useState<string | undefined>();

  const priceEditable = product.price != null;
  const priceTouched = priceEditable && price.trim().length > 0;
  const registeredAt = formatDate(product.createdAt);

  const handleSubmit = () => {
    const nextErrors = validateStoreProductForm({
      name,
      description,
      category,
      quantity: String(quantity),
    });
    // 가격은 비워두면 "안 바꿈"이라 검증 대상이 아니다 — 값을 입력했을 때만 유효성을 본다.
    const nextPriceError = priceTouched ? validatePrice(price) : undefined;
    setErrors(nextErrors);
    setPriceError(nextPriceError);
    if (Object.keys(nextErrors).length > 0 || nextPriceError != null || category == null) return;

    updateProduct.mutate(
      {
        name: name.trim(),
        description: description.trim().length > 0 ? description.trim() : null,
        category,
        quantity,
        ...(priceTouched ? { price: Number(price) } : {}),
      },
      { onSuccess: () => router.back() },
    );
  };

  return (
    <View style={{ gap: theme.space.xl }}>
      <ProductPreviewCard product={product} registeredAt={registeredAt} />

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

      <View style={{ gap: 6 }}>
        <Input
          label="가격 (도토리)"
          variant="box"
          placeholder={priceEditable ? '예: 3000' : 'AI 산정 후 수정할 수 있어요'}
          value={price}
          onChangeText={setPrice}
          keyboardType="number-pad"
          editable={priceEditable}
          error={priceError}
        />
        {priceEditable ? (
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            비워두면 기존 가격이 유지됩니다.
          </Text>
        ) : null}
      </View>

      {updateProduct.isError ? (
        <Text style={[theme.typography.bodySm, { color: theme.color.status.danger.text }]}>
          수정에 실패했어요. 잠시 후 다시 시도해주세요.
        </Text>
      ) : null}

      <Button
        label="저장하기"
        variant="primary"
        loading={updateProduct.isPending}
        onPress={handleSubmit}
      />
    </View>
  );
}

/** 지금 뭘 수정하는지 한눈에 보여주는 미리보기 카드. 입력값이 아니라 항상 원본 product 기준이다. */
function ProductPreviewCard({
  product,
  registeredAt,
}: {
  product: StoreProductDetail;
  registeredAt: string;
}) {
  const theme = useTheme();
  const badge = categoryBadgeColors(theme, product.category);

  return (
    <View style={styles.previewRow}>
      <View
        style={[styles.previewThumb, { backgroundColor: badge.bg, borderRadius: theme.radius.lg }]}
      >
        <Image
          source={categoryImage(product.category)}
          style={styles.previewImage}
          resizeMode="contain"
        />
      </View>
      <View style={{ flex: 1, gap: theme.space.xs }}>
        <View
          style={[
            styles.previewBadge,
            { backgroundColor: badge.bg, borderRadius: theme.radius.full },
          ]}
        >
          <Text style={[theme.typography.caption, { color: badge.text }]}>
            {categoryLabel(product.category)}
          </Text>
        </View>
        <Text
          numberOfLines={1}
          style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
        >
          {product.name}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          등록일 {registeredAt}
        </Text>
      </View>
    </View>
  );
}

/** "2026-09-01T..." → "2026. 09. 01" */
function formatDate(iso: string): string {
  const d = new Date(iso);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}. ${month}. ${day}`;
}

const styles = StyleSheet.create({
  previewRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  previewThumb: { width: 72, height: 72, alignItems: 'center', justifyContent: 'center' },
  previewImage: { width: 48, height: 48 },
  previewBadge: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 3 },
});
