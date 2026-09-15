import { LocaleConfig } from 'react-native-calendars';

/**
 * react-native-calendars 한글 로케일 설정. 라이브러리가 기본으로는 영어만 제공해서
 * 직접 등록해야 한다. 부작용(모듈 로드 시 LocaleConfig를 채움)이라 캘린더를 쓰는
 * 화면에서 `import '@/shared/lib/calendarLocale';` 로 한 번만 불러오면 된다.
 */
LocaleConfig.locales.kr = {
  monthNames: [
    '1월',
    '2월',
    '3월',
    '4월',
    '5월',
    '6월',
    '7월',
    '8월',
    '9월',
    '10월',
    '11월',
    '12월',
  ],
  monthNamesShort: [
    '1월',
    '2월',
    '3월',
    '4월',
    '5월',
    '6월',
    '7월',
    '8월',
    '9월',
    '10월',
    '11월',
    '12월',
  ],
  dayNames: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
  dayNamesShort: ['일', '월', '화', '수', '목', '금', '토'],
  today: '오늘',
};
LocaleConfig.defaultLocale = 'kr';
