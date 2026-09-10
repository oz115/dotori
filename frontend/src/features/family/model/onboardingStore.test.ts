import { useOnboardingStore } from './onboardingStore';

beforeEach(() => useOnboardingStore.getState().reset());

describe('useOnboardingStore', () => {
  it('starts empty', () => {
    expect(useOnboardingStore.getState()).toMatchObject({
      familyName: '',
      weeklyAllowance: null,
      weeklyRewardLimit: null,
      financialKnowledgeLevel: null,
    });
  });

  it('patches values across steps', () => {
    useOnboardingStore.getState().patch({ familyName: '도토리 가족' });
    useOnboardingStore
      .getState()
      .patch({ weeklyAllowance: 5000, financialKnowledgeLevel: 'BEGINNER' });

    expect(useOnboardingStore.getState()).toMatchObject({
      familyName: '도토리 가족',
      weeklyAllowance: 5000,
      financialKnowledgeLevel: 'BEGINNER',
    });
  });

  it('reset clears everything', () => {
    useOnboardingStore.getState().patch({ familyName: 'x', weeklyRewardLimit: 3000 });
    useOnboardingStore.getState().reset();

    expect(useOnboardingStore.getState()).toMatchObject({
      familyName: '',
      weeklyRewardLimit: null,
    });
  });
});
