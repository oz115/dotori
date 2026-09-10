import { savedFamilyId } from './savedFamilyId';

beforeEach(() => savedFamilyId.clear());

describe('savedFamilyId', () => {
  it('is null before anything is saved', async () => {
    expect(await savedFamilyId.get()).toBeNull();
  });

  it('round-trips an id', async () => {
    await savedFamilyId.set(42);
    expect(await savedFamilyId.get()).toBe(42);
  });

  it('clear removes it', async () => {
    await savedFamilyId.set(7);
    await savedFamilyId.clear();
    expect(await savedFamilyId.get()).toBeNull();
  });
});
