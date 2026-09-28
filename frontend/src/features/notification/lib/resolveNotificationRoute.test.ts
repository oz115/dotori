import { resolveNotificationRoute } from './resolveNotificationRoute';

describe('resolveNotificationRoute', () => {
  it('routes a PARENT to the parent product detail screen', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'PARENT')).toEqual({
      pathname: '/(parent)/(tabs)/store/[productId]',
      params: { productId: '5', viaDeepLink: '1' },
    });
  });

  it('routes a CHILD to the child product detail screen', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'CHILD')).toEqual({
      pathname: '/(child)/(tabs)/store/[productId]',
      params: { productId: '5', viaDeepLink: '1' },
    });
  });

  it('returns null for PURCHASE when refId is missing', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE' }, 'PARENT')).toBeNull();
    expect(resolveNotificationRoute({ type: 'PURCHASE' }, 'CHILD')).toBeNull();
  });

  it('routes a PARENT CHORE to the completion request list via deep link', () => {
    expect(resolveNotificationRoute({ type: 'CHORE', refId: '3' }, 'PARENT')).toEqual({
      pathname: '/(parent)/(tabs)/task/requests',
      params: { viaDeepLink: '1' },
    });
  });

  describe('알림함에서 탭한 경우 (fromNotificationList)', () => {
    // 푸시에서 곧장 열린 것과 알림함에서 들어온 것은 뒤로가기가 달라야 한다 — 알림함에서
    // 왔으면 알림함으로 돌아가야 하는데, 예전에는 둘을 구분하지 않아 그 탭의 목록으로 갔다 (QA).
    it('상점 상세에는 viaDeepLink 대신 from 을 싣는다', () => {
      expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'PARENT', true)).toEqual({
        pathname: '/(parent)/(tabs)/store/[productId]',
        params: { productId: '5', from: 'notification' },
      });
      expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'CHILD', true)).toEqual({
        pathname: '/(child)/(tabs)/store/[productId]',
        params: { productId: '5', from: 'notification' },
      });
    });

    it('완료 요청 목록에도 from 을 싣는다', () => {
      expect(resolveNotificationRoute({ type: 'CHORE', refId: '3' }, 'PARENT', true)).toEqual({
        pathname: '/(parent)/(tabs)/task/requests',
        params: { from: 'notification' },
      });
    });

    it('탭 바깥 화면은 그대로다 — 알림함 위에 쌓이므로 뒤로가기가 이미 알림함으로 간다', () => {
      expect(resolveNotificationRoute({ type: 'CASHOUT', refId: '7' }, 'PARENT', true)).toBe(
        '/(parent)/wallet/cashouts',
      );
      expect(resolveNotificationRoute({ type: 'INVITATION', refId: '1' }, 'PARENT', true)).toBe(
        '/(parent)/family',
      );
    });
  });

  it.each([
    ['CASHOUT', '7', '/(parent)/wallet/cashouts'],
    ['CHARGE', undefined, '/(parent)/account/transactions'],
    ['INVITATION', '1', '/(parent)/family'],
    ['INVESTMENT', undefined, '/(parent)/(tabs)/investment'],
  ])('routes a PARENT %s notification to %s', (type, refId, expected) => {
    expect(resolveNotificationRoute({ type, refId }, 'PARENT')).toBe(expected);
  });

  it('routes a CHILD CHORE to the task tab with the chore detail modal opened', () => {
    expect(resolveNotificationRoute({ type: 'CHORE', refId: '3' }, 'CHILD')).toEqual({
      pathname: '/(child)/(tabs)/task',
      params: { openChoreId: '3' },
    });
  });

  it.each([
    ['CHORE', undefined, '/(child)/(tabs)/task'],
    ['CASHOUT', '7', '/(child)/wallet/cashouts'],
    ['CHARGE', undefined, '/(child)/(tabs)'],
    ['INVITATION', '1', '/(child)/(tabs)'],
    ['SAVINGS', '9', '/(child)/(tabs)/save'],
    ['INVESTMENT', undefined, '/(child)/(tabs)/invest'],
    ['DAILY', undefined, '/(child)/quiz'],
  ])('routes a CHILD %s notification to %s', (type, refId, expected) => {
    expect(resolveNotificationRoute({ type, refId }, 'CHILD')).toBe(expected);
  });

  it('returns null for a type the role never receives', () => {
    expect(resolveNotificationRoute({ type: 'DAILY' }, 'PARENT')).toBeNull();
    expect(resolveNotificationRoute({ type: 'SAVINGS', refId: '9' }, 'PARENT')).toBeNull();
  });

  it('returns null for an unrecognized type', () => {
    expect(resolveNotificationRoute({ type: 'UNKNOWN', refId: '5' }, 'PARENT')).toBeNull();
  });

  it('returns null when role is unknown', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, undefined)).toBeNull();
  });

  it('returns null when data itself is missing', () => {
    expect(resolveNotificationRoute(undefined, 'PARENT')).toBeNull();
  });
});
