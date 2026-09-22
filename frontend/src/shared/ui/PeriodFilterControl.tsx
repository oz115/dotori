import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { formatDateShort, monthsAgo, toDateParam } from '@/shared/lib/dateRange';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui/Button';
import { DateRangeCalendar } from '@/shared/ui/DateRangeCalendar';

export type PeriodPreset = 'ALL' | 'MONTH' | 'THREE_MONTHS' | 'CUSTOM';

export type PeriodFilterValue = {
  preset: PeriodPreset;
  /** 직접설정일 때 쓰는 범위. 프리셋일 땐 periodDateRange 가 오늘 기준으로 계산한다 */
  start: Date;
  end: Date;
};

export function defaultPeriodFilter(): PeriodFilterValue {
  return { preset: 'ALL', start: monthsAgo(1), end: new Date() };
}

/** 조회에 쓸 "YYYY-MM-DD" 범위. 전체 기간이면 null — 필터를 걸지 않는다 */
export function periodDateRange(
  value: PeriodFilterValue,
): { startDate: string; endDate: string } | null {
  const today = new Date();
  switch (value.preset) {
    case 'ALL':
      return null;
    case 'MONTH':
      return { startDate: toDateParam(monthsAgo(1)), endDate: toDateParam(today) };
    case 'THREE_MONTHS':
      return { startDate: toDateParam(monthsAgo(3)), endDate: toDateParam(today) };
    case 'CUSTOM':
      return { startDate: toDateParam(value.start), endDate: toDateParam(value.end) };
  }
}

export function periodLabel(value: PeriodFilterValue): string {
  switch (value.preset) {
    case 'ALL':
      return '전체 기간';
    case 'MONTH':
      return '최근 1개월';
    case 'THREE_MONTHS':
      return '최근 3개월';
    case 'CUSTOM':
      return `${formatDateShort(value.start)} ~ ${formatDateShort(value.end)}`;
  }
}

const PRESET_OPTIONS: { key: PeriodPreset; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'MONTH', label: '1개월' },
  { key: 'THREE_MONTHS', label: '3개월' },
  // 6개월 칩도 넣어 봤지만 한 줄에 안 들어가 직접설정이 다음 줄로 밀렸다 — 네 개 유지
  { key: 'CUSTOM', label: '직접설정' },
];

type Props = {
  value: PeriodFilterValue;
  onChange: (next: PeriodFilterValue) => void;
  /** 직접설정 달력의 하한 (보통 가입일). 가입 전 날짜는 보여줄 데이터가 없다 */
  minPickableDate?: Date;
};

/**
 * 기간 조회 공용 컨트롤 — 요약 알약을 누르면 바텀시트가 열리고, 프리셋 칩과
 * 시작일/종료일 달력(DateRangeCalendar)으로 기간을 고른다. 실계좌 거래내역의
 * 조회 방식을 그대로 가져와 투자 거래·구매 내역 같은 기간 조회 화면이 함께 쓴다.
 * 시트에서 고른 값은 "확인"을 눌러야 반영된다 (달력을 만지는 동안 재조회 방지).
 */
export function PeriodFilterControl({ value, onChange, minPickableDate }: Props) {
  const theme = useTheme();
  const [sheetOpen, setSheetOpen] = useState(false);

  return (
    <>
      <Pressable
        onPress={() => setSheetOpen(true)}
        accessibilityRole="button"
        accessibilityLabel="조회 기간 고르기"
        style={[
          styles.summaryBar,
          {
            backgroundColor: theme.color.background.surfaceSunken,
            borderRadius: theme.radius.full,
            paddingHorizontal: theme.space.lg,
            gap: theme.space.sm,
          },
        ]}
      >
        <Ionicons name="calendar-outline" size={18} color={theme.color.text.muted} />
        <Text
          style={[theme.typography.body, { color: theme.color.text.default, flex: 1 }]}
          numberOfLines={1}
        >
          {periodLabel(value)}
        </Text>
        <Ionicons name="chevron-down" size={18} color={theme.color.text.muted} />
      </Pressable>

      <Modal
        visible={sheetOpen}
        transparent
        animationType="slide"
        onRequestClose={() => setSheetOpen(false)}
      >
        <Pressable
          style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}
          onPress={() => setSheetOpen(false)}
        >
          <Pressable
            style={[
              styles.sheet,
              {
                backgroundColor: theme.color.background.canvas,
                borderTopLeftRadius: theme.radius.xl,
                borderTopRightRadius: theme.radius.xl,
                padding: theme.space.xl,
                gap: theme.space.lg,
              },
            ]}
            onPress={(e) => e.stopPropagation()}
          >
            {/* 열 때마다 draft 가 "지금 적용된 값"에서 새로 시작하도록 열려 있을 때만 마운트 */}
            {sheetOpen ? (
              <PeriodSheetContent
                value={value}
                minPickableDate={minPickableDate}
                onApply={(next) => {
                  onChange(next);
                  setSheetOpen(false);
                }}
              />
            ) : null}
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

function PeriodSheetContent({
  value,
  minPickableDate,
  onApply,
}: {
  value: PeriodFilterValue;
  minPickableDate?: Date;
  onApply: (next: PeriodFilterValue) => void;
}) {
  const theme = useTheme();
  const [draftPreset, setDraftPreset] = useState(value.preset);
  // 기본 시작일(한 달 전)이 하한(가입일)보다 이르면 하한으로 끌어올린다 — 고를 수 없는
  // 날짜가 시작일로 미리 선택돼 있으면 안 된다
  const [draftStart, setDraftStart] = useState(
    minPickableDate != null && value.start < minPickableDate ? minPickableDate : value.start,
  );
  const [draftEnd, setDraftEnd] = useState(value.end);

  return (
    <>
      <View
        style={[styles.grabber, { backgroundColor: theme.color.border.strong }]}
        accessibilityElementsHidden
      />

      <View style={{ gap: theme.space.sm }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          조회 기간
        </Text>
        <View style={[styles.chipRow, { gap: theme.space.sm }]}>
          {PRESET_OPTIONS.map((option) => {
            const active = option.key === draftPreset;
            return (
              <Pressable
                key={option.key}
                accessibilityRole="button"
                accessibilityState={{ selected: active }}
                onPress={() => setDraftPreset(option.key)}
                style={[
                  styles.chip,
                  {
                    borderRadius: theme.radius.full,
                    backgroundColor: active
                      ? theme.color.brand.solid
                      : theme.color.background.surfaceSunken,
                  },
                ]}
              >
                <Text
                  style={[
                    theme.typography.label,
                    { color: active ? theme.color.text.onBrand : theme.color.text.default },
                  ]}
                >
                  {option.label}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </View>

      {draftPreset === 'CUSTOM' ? (
        <DateRangeCalendar
          start={draftStart}
          end={draftEnd}
          minPickableDate={minPickableDate}
          onChange={(start, end) => {
            setDraftStart(start);
            setDraftEnd(end);
          }}
        />
      ) : null}

      <Button
        label="확인"
        variant="primary"
        onPress={() => onApply({ preset: draftPreset, start: draftStart, end: draftEnd })}
      />
    </>
  );
}

const styles = StyleSheet.create({
  summaryBar: { flexDirection: 'row', alignItems: 'center', minHeight: 48 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap' },
  chip: { paddingHorizontal: 16, paddingVertical: 8 },
  backdrop: { flex: 1, justifyContent: 'flex-end' },
  sheet: { width: '100%' },
  grabber: { width: 40, height: 4, borderRadius: 2, alignSelf: 'center' },
});
