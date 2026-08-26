import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "/api/v1";

// Access token stored in localStorage (short-lived, 15 min)
// Refresh token is in HttpOnly cookie — never accessible to JavaScript
const ACCESS_TOKEN_KEY = "hirehub_access_token";

// Simple pub/sub for auth state changes
type AuthListener = () => void;
let authListeners: AuthListener[] = [];

export function onAuthChange(listener: AuthListener) {
  authListeners.push(listener);
  return () => {
    authListeners = authListeners.filter((l) => l !== listener);
  };
}

function notifyAuthChange() {
  authListeners.forEach((l) => l());
}

// Token helpers
export function getAccessToken(): string | null {
  try {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setAccessToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  notifyAuthChange();
}

export function clearAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  notifyAuthChange();
}

/**
 * After login/register, the refresh token is set as an HttpOnly cookie by the backend.
 * We only store the access token in localStorage (short-lived).
 */
export function setTokens(access: string, _refresh?: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, access);
  notifyAuthChange();
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  notifyAuthChange();
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

// Create axios instance
const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // Send cookies (refresh token) with requests
});

// Request interceptor — attach JWT
api.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401 with token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error || !token) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const requestId = error.response?.data?.requestId;

    // Attach requestId to error for debugging
    if (requestId) {
      error.requestId = requestId;
    }

    // Handle transient failures with retry (GET only)
    if (
      [502, 503, 504].includes(status) &&
      (!originalRequest.method || originalRequest.method.toUpperCase() === "GET") &&
      !originalRequest._transientRetry
    ) {
      originalRequest._transientRetry = true;
      await new Promise((r) => setTimeout(r, 1000));
      return api(originalRequest);
    }

    // Skip refresh for auth endpoints and already-retried requests
    if (
      status !== 401 ||
      originalRequest._retry ||
      originalRequest.url?.includes("/auth/login") ||
      originalRequest.url?.includes("/auth/register") ||
      originalRequest.url?.includes("/auth/refresh")
    ) {
      return Promise.reject(error);
    }

    // If already refreshing, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Refresh token is sent via HttpOnly cookie automatically
      const { data } = await axios.post(
        `${BASE_URL}/auth/refresh`,
        {},
        { withCredentials: true }
      );
      const responseData = data.data || data;
      if (responseData.accessToken) {
        setTokens(responseData.accessToken);
        processQueue(null, responseData.accessToken);
        originalRequest.headers.Authorization = `Bearer ${responseData.accessToken}`;
        return api(originalRequest);
      }
      throw new Error("No access token in refresh response");
    } catch (refreshError) {
      processQueue(refreshError, null);
      clearAccessToken();
      window.location.href = "/auth";
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;

// Helper to unwrap ApiResponse<T>
export function unwrap<T>(response: { data: { data?: T; success?: boolean; message?: string } }): T {
  const body = response.data;
  if (body && typeof body === "object" && "data" in body) {
    return body.data as T;
  }
  return body as unknown as T;
}

// Friendly error messages for common status codes
export function getFriendlyErrorMessage(error: unknown): string {
  if (!error || typeof error !== "object") return "An unexpected error occurred.";
  const axiosErr = error as { response?: { status?: number; data?: { code?: string; message?: string } }; requestId?: string };
  const status = axiosErr.response?.status;
  const code = axiosErr.response?.data?.code;
  const message = axiosErr.response?.data?.message;

  if (status === 429) return "Too many requests. Please try again shortly.";
  if (status === 503 && code === "AI_SERVICE_UNAVAILABLE") return "AI services are temporarily unavailable. Please try again later.";
  if (status === 503) return "Service temporarily unavailable. Please try again later.";
  if (status === 502) return "Server error. Please try again later.";
  if (message) return message;
  return "An unexpected error occurred. Please try again later.";
}

// Helper for multipart uploads (no JSON content-type)
const uploadApi = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

uploadApi.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

uploadApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      clearAccessToken();
      window.location.href = "/auth";
    }
    return Promise.reject(error);
  }
);

export { uploadApi };
