import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { authService, type AuthUser } from "@/services/authService";
import { setTokens, clearTokens, getAccessToken, isAuthenticated as checkAuth } from "@/lib/api";

interface AuthContextType {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string, role: "STUDENT" | "RECRUITER") => Promise<void>;
  oauthLogin: (code: string, provider: "google" | "github") => Promise<void>;
  selectRole: (role: "STUDENT" | "RECRUITER") => Promise<void>;
  signOut: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    if (!getAccessToken()) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      const me = await authService.getMe();
      setUser(me);
    } catch {
      clearTokens();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = useCallback(async (email: string, password: string) => {
    const result = await authService.login({ email, password });
    setTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
  }, []);

  const register = useCallback(async (email: string, password: string, name: string, role: "STUDENT" | "RECRUITER") => {
    const result = await authService.register({ email, password, name, role });
    setTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
  }, []);

  const oauthLogin = useCallback(async (code: string, provider: "google" | "github") => {
    const result = provider === "google"
      ? await authService.oauthGoogle(code)
      : await authService.oauthGithub(code);
    setTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
  }, []);

  const selectRole = useCallback(async (role: "STUDENT" | "RECRUITER") => {
    const result = await authService.selectRole(role);
    setTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
  }, []);

  const signOut = useCallback(async () => {
    await authService.logout();
    clearTokens();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user && checkAuth(),
        login,
        register,
        oauthLogin,
        selectRole,
        signOut,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
