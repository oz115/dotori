import { resolveNotificationRoute } from './resolveNotificationRoute';

describe('resolveNotificationRoute', () => {
  it('routes a PARENT to the parent product detail screen', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'PARENT')).toEqual({
      pathname: '/(parent)/(tabs)/store/[productId]',
      params: { productId: '5' },
    });
  });

  it('routes a CHILD to the child product detail screen', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, 'CHILD')).toEqual({
      pathname: '/(child)/(tabs)/store/[productId]',
      params: { productId: '5' },
    });
  });

  it('returns null for an unrecognized type', () => {
    expect(resolveNotificationRoute({ type: 'CHORE', refId: '5' }, 'PARENT')).toBeNull();
  });

  it('returns null when refId is missing', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE' }, 'PARENT')).toBeNull();
  });

  it('returns null when role is unknown', () => {
    expect(resolveNotificationRoute({ type: 'PURCHASE', refId: '5' }, undefined)).toBeNull();
  });

  it('returns null when data itself is missing', () => {
    expect(resolveNotificationRoute(undefined, 'PARENT')).toBeNull();
  });
});
