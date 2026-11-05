

import axios from "axios";
import { clearSession, getToken, logout } from "./auth";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = getToken();
  const headers = config.headers;
  const hasAuthorization = headers?.has
    ? headers.has("Authorization")
    : headers?.Authorization !== undefined
      || headers?.authorization !== undefined;

  if (token && !hasAuthorization) {
    if (headers?.set) {
      headers.set("Authorization", `Bearer ${token}`);
    } else {
      config.headers = {
        ...headers,
        Authorization: `Bearer ${token}`,
      };
    }
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url ?? "";
      const isAuthRequest =
        /\/auth\/(?:login|register)(?:[/?#]|$)/i.test(requestUrl);
      const currentPath = window.location.pathname;
      const isAuthPage = currentPath === "/login" || currentPath === "/register";

      if (isAuthRequest || isAuthPage) {
        clearSession();
      } else {
        logout();
      }
    }

    return Promise.reject(error);
  },
);

export default api;
