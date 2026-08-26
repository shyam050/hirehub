import api, { unwrap } from "@/lib/api";

export interface AuthUser {
  id: string;
  email: string;
  name: string | null;
  image: string | null;
  role: "STUDENT" | "RECRUITER" | "ADMIN" | null;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken?: string; // Now in HttpOnly cookie — may not be in response body
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: "STUDENT" | "RECRUITER";
}

export interface LoginRequest {
  email: string;
  password: string;
}

export const authService = {
  async register(data: RegisterRequest): Promise<AuthTokens> {
    const res = await api.post("/auth/register", data);
    return unwrap<AuthTokens>(res);
  },

  async login(data: LoginRequest): Promise<AuthTokens> {
    const res = await api.post("/auth/login", data);
    return unwrap<AuthTokens>(res);
  },

  async getMe(): Promise<AuthUser> {
    const res = await api.get("/auth/me");
    return unwrap<AuthUser>(res);
  },

  async logout(): Promise<void> {
    try {
      await api.post("/auth/logout");
    } catch {
      // Ignore errors on logout
    }
  },

  // OAuth
  async oauthGoogle(code: string): Promise<AuthTokens> {
    const res = await api.post("/auth/oauth/google", { code });
    return unwrap<AuthTokens>(res);
  },

  async oauthGithub(code: string): Promise<AuthTokens> {
    const res = await api.post("/auth/oauth/github", { code });
    return unwrap<AuthTokens>(res);
  },

  async selectRole(role: "STUDENT" | "RECRUITER"): Promise<AuthTokens> {
    const res = await api.post("/auth/oauth/role", { role });
    return unwrap<AuthTokens>(res);
  },
};
