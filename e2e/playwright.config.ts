import { defineConfig, devices } from '@playwright/test';

const CI = Boolean(process.env.CI);
const API_PORT = process.env.E2E_API_PORT ?? '8080';
const WEB_PORT = process.env.E2E_WEB_PORT ?? '5173';
const WEB_URL = `http://localhost:${WEB_PORT}`;
const JAR = '../doping-prescription-core/target/doping-prescription-core-0.0.1-SNAPSHOT.jar';

/**
 * The demo scenario (docs/demo-senaryosu.md) against the real stack. Locally it reuses the backend and web app you
 * already run; in CI it starts both (Docker Compose provides PostgreSQL, Redis and Kafka). E2E_API_PORT and
 * E2E_WEB_PORT run it next to another copy of the app.
 */
export default defineConfig({
  testDir: 'tests',
  // The scenario includes one simulated video (about 15 s at the demo speed).
  timeout: 120_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: WEB_URL,
    viewport: { width: 1600, height: 900 },
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1600, height: 900 } } }],
  webServer: [
    {
      command: `java -jar ${JAR} --spring.profiles.active=demo --server.port=${API_PORT} --prescription.auth.cors-allowed-origin=${WEB_URL}`,
      url: `http://localhost:${API_PORT}/actuator/health`,
      reuseExistingServer: !CI,
      timeout: 180_000,
    },
    {
      command: `npm --prefix ../doping-prescription-ui run dev -- --port ${WEB_PORT}`,
      env: { VITE_API_BASE_URL: `http://localhost:${API_PORT}` },
      url: WEB_URL,
      reuseExistingServer: !CI,
      timeout: 60_000,
    },
  ],
});
