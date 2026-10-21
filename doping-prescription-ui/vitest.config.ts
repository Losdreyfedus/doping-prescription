import { defineConfig, mergeConfig } from 'vitest/config';

import viteConfig from './vite.config.ts';

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      restoreMocks: true,
      // Each worker boots its own jsdom; with one worker per core the boots compete for CPU and the first render
      // of a file can exceed Testing Library's 1 s wait. Three workers were both stable and fastest here.
      maxWorkers: 3,
    },
  }),
);
