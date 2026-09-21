import { investmentAssetRow, savingsAssetRow } from './assetRows';

describe('savingsAssetRow', () => {
  it('가입 건수와 원금 + 이자 합계를 그대로 보여 준다', () => {
    expect(
      savingsAssetRow({
        isPending: false,
        isError: false,
        data: { count: 2, totalAmount: 5300 },
      }),
    ).toEqual({ subtitle: '2개 상품에 저축 중', amount: '5,300P' });
  });

  it('가입이 없으면 0P 와 빈 상태 문구다 — 가짜 3개·3000P 를 보여 주지 않는다', () => {
    const row = savingsAssetRow({
      isPending: false,
      isError: false,
      data: { count: 0, totalAmount: 0 },
    });
    expect(row).toEqual({ subtitle: '가입한 상품 없음', amount: '0P' });
  });

  it('불러오는 동안은 금액을 비워 둔다', () => {
    expect(savingsAssetRow({ isPending: true, isError: false, data: undefined })).toEqual({
      subtitle: '불러오는 중이에요',
      amount: '-',
    });
  });

  it('오류면 0P 로 속이지 않고 오류 문구를 보여 준다', () => {
    expect(savingsAssetRow({ isPending: false, isError: true, data: undefined })).toEqual({
      subtitle: '불러오지 못했어요',
      amount: '-',
    });
  });
});

describe('investmentAssetRow', () => {
  it('보유 종목 수와 현재 시세 기준 보유 가치를 보여 준다', () => {
    expect(
      investmentAssetRow({
        isPending: false,
        isError: false,
        data: { count: 3, totalValue: 4200, totalEarned: 300 },
      }),
    ).toEqual({ subtitle: '3개 회사 보유 중', amount: '4,200P' });
  });

  it('보유가 없으면 0P 와 빈 상태 문구다', () => {
    expect(
      investmentAssetRow({
        isPending: false,
        isError: false,
        data: { count: 0, totalValue: 0, totalEarned: 0 },
      }),
    ).toEqual({ subtitle: '보유 종목 없음', amount: '0P' });
  });

  it('불러오는 동안은 금액을 비워 두고, 오류면 오류 문구를 보여 준다', () => {
    expect(investmentAssetRow({ isPending: true, isError: false, data: undefined })).toEqual({
      subtitle: '불러오는 중이에요',
      amount: '-',
    });
    expect(investmentAssetRow({ isPending: false, isError: true, data: undefined })).toEqual({
      subtitle: '불러오지 못했어요',
      amount: '-',
    });
  });
});
