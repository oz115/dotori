import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';

import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { isPriceReady, isSoldOut } from '@/features/store/lib/productStatus';
import { useDeleteStoreProduct } from '@/features/store/model/useDeleteStoreProduct';
import { useStoreProduct } from '@/features/store/model/useStoreProduct';
import { DeleteProductModal } from '@/features/store/ui/DeleteProductModal';
import { useTheme } from '@/shared/theme';
import { Button, Header } from '@/shared/ui';

type StoreProductDetailProps = {
  familyId: number | undefined;
  productId: number | undefined;
  /** 로그인한 사용자 역할. 하단 액션(구매하기 vs 수정/삭제) 분기에만 쓴다 — auth 슬라이스를
   * 이 feature 가 직접 import 하지 않도록 호출하는 라우트가 넘겨준다. */
  role: 'PARENT' | 'CHILD' | undefined;
  /** 등록자 표시 이름. family 슬라이스(구성원 목록)에서 createdBy 로 찾은 값을 라우트가 넘겨준다. */
  registrantName: string | undefined;
};

/**
 * 상점 상품 상세 (SHOP-02). 부모/아이 공통 화면이고, 하단 액션만 역할로 갈린다.
 * 구매(S15P21A505-186)는 아직 별도 작업이라 버튼만 배치해 둔다.
 */
export function StoreProductDetailScreen({
  familyId,
  productId,
  role,
  registrantName,
}: StoreProductDetailProps) {
  const theme = useTheme();
  const { data: product, isPending, isError } = useStoreProduct(familyId, productId);

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title={product?.name ?? '상품 상세'}
        onBack={() => (router.canGoBack() ? router.back() : router.replace('/store'))}
      />

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
        <StoreProductDetailBody
          familyId={familyId}
          product={product}
          registrantName={registrantName}
          role={role}
        />
      )}
    </View>
  );
}

function Centered({ children }: { children: ReactNode }) {
  return <View style={styles.centered}>{children}</View>;
}

type BodyProps = {
  familyId: number | undefined;
  product: NonNullable<ReturnType<typeof useStoreProduct>['data']>;
  registrantName: string | undefined;
  role: 'PARENT' | 'CHILD' | undefined;
};

function StoreProductDetailBody({ familyId, product, registrantName, role }: BodyProps) {
  const theme = useTheme();
  const badge = categoryBadgeColors(theme, product.category);
  const soldOut = isSoldOut(product.quantity);
  const priceReady = isPriceReady(product.price);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const deleteProduct = useDeleteStoreProduct(familyId, product.storeProductId);

  const handleDeleteConfirm = () => {
    deleteProduct.mutate(undefined, {
      onSuccess: () => {
        setDeleteOpen(false);
        router.replace('/store');
      },
    });
  };
  const registeredAt = new Date(product.createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.layout.screenY,
          gap: theme.space.xl,
        }}
      >
        <View
          style={[
            styles.imageBox,
            {
              backgroundColor: badge.bg,
              borderRadius: theme.radius.xl,
              opacity: soldOut ? 0.5 : 1,
            },
          ]}
        >
          <Image
            source={categoryImage(product.category)}
            style={styles.image}
            resizeMode="contain"
          />
          {soldOut ? (
            <View style={[styles.soldOutBadge, { borderRadius: theme.radius.full }]}>
              <Text style={[theme.typography.label, styles.soldOutText]}>품절</Text>
            </View>
          ) : null}
        </View>

        <View style={{ gap: theme.space.sm }}>
          <View
            style={[
              styles.categoryBadge,
              {
                backgroundColor: badge.bg,
                borderRadius: theme.radius.full,
                alignSelf: 'flex-start',
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: badge.text }]}>
              {categoryLabel(product.category)}
            </Text>
          </View>
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
            {product.name}
          </Text>
          {product.description ? (
            <Text style={[theme.typography.body, { color: theme.color.text.default }]}>
              {product.description}
            </Text>
          ) : null}
        </View>

        <View
          style={[
            styles.infoCard,
            {
              backgroundColor: theme.color.background.surfaceSunken,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.md,
            },
          ]}
        >
          <InfoRow label="가격">
            {priceReady ? (
              <View style={styles.priceRow}>
                <Image
                  source={require('@/assets/images/icons/icon-dotori.png')}
                  style={styles.acornIcon}
                  resizeMode="contain"
                />
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {product.price?.toLocaleString('ko-KR')}
                </Text>
              </View>
            ) : (
              <View style={styles.priceRow}>
                <Ionicons name="sparkles" size={14} color={theme.color.brand.solid} />
                <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
                  AI가 가격 산정 중
                </Text>
              </View>
            )}
          </InfoRow>
          <InfoRow label="재고">
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {soldOut ? '품절' : `${product.quantity}개`}
            </Text>
          </InfoRow>
          <InfoRow label="등록자">
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {registrantName ?? '알 수 없음'}
            </Text>
          </InfoRow>
          <InfoRow label="등록일">
            <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
              {registeredAt}
            </Text>
          </InfoRow>
        </View>
      </ScrollView>

      <View
        style={[
          styles.actionBar,
          {
            padding: theme.space.lg,
            gap: theme.space.md,
            borderTopColor: theme.color.border.default,
            backgroundColor: theme.color.background.surface,
          },
        ]}
      >
        {role === 'PARENT' ? (
          <>
            <Button
              label="수정"
              variant="outline"
              style={styles.flex}
              onPress={() =>
                router.push({
                  pathname: '/store/[productId]/edit',
                  params: { productId: String(product.storeProductId) },
                })
              }
            />
            <Button
              label="삭제"
              variant="outline"
              style={styles.flex}
              onPress={() => setDeleteOpen(true)}
            />
          </>
        ) : (
          <Button
            label={soldOut ? '품절' : priceReady ? '구매하기' : 'AI가 가격 산정 중'}
            variant="primary"
            disabled={soldOut || !priceReady}
            style={styles.flex}
            // TODO: S15P21A505-186 상품 구매 태스크에서 연결한다
            onPress={() => {}}
          />
        )}
      </View>

      <DeleteProductModal
        visible={deleteOpen}
        onCancel={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        loading={deleteProduct.isPending}
      />
    </>
  );
}

function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  const theme = useTheme();
  return (
    <View style={styles.infoRow}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>{label}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  imageBox: { aspectRatio: 1.5, alignItems: 'center', justifyContent: 'center' },
  image: { height: '75%', aspectRatio: 1 },
  soldOutBadge: {
    position: 'absolute',
    top: 12,
    right: 12,
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: 'rgba(39, 44, 38, 0.75)',
  },
  soldOutText: { color: '#FFFFFF' },
  categoryBadge: { paddingHorizontal: 10, paddingVertical: 3 },
  infoCard: {},
  infoRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
  actionBar: { flexDirection: 'row', borderTopWidth: StyleSheet.hairlineWidth },
});
