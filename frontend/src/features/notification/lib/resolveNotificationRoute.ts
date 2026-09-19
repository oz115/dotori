import type { Href } from 'expo-router';

type Role = 'PARENT' | 'CHILD' | undefined;

/**
 * 알림 탭으로 받은 FCM data 페이로드(백엔드 PushNotificationServiceImpl 이 실어 보낸
 * type/refId)를 실제 라우트로 바꾼다. FCM data 값은 항상 string 이라 unknown 에서 직접
 * 좁혀 쓴다. 처리할 수 없는 조합(모르는 타입, role 미확인, refId 가 꼭 필요한데 없음)은
 * null 을 반환하고, 호출자는 아무 것도 하지 않는다(현재 OS 기본 동작에 맡김).
 *
 * refId 로 상세까지 들어가는 건 상점 상품(PURCHASE)뿐이다. 나머지는 refId 가 가리키는
 * 대상(choreId·cashoutRequestId·savingsSubscriptionId·familyId)만으로 열 수 있는 상세 화면이
 * 없거나(부모 할 일 상세는 childId 도 필요), 애초에 refId 가 없는 알림(CHARGE·DAILY)이라
 * 그 알림을 처리할 수 있는 목록 화면으로 보낸다.
 */
export function resolveNotificationRoute(
  data: Record<string, unknown> | undefined,
  role: Role,
): Href | null {
  const type = data?.type;
  const refId = typeof data?.refId === 'string' ? data.refId : undefined;
  if (typeof type !== 'string' || !role) {
    return null;
  }

  return role === 'PARENT' ? resolveParentRoute(type, refId) : resolveChildRoute(type, refId);
}

function resolveParentRoute(type: string, refId: string | undefined): Href | null {
  switch (type) {
    // 아이가 상품을 샀다 — 그 상품 상세
    case 'PURCHASE':
      if (!refId) return null;
      return {
        pathname: '/(parent)/(tabs)/store/[productId]',
        // viaDeepLink: 이 화면이 store 탭 밖(알림)에서 곧장 열렸다는 표시. 상세 화면이
        // 이걸 보고 목록을 먼저 깔아준다 — 안 그러면 그 탭 내부 스택에 상세 하나만
        // 남아서 뒤로가기·탭 전환이 깨진다.
        params: { productId: refId, viaDeepLink: '1' },
      };
    // 아이가 완료를 인증했다 — 승인·거절하는 완료 요청 목록 (store 상세와 같은 이유로 viaDeepLink)
    case 'CHORE':
      return { pathname: '/(parent)/(tabs)/task/requests', params: { viaDeepLink: '1' } };
    // 아이가 환전을 요청했다 — 승인해야 송금되는 환전 요청 목록
    case 'CASHOUT':
      return '/(parent)/wallet/cashouts';
    // 아이가 실제 돈을 부모 계좌로 보냈다 — 그 입금이 찍힌 계좌 거래내역
    case 'CHARGE':
      return '/(parent)/account/transactions';
    // 아이가 가족에 들어왔다 — 연결된 아이 수가 보이는 가족 관리
    case 'INVITATION':
      return '/(parent)/family';
    case 'INVESTMENT':
      return '/(parent)/(tabs)/investment';
    default:
      return null;
  }
}

function resolveChildRoute(type: string, refId: string | undefined): Href | null {
  switch (type) {
    // 부모가 새 상품을 올렸다 — 그 상품 상세
    case 'PURCHASE':
      if (!refId) return null;
      return {
        pathname: '/(child)/(tabs)/store/[productId]',
        params: { productId: refId, viaDeepLink: '1' },
      };
    // 새 할 일 / 완료 승인 / 거절 — 아이 쪽엔 할 일 상세 화면이 없어 할 일 탭
    case 'CHORE':
      return '/(child)/(tabs)/task';
    // 환전 승인 / 거절 — 내 환전 요청 내역
    case 'CASHOUT':
      return '/(child)/wallet/cashouts';
    // 부모가 도토리를 줬다 — 잔액이 보이는 홈
    case 'CHARGE':
      return '/(child)/(tabs)';
    // 가족에 연결됐다 — 홈
    case 'INVITATION':
      return '/(child)/(tabs)';
    // 만기 / 납입 알림 — 저축 탭 (refId 는 가입 id 라 상품 상세로는 못 연다)
    case 'SAVINGS':
      return '/(child)/(tabs)/save';
    case 'INVESTMENT':
      return '/(child)/(tabs)/invest';
    // 아침 알림 — 오늘의 퀴즈
    case 'DAILY':
      return '/(child)/quiz';
    default:
      return null;
  }
}
