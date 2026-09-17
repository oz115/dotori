export type DeviceTokenPlatform = 'ANDROID' | 'IOS';

/** POST/GET .../device-tokens 공통 응답 조각 */
export type DeviceTokenRegisterBody = {
  fcmToken: string;
  platform: DeviceTokenPlatform;
};

/** GET .../device-tokens 응답 (NOTI-10) */
export type DeviceTokenStatus = {
  registered: boolean;
};
