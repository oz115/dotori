import { httpClient } from '@/shared/api';
import { investmentApi } from './investmentApi';

jest.mock('@/shared/api', () => ({ httpClient: { get: jest.fn() } }));

it.each([400, 404, 405])(
  '뉴스 해설 API 미지원(%s)은 재시도 오류가 아닌 미지원으로 구분한다',
  async (status) => {
    jest.mocked(httpClient.get).mockRejectedValue({ response: { status } });
    await expect(investmentApi.getNewsReview()).resolves.toBeNull();
  },
);

it('연결 실패는 미지원으로 숨기지 않고 재시도할 수 있게 전달한다', async () => {
  const error = new Error('offline');
  jest.mocked(httpClient.get).mockRejectedValue(error);
  await expect(investmentApi.getNewsReview()).rejects.toBe(error);
});
