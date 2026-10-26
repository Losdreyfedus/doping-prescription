import { defineConfig, mergeConfig } from 'vitest/config';

import viteConfig from './vite.config.ts';

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      restoreMocks: true,
      // Each worker boots its own jsdom; with one worker per core the boots compete for CPU and slow every file's first
      // render (see also asyncUtilTimeout in src/test/setup.ts). Three workers were the fastest here.
      maxWorkers: 3,
    },
  }),
);
