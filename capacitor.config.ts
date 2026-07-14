import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.hefimer.app',
  appName: 'Hefimer',
  webDir: 'dist',
  server: {
    url: 'https://hefimer.pages.dev',
    cleartext: true
  }
};

export default config;
