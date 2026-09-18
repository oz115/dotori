import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';

import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { categoryTagline } from '@/features/store/lib/categoryTagline';
import { isPriceReady, isSoldOut } from '@/features/store/lib/productStatus';
import { purchaseErrorMessage } from '@/features/store/lib/purchaseError';
import type { StorePurchaseResult } from '@/features/store/model/types';
import { useDeleteStoreProduct } from '@/features/store/model/useDeleteStoreProduct';
import { usePurchaseStoreProduct } from '@/features/store/model/usePurchaseStoreProduct';
import { useStoreProduct } from '@/features/store/model/useStoreProduct';
import { DeleteProductModal } from '@/features/store/ui/DeleteProductModal';
import { PurchaseConfirmModal } from '@/features/store/ui/PurchaseConfirmModal';
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
  /** 자녀의 도토리 잔액. wallet 슬라이스 값이라 라우트가 넘겨준다. 부모 화면에선 안 쓴다. */
  balance: number | undefined;
  /** 구매 성공 후 라우트가 지갑 잔액 캐시를 갱신할 수 있게 알려준다. */
  onPurchased?: () => void;
  /**
   * 뒤로 갈 스택이 없을 때(딥링크 등)와 삭제 성공 후 목록으로 돌아갈 때 쓴다.
   * (child)/(parent) 둘 다 `/store`가 있어 그룹 접두사 없이 push/replace하면
   * expo-router가 어느 쪽인지 못 정한다 — 그래서 라우트가 정확한 경로를 만들어 넘긴다.
   */
  onNavigateToList: () => void;
};

/**
 * 상점 상품 상세 (SHOP-02). 부모/아이 공통 화면이고, 하단 액션만 역할로 갈린다.
 */
export function StoreProductDetailScreen({
  familyId,
  productId,
  role,
  registrantName,
  balance,
  onPurchased,
  onNavigateToList,
}: StoreProductDetailProps) {
  const theme = useTheme();
  const { data: product, isPending, isError } = useStoreProduct(familyId, productId);

  return (
    <View style={[styles.flex, { backgroundColor: theme.color.background.canvas }]}>
      <Header
        title={product?.name ?? '상품 상세'}
        onBack={() => (router.canGoBack() ? router.back() : onNavigateToList())}
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
          balance={balance}
          onPurchased={onPurchased}
          onNavigateToList={onNavigateToList}
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
  balance: number | undefined;
  onPurchased?: () => void;
  onNavigateToList: () => void;
};

function StoreProductDetailBody({
  familyId,
  product,
  registrantName,
  role,
  balance,
  onPurchased,
  onNavigateToList,
}: BodyProps) {
  const theme = useTheme();
  const badge = categoryBadgeColors(theme, product.category);
  const soldOut = isSoldOut(product.quantity);
  const priceReady = isPriceReady(product.price);
  const registeredAt = new Date(product.createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const [deleteOpen, setDeleteOpen] = useState(false);
  const deleteProduct = useDeleteStoreProduct(familyId, product.storeProductId);

  const handleDeleteConfirm = () => {
    deleteProduct.mutate(undefined, {
      onSuccess: () => {
        setDeleteOpen(false);
        onNavigateToList();
      },
    });
  };

  const [purchaseOpen, setPurchaseOpen] = useState(false);
  const [purchaseResult, setPurchaseResult] = useState<StorePurchaseResult | null>(null);
  const purchaseProduct = usePurchaseStoreProduct(familyId, product.storeProductId);
  const insufficientBalance = balance != null && product.price != null && balance < product.price;

  const openPurchase = () => {
    purchaseProduct.reset();
    setPurchaseResult(null);
    setPurchaseOpen(true);
  };
  const handlePurchaseConfirm = () => {
    purchaseProduct.mutate(undefined, {
      onSuccess: (result) => {
        setPurchaseResult(result);
        onPurchased?.();
      },
    });
  };
  const closePurchase = () => {
    setPurchaseOpen(false);
    setPurchaseResult(null);
  };

  const purchaseLabel = soldOut
    ? '품절'
    : !priceReady
      ? 'AI가 가격 산정 중'
      : insufficientBalance
        ? '도토리 부족'
        : '구매하기';
  const purchaseDisabled = soldOut || !priceReady || insufficientBalance;

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
            styles.banner,
            {
              backgroundColor: badge.bg,
              borderRadius: theme.radius.xl,
              opacity: soldOut ? 0.6 : 1,
            },
          ]}
        >
          <View style={{ flex: 1, gap: theme.space.xs }}>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              {categoryTagline(product.category)}
            </Text>
            <View style={[styles.taglineUnderline, { backgroundColor: theme.color.brand.solid }]} />
          </View>
          <Image
            source={categoryImage(product.category)}
            style={styles.bannerImage}
            resizeMode="contain"
          />
          {soldOut ? (
            <View style={[styles.soldOutBadge, { borderRadius: theme.radius.full }]}>
              <Text style={[theme.typography.label, styles.soldOutText]}>품절</Text>
            </View>
          ) : null}
        </View>

        <View
          style={[
            styles.card,
            {
              backgroundColor: theme.color.background.surface,
              borderColor: theme.color.border.default,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.sm,
            },
          ]}
        >
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
          <View style={styles.footerRow}>
            {priceReady ? (
              <View style={styles.priceRow}>
                <Image
                  source={require('@/assets/images/icons/icon-dotori.png')}
                  style={styles.acornIcon}
                  resizeMode="contain"
                />
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {product.price?.toLocaleString('ko-KR')}P
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
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {soldOut ? '품절' : `재고 ${product.quantity}개`}
            </Text>
          </View>
        </View>

        {product.description ? (
          <View
            style={[
              styles.card,
              {
                backgroundColor: theme.color.background.surface,
                borderColor: theme.color.border.default,
                borderRadius: theme.radius.lg,
                padding: theme.space.lg,
                gap: theme.space.sm,
              },
            ]}
          >
            <View style={styles.descHeader}>
              <View
                style={[
                  styles.descIcon,
                  { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
                ]}
              >
                <Ionicons name="document-text-outline" size={16} color={theme.color.brand.solid} />
              </View>
              <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                상품 설명
              </Text>
            </View>
            <Text style={[theme.typography.body, { color: theme.color.text.default }]}>
              {product.description}
            </Text>
          </View>
        ) : null}

        {role === 'PARENT' && product.reason ? (
          <View
            style={[
              styles.card,
              {
                backgroundColor: theme.color.background.surface,
                borderColor: theme.color.border.default,
                borderRadius: theme.radius.lg,
                padding: theme.space.lg,
                gap: theme.space.sm,
              },
            ]}
          >
            <View style={styles.descHeader}>
              <View
                style={[
                  styles.descIcon,
                  { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
                ]}
              >
                <Ionicons name="sparkles" size={16} color={theme.color.brand.solid} />
              </View>
              <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                AI 산정 이유
              </Text>
            </View>
            <Text style={[theme.typography.body, { color: theme.color.text.default }]}>
              {product.reason}
            </Text>
          </View>
        ) : null}

        <View
          style={[
            styles.infoCard,
            {
              backgroundColor: theme.color.background.surfaceSunken,
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.lg,
            },
          ]}
        >
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
          {role === 'CHILD' ? (
            <InfoRow label="구매 제한">
              <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                하루 1회
              </Text>
            </InfoRow>
          ) : null}
        </View>

        {role === 'CHILD' ? (
          <View
            style={[
              styles.noticeRow,
              {
                backgroundColor: theme.color.brand.subtle,
                borderRadius: theme.radius.lg,
                padding: theme.space.md,
              },
            ]}
          >
            <Image
              source={require('@/assets/images/store/mascot-purchase-note.png')}
              style={styles.noticeMascot}
              resizeMode="contain"
            />
            <Text
              style={[theme.typography.bodySm, styles.flex, { color: theme.color.text.default }]}
            >
              구매하면 부모님께 알림이 전송돼요
            </Text>
          </View>
        ) : null}
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
              label="삭제"
              variant="outline"
              tone="danger"
              style={styles.deleteButton}
              onPress={() => setDeleteOpen(true)}
            />
            <Button
              label="수정"
              variant="outline"
              style={styles.editButton}
              onPress={() =>
                router.push({
                  pathname: '/store/[productId]/edit',
                  params: { productId: String(product.storeProductId) },
                })
              }
            />
          </>
        ) : (
          <>
            <View style={{ gap: 2 }}>
              <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
                내 도토리
              </Text>
              <View style={styles.priceRow}>
                <Image
                  source={require('@/assets/images/icons/icon-dotori.png')}
                  style={styles.acornIcon}
                  resizeMode="contain"
                />
                <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                  {balance != null ? balance.toLocaleString('ko-KR') : '—'}
                </Text>
              </View>
            </View>
            <Button
              label={purchaseLabel}
              variant="primary"
              disabled={purchaseDisabled}
              style={styles.purchaseButton}
              onPress={openPurchase}
            />
          </>
        )}
      </View>

      <DeleteProductModal
        visible={deleteOpen}
        onCancel={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        loading={deleteProduct.isPending}
      />

      {priceReady ? (
        <PurchaseConfirmModal
          visible={purchaseOpen}
          productName={product.name}
          price={product.price as number}
          loading={purchaseProduct.isPending}
          error={purchaseProduct.isError ? purchaseErrorMessage(purchaseProduct.error) : null}
          result={purchaseResult}
          onCancel={() => setPurchaseOpen(false)}
          onConfirm={handlePurchaseConfirm}
          onClose={closePurchase}
        />
      ) : null}
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
  banner: { flexDirection: 'row', alignItems: 'center', padding: 20, gap: 12 },
  bannerImage: { width: 96, height: 96 },
  taglineUnderline: { width: 32, height: 3, borderRadius: 2 },
  card: { borderWidth: 1 },
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
  descHeader: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  descIcon: { width: 28, height: 28, alignItems: 'center', justifyContent: 'center' },
  infoCard: {},
  infoRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  footerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
  noticeRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  noticeMascot: { width: 40, height: 40 },
  actionBar: {
    flexDirection: 'row',
    borderTopWidth: StyleSheet.hairlineWidth,
    alignItems: 'center',
  },
  deleteButton: { flex: 1 },
  editButton: { flex: 2 },
  purchaseButton: { flex: 1 },
});
