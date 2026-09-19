const trimSlash = (url) => url.replace(/\/+$/, '');

export const AUTH_API_URL = trimSlash(
  import.meta.env.VITE_API_URL || 'http://localhost:8082'
);

export const REGISTER_API_URL = trimSlash(
  import.meta.env.VITE_REGISTER_URL || 'http://localhost:8081'
);