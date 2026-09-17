import { fontFamily, type Theme } from '@/shared/theme';

/**
 * 앱 디자인 토큰을 react-native-calendars 의 theme 프롭 형태로 옮긴다.
 *
 * 라이브러리가 자체 색·폰트 체계를 쓰기 때문에 화면마다 따로 매핑하면 달력만 다른 앱처럼
 * 보인다. 거래내역·할 일 등록이 같은 달력을 쓰므로 여기 한 곳에 둔다.
 */
export function buildCalendarTheme(theme: Theme) {
  return {
    backgroundColor: theme.color.background.surface,
    calendarBackground: theme.color.background.surface,
    textSectionTitleColor: theme.color.text.muted,
    dayTextColor: theme.color.text.default,
    textDisabledColor: theme.color.text.muted,
    todayTextColor: theme.color.brand.solid,
    monthTextColor: theme.color.text.strong,
    arrowColor: theme.color.brand.solid,
    selectedDayBackgroundColor: theme.color.brand.solid,
    selectedDayTextColor: theme.color.text.onBrand,
    textDayFontFamily: fontFamily.regular,
    textMonthFontFamily: fontFamily.semibold,
    textDayHeaderFontFamily: fontFamily.medium,
  };
}
