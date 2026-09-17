import { formatNotificationTime } from './formatNotificationTime';

describe('formatNotificationTime', () => {
  const now = new Date(2026, 8, 16, 15, 30, 0); // 2026-09-16 15:30

  it('1분 미만이면 "방금 전"', () => {
    const iso = new Date(2026, 8, 16, 15, 29, 40).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('방금 전');
  });

  it('1시간 미만이면 "N분 전"', () => {
    const iso = new Date(2026, 8, 16, 15, 10, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('20분 전');
  });

  it('같은 날 1시간 이상이면 "N시간 전"', () => {
    const iso = new Date(2026, 8, 16, 12, 0, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('3시간 전');
  });

  it('어제면 "어제 오후 4:21"', () => {
    const iso = new Date(2026, 8, 15, 16, 21, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('어제 오후 4:21');
  });

  it('어제도 아니고 올해면 "9월 11일 오전 10:14"', () => {
    const iso = new Date(2026, 8, 11, 10, 14, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('9월 11일 오전 10:14');
  });

  it('연도가 다르면 연도를 붙인다', () => {
    const iso = new Date(2025, 11, 25, 9, 0, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('2025년 12월 25일 오전 9:00');
  });

  it('자정(0시)은 오전 12시로 표기한다', () => {
    const iso = new Date(2026, 8, 15, 0, 5, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('어제 오전 12:05');
  });

  it('정오(12시)는 오후 12시로 표기한다', () => {
    const iso = new Date(2026, 8, 15, 12, 0, 0).toISOString();
    expect(formatNotificationTime(iso, now)).toBe('어제 오후 12:00');
  });
});
