import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Image, Modal, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Calendar } from 'react-native-calendars';

import {
  DESCRIPTION_MAX,
  TITLE_MAX,
  isRepeating,
  type ChoreFormErrors,
  type ChoreFormInput,
} from '@/features/chore/lib/choreForm';
import {
  DIFFICULTIES,
  REPEAT_TYPES,
  difficultyLabel,
  repeatLabel,
} from '@/features/chore/lib/choreLabels';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import type { ChoreDifficulty, ChoreRepeatType } from '@/features/chore/model/types';
import { ChildBadge } from '@/features/chore/ui/ChildBadge';
import '@/shared/lib/calendarLocale';
import { buildCalendarTheme } from '@/shared/lib/calendarTheme';
import { useTheme } from '@/shared/theme';
import { Input } from '@/shared/ui';

const DIFFICULTY_IMAGE: Record<ChoreDifficulty, number> = {
  EASY: require('@/assets/images/chore/difficulty-easy.png'),
  NORMAL: require('@/assets/images/chore/difficulty-normal.png'),
  HARD: require('@/assets/images/chore/difficulty-hard.png'),
};

const REPEAT_ICON: Record<ChoreRepeatType, 'calendar-outline' | 'sync-outline'> = {
  ONCE: 'calendar-outline',
  DAY: 'sync-outline',
  WEEK: 'calendar-outline',
  MONTH: 'calendar-outline',
};

type ChoreFormFieldsProps = {
  value: ChoreFormInput;
  onChange: (next: ChoreFormInput) => void;
  errors: ChoreFormErrors;
  /** 등록에서만 고른다. 수정에서는 넘기지 않는다 - 서버가 담당 아이 변경을 받지 않는다 */
  childList?: ChoreChild[];
  /** 오늘 (YYYY-MM-DD). 달력의 최소 날짜 */
  today: string;
};

/**
 * 할 일 등록·수정이 함께 쓰는 입력 묶음.
 *
 * 보상 입력란은 없다. 서버가 등록 요청에서 보상을 받지 않고 AI 가 산정하기 때문이다
 * (수정 화면은 보상만 따로 자기 자리에서 다룬다).
 */
export function ChoreFormFields({
  value,
  onChange,
  errors,
  childList,
  today,
}: ChoreFormFieldsProps) {
  const theme = useTheme();
  const patch = (next: Partial<ChoreFormInput>) => onChange({ ...value, ...next });

  return (
    <View style={{ gap: theme.space.xl }}>
      {childList ? (
        <Field label="담당 아이" error={errors.childId}>
          <View style={[styles.chips, { gap: theme.space.sm }]}>
            {childList.map((child) => {
              const selected = child.childId === value.childId;
              return (
                <Pressable
                  key={child.childId}
                  onPress={() => patch({ childId: child.childId })}
                  accessibilityRole="radio"
                  accessibilityState={{ selected }}
                  style={[
                    styles.selectable,
                    {
                      borderRadius: theme.radius.md,
                      borderColor: selected ? theme.color.brand.solid : theme.color.border.default,
                      backgroundColor: selected
                        ? theme.color.brand.tint
                        : theme.color.background.surface,
                    },
                  ]}
                >
                  <ChildBadge name={child.name} />
                </Pressable>
              );
            })}
          </View>
        </Field>
      ) : null}

      <Input
        label="할 일 제목"
        variant="box"
        value={value.title}
        onChangeText={(title) => patch({ title })}
        maxLength={TITLE_MAX}
        placeholder="예) 수학 문제집 1페이지 풀기"
        error={errors.title}
      />

      <Field label="할 일 설명 (선택)" error={errors.description}>
        <View
          style={[
            styles.textBox,
            { borderColor: theme.color.border.default, borderRadius: theme.radius.md },
          ]}
        >
          <TextInput
            value={value.description}
            onChangeText={(description) => patch({ description })}
            maxLength={DESCRIPTION_MAX}
            multiline
            placeholder="어떤 내용을 할 지 자세히 설명해주세요."
            placeholderTextColor={theme.color.text.muted}
            style={[theme.typography.bodySm, styles.textInput, { color: theme.color.text.default }]}
          />
          <Text
            style={[theme.typography.caption, styles.counter, { color: theme.color.text.muted }]}
          >
            {value.description.length}/{DESCRIPTION_MAX}
          </Text>
        </View>
      </Field>

      <Field label="난이도">
        <View style={[styles.chips, { gap: theme.space.sm }]}>
          {DIFFICULTIES.map((difficulty) => {
            const selected = difficulty === value.difficulty;
            return (
              <Pressable
                key={difficulty}
                onPress={() => patch({ difficulty })}
                accessibilityRole="radio"
                accessibilityState={{ selected }}
                style={[
                  styles.selectable,
                  styles.grow,
                  {
                    borderRadius: theme.radius.md,
                    borderColor: selected ? theme.color.brand.solid : theme.color.border.default,
                    backgroundColor: selected
                      ? theme.color.brand.tint
                      : theme.color.background.surface,
                  },
                ]}
              >
                <Image
                  source={DIFFICULTY_IMAGE[difficulty]}
                  style={styles.sprout}
                  resizeMode="contain"
                />
                <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
                  {difficultyLabel(difficulty)}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </Field>

      <Field label="반복 설정">
        <View style={[styles.chips, { gap: theme.space.sm }]}>
          {REPEAT_TYPES.map((repeatType) => {
            const selected = repeatType === value.repeatType;
            return (
              <Pressable
                key={repeatType}
                onPress={() => patch({ repeatType })}
                accessibilityRole="radio"
                accessibilityState={{ selected }}
                style={[
                  styles.selectable,
                  styles.grow,
                  {
                    borderRadius: theme.radius.md,
                    borderColor: selected ? theme.color.brand.solid : theme.color.border.default,
                    backgroundColor: selected
                      ? theme.color.brand.tint
                      : theme.color.background.surface,
                  },
                ]}
              >
                <Ionicons
                  name={REPEAT_ICON[repeatType]}
                  size={16}
                  color={theme.color.text.default}
                />
                <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
                  {repeatLabel(repeatType)}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </Field>

      {/* 반복은 서버가 주기에서 기간을 계산한다. 물어봐도 무시되므로 아예 감춘다 */}
      {isRepeating(value.repeatType) ? null : (
        <Field label="언제까지" error={errors.endDate}>
          <DateField
            value={value.endDate}
            today={today}
            onChange={(endDate) => patch({ endDate })}
          />
        </Field>
      )}
    </View>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.sm }}>
      <Text style={[theme.typography.label, { color: theme.color.text.label }]}>{label}</Text>
      {children}
      {error ? (
        <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

/** 누르면 달력이 뜨는 날짜 칸. 지난 날짜는 고를 수 없다 */
function DateField({
  value,
  today,
  onChange,
}: {
  value: string | null;
  today: string;
  onChange: (date: string | null) => void;
}) {
  const theme = useTheme();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        accessibilityRole="button"
        style={[
          styles.dateField,
          {
            height: theme.layout.inputHeight,
            borderColor: theme.color.border.default,
            borderRadius: theme.radius.md,
            paddingHorizontal: theme.space.lg,
            gap: theme.space.sm,
          },
        ]}
      >
        <Ionicons name="calendar-outline" size={18} color={theme.color.text.muted} />
        <Text
          style={[
            theme.typography.body,
            styles.grow,
            { color: value ? theme.color.text.default : theme.color.text.muted },
          ]}
        >
          {value ?? '날짜를 선택해주세요'}
        </Text>
        {value ? (
          <Pressable
            onPress={() => onChange(null)}
            hitSlop={8}
            accessibilityRole="button"
            accessibilityLabel="날짜 지우기"
          >
            <Ionicons name="close" size={18} color={theme.color.text.muted} />
          </Pressable>
        ) : null}
      </Pressable>

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable
          style={[styles.backdrop, { backgroundColor: theme.color.background.overlay }]}
          onPress={() => setOpen(false)}
          accessibilityRole="button"
          accessibilityLabel="달력 닫기"
        >
          <Pressable
            onPress={(event) => event.stopPropagation()}
            style={[
              styles.calendarCard,
              {
                backgroundColor: theme.color.background.surface,
                borderRadius: theme.radius.xl,
                padding: theme.space.md,
              },
            ]}
          >
            <Calendar
              minDate={today}
              current={value ?? today}
              markedDates={value ? { [value]: { selected: true } } : {}}
              onDayPress={(day) => {
                onChange(day.dateString);
                setOpen(false);
              }}
              theme={buildCalendarTheme(theme)}
            />
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  chips: { flexDirection: 'row', flexWrap: 'wrap' },
  selectable: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1.5,
    paddingVertical: 12,
    paddingHorizontal: 12,
  },
  grow: { flex: 1 },
  sprout: { width: 20, height: 20 },
  textBox: { borderWidth: 1, paddingHorizontal: 12, paddingTop: 10, paddingBottom: 6 },
  textInput: { minHeight: 72, textAlignVertical: 'top' },
  counter: { textAlign: 'right' },
  dateField: { flexDirection: 'row', alignItems: 'center', borderWidth: 1 },
  backdrop: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
  calendarCard: { width: '100%' },
});
