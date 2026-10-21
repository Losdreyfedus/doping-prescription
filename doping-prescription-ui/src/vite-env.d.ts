/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of doping-prescription-core; defaults to http://localhost:8080. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
