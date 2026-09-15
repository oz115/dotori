import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import {
  DESCRIPTION_MAX,
  NAME_MAX,
  type StoreProductFormErrors,
} from '@/features/store/lib/storeProductForm';
import type { StoreProductCategory } from '@/features/store/model/types';
import { useTheme } from '@/shared/theme';
import { Input, SelectableCard } from '@/shared/ui';

const CATEGORIES: StoreProductCategory[] = ['GAME', 'SNACK', 'TOY', 'OUTING', 'PRIVILEGE', 'ETC'];

type StoreProductFieldsProps = {
  name: string;
  onNameChange: (value: string) => void;
  description: string;
  onDescriptionChange: (value: string) => void;
  category: StoreProductCategory | null;
  onCategoryChange: (category: StoreProductCategory) => void;
  quantity: number;
  onQuantityChange: (value: number) => void;
  errors: StoreProductFormErrors;
};

/**
 * 상품명/설명/카테고리/수량 입력 — 등록(SHOP-03)·수정(SHOP-04) 화면이 공유한다.
 * 가격은 화면마다 다뤄야 하는 방식이 달라서(등록엔 없음, 수정은 null일 때 막음) 여기 없다.
 */
export function StoreProductFields({
  name,
  onNameChange,
  description,
  onDescriptionChange,
  category,
  onCategoryChange,
  quantity,
  onQuantityChange,
  errors,
}: StoreProductFieldsProps) {
  const theme = useTheme();

  return (
    <>
      <View style={{ gap: 6 }}>
        <FieldLabel required>상품명</FieldLabel>
        <Input
          variant="box"
          placeholder="예: 게임 30분 이용권"
          value={name}
          onChangeText={onNameChange}
          maxLength={NAME_MAX}
          error={errors.name}
        />
        <CharCounter current={name.length} max={NAME_MAX} />
      </View>

      <View style={{ gap: 6 }}>
        <FieldLabel>설명 (선택)</FieldLabel>
        <Input
          variant="box"
          placeholder="예: 정해진 시간 외에 추가로 게임을 하고 싶을 때 구매"
          value={description}
          onChangeText={onDescriptionChange}
          multiline
          numberOfLines={3}
          maxLength={DESCRIPTION_MAX}
          error={errors.description}
        />
        <CharCounter current={description.length} max={DESCRIPTION_MAX} />
      </View>

      <View style={{ gap: theme.space.sm }}>
        <FieldLabel required>카테고리</FieldLabel>
        <View style={styles.categoryGrid}>
          {CATEGORIES.map((key) => {
            const selected = category === key;
            return (
              <SelectableCard
                key={key}
                selected={selected}
                onPress={() => onCategoryChange(key)}
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

      <QuantityStepper value={quantity} onChange={onQuantityChange} error={errors.quantity} />
    </>
  );
}

/** 필드 라벨. required 면 빨간 별표를 옆에 붙인다 — Input의 label prop은 문자열만 받아서 못 쓴다. */
function FieldLabel({ children, required = false }: { children: ReactNode; required?: boolean }) {
  const theme = useTheme();
  return (
    <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
      {children}
      {required ? <Text style={{ color: theme.color.status.danger.solid }}> *</Text> : null}
    </Text>
  );
}

/** 입력 아래 우측에 붙는 글자수 카운터. 백엔드 제약(NAME_MAX/DESCRIPTION_MAX)과 같은 값을 쓴다. */
function CharCounter({ current, max }: { current: number; max: number }) {
  const theme = useTheme();
  return (
    <Text style={[theme.typography.caption, styles.charCounter, { color: theme.color.text.muted }]}>
      {current}/{max}
    </Text>
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
      <FieldLabel required>재고 수량</FieldLabel>
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
    width: 32,
    height: 32,
  },
  charCounter: {
    textAlign: 'right',
  },
});
