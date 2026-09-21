import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Calendar, type DateData } from 'react-native-calendars';

// 부작용 import — react-native-calendars에 한글 월/요일 이름을 등록한다
import '@/shared/lib/calendarLocale';
import { buildCalendarTheme } from '@/shared/lib/calendarTheme';
import {
  addDays,
  addMonths,
  formatDateShort,
  fromDateParam,
  minDate as earlierOf,
  toDateParam,
} from '@/shared/lib/dateRange';
import { useTheme, type Theme } from '@/shared/theme';

type Props = {
  start: Date;
  end: Date;
  onChange: (start: Date, end: Date) => void;
  /** 시작~종료 최대 폭(개월). 서버 제약이 있는 화면(실계좌 3개월)만 넘긴다 */
  maxRangeMonths?: number;
  /** 달력에서 고를 수 있는 가장 이른 날짜. 안 넘기면 제한 없음 */
  minPickableDate?: Date;
};

/**
 * 시작일~종료일 범위 달력 (실계좌 거래내역이 원형, 모든 기간 조회 화면이 같이 쓴다).
 *
 * 위의 시작일/종료일 칸이 "지금 무엇을 고치는지"를 정한다 — 시작일 칸을 누른 상태에선
 * 달력을 몇 번을 눌러도 시작일만 바뀌고, 종료일 칸을 누르면 종료일만 바뀐다.
 * (예전의 "첫 탭=시작일, 둘째 탭=종료일" 번갈이 방식은 탭을 반복하면 의도와 다른 쪽이
 * 바뀌어 헷갈린다는 피드백으로 바꿨다.)
 */
export function DateRangeCalendar({
  start,
  end,
  onChange,
  maxRangeMonths,
  minPickableDate,
}: Props) {
  const theme = useTheme();
  const [editing, setEditing] = useState<'start' | 'end'>('start');
  const today = new Date();

  const startStr = toDateParam(start);
  const endStr = toDateParam(end);

  const handleDayPress = (day: DateData) => {
    const picked = fromDateParam(day.dateString);
    if (editing === 'start') {
      // 시작일만 움직인다. 종료일이 시작일보다 앞서게 되면 같은 날로 끌어온다.
      let nextEnd = end < picked ? picked : end;
      if (maxRangeMonths != null) {
        nextEnd = earlierOf(nextEnd, addMonths(picked, maxRangeMonths));
      }
      onChange(picked, earlierOf(nextEnd, today));
      return;
    }
    // 종료일만 움직인다. 시작일보다 앞이나 최대 폭 밖을 고르면 경계로 끌어온다.
    let nextEnd = picked < start ? start : picked;
    if (maxRangeMonths != null) {
      nextEnd = earlierOf(nextEnd, addMonths(start, maxRangeMonths));
    }
    onChange(start, earlierOf(nextEnd, today));
  };

  const markedDates = useMemo(
    () => buildRangeMarking(startStr, endStr, theme),
    [startStr, endStr, theme],
  );

  return (
    <View style={{ gap: theme.space.sm }}>
      <View style={[styles.dateRow, { gap: theme.space.sm }]}>
        <DateField
          label="시작일"
          date={start}
          active={editing === 'start'}
          onPress={() => setEditing('start')}
        />
        <Text style={[theme.typography.body, { color: theme.color.text.muted }]}>~</Text>
        <DateField
          label="종료일"
          date={end}
          active={editing === 'end'}
          onPress={() => setEditing('end')}
        />
      </View>
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
        {editing === 'start'
          ? '달력에서 시작일을 고르는 중이에요. 종료일을 바꾸려면 종료일 칸을 눌러 주세요.'
          : '달력에서 종료일을 고르는 중이에요. 시작일을 바꾸려면 시작일 칸을 눌러 주세요.'}
      </Text>

      <Calendar
        current={editing === 'start' ? startStr : endStr}
        minDate={minPickableDate != null ? toDateParam(minPickableDate) : undefined}
        maxDate={toDateParam(today)}
        markingType="period"
        markedDates={markedDates}
        onDayPress={handleDayPress}
        theme={buildCalendarTheme(theme)}
        style={[styles.calendar, { borderColor: theme.color.border.default }]}
      />
    </View>
  );
}

function DateField({
  label,
  date,
  active,
  onPress,
}: {
  label: string;
  date: Date;
  active: boolean;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected: active }}
      accessibilityLabel={`${label} 고르기`}
      onPress={onPress}
      style={[
        styles.dateField,
        {
          borderColor: active ? theme.color.brand.solid : theme.color.border.default,
          backgroundColor: active ? theme.color.brand.subtle : theme.color.background.surface,
          borderRadius: theme.radius.md,
          padding: theme.space.sm,
        },
      ]}
    >
      <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>{label}</Text>
      <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
        {formatDateShort(date)}
      </Text>
    </Pressable>
  );
}

/** 시작일 ~ 종료일 사이 모든 날짜를 브랜드 컬러로 칠한다 (양 끝은 둥글게). */
function buildRangeMarking(startStr: string, endStr: string, theme: Theme) {
  const marks: Record<
    string,
    { color: string; textColor: string; startingDay?: boolean; endingDay?: boolean }
  > = {};
  let cursor = fromDateParam(startStr);

  // 'YYYY-MM-DD' 문자열은 사전순 비교가 곧 날짜순 비교라 Date 객체 없이 비교할 수 있다.
  for (let key = toDateParam(cursor); key <= endStr; key = toDateParam(cursor)) {
    marks[key] = {
      color: theme.color.brand.solid,
      textColor: theme.color.text.onBrand,
      startingDay: key === startStr,
      endingDay: key === endStr,
    };
    cursor = addDays(cursor, 1);
  }
  return marks;
}

const styles = StyleSheet.create({
  dateRow: { flexDirection: 'row', alignItems: 'center' },
  dateField: { flex: 1, borderWidth: 1 },
  calendar: { borderWidth: 1, borderRadius: 12 },
});
