import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/hooks/use-auth";
import {
  Loader2,
  GraduationCap,
  Building2,
} from "lucide-react";
import { SiGoogle, SiGithub } from "@icons-pack/react-simple-icons";
import { Suspense, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";

interface AuthProps {
  redirectAfterAuth?: string;
}

function resolveRedirectAfterAuth(returnTo: string | null, fallback = "/dashboard") {
  if (returnTo?.startsWith("/") && !returnTo.startsWith("//")) {
    return returnTo;
  }
  return fallback;
}

// Google Client ID — configured via env or inline
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";
const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID || "";

function Auth({ redirectAfterAuth }: AuthProps = {}) {
  const { isLoading: authLoading, isAuthenticated, user, login, register, oauthLogin, selectRole } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirect = resolveRedirectAfterAuth(
    searchParams.get("returnTo"),
    redirectAfterAuth
  );

  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<"STUDENT" | "RECRUITER">("STUDENT");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingRole, setPendingRole] = useState(false);

  // Handle OAuth redirect callback (GitHub uses ?code= in URL)
  useEffect(() => {
    const code = searchParams.get("code");
    const provider = searchParams.get("provider");
    if (code && provider === "github") {
      setIsLoading(true);
      oauthLogin(code, "github")
        .then(() => {
          window.history.replaceState({}, "", window.location.pathname);
          navigate(redirect);
        })
        .catch((err: any) => {
          setError(err?.response?.data?.message || "GitHub sign-in failed");
          setIsLoading(false);
        });
    }
  }, [searchParams, oauthLogin, navigate, redirect]);

  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      if (!user?.role) {
        setPendingRole(true);
      } else {
        navigate(redirect);
      }
    }
  }, [authLoading, isAuthenticated, user, navigate, redirect]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      await login(email, password);
      navigate(redirect);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || "Invalid email or password");
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      await register(email, password, name, role);
      navigate(redirect);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || "Registration failed");
    } finally {
      setIsLoading(false);
    }
  };

  // ── Google OAuth ──
  const handleGoogleOAuth = () => {
    if (!GOOGLE_CLIENT_ID) {
      setError("Google Sign-In is not configured. Set VITE_GOOGLE_CLIENT_ID.");
      return;
    }
    const redirectUri = window.location.origin + "/auth";
    const scope = "openid email profile";
    const url = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${GOOGLE_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=${encodeURIComponent(scope)}&prompt=select_account`;
    window.location.href = url;
  };

  // ── GitHub OAuth ──
  const handleGithubOAuth = () => {
    if (!GITHUB_CLIENT_ID) {
      setError("GitHub Sign-In is not configured. Set VITE_GITHUB_CLIENT_ID.");
      return;
    }
    const redirectUri = window.location.origin + "/auth";
    const url = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=read:user user:email`;
    window.location.href = url;
  };

  // ── Role Selection (for new OAuth users) ──
  const handleRoleSelect = async (selectedRole: "STUDENT" | "RECRUITER") => {
    setIsLoading(true);
    setError(null);
    try {
      await selectRole(selectedRole);
      if (selectedRole === "STUDENT") {
        navigate("/dashboard/profile?setup=true");
      } else {
        navigate("/dashboard/company");
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || "Failed to set role");
      setIsLoading(false);
    }
  };

  // ── Role Selection Screen ──
  if (pendingRole) {
    return (
      <div className="min-h-screen flex items-center justify-center px-5 py-12">
        <div className="w-full max-w-sm">
          <h1 className="text-xl font-bold tracking-tight">Choose your role</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            This determines your dashboard and permissions.
          </p>
          <div className="mt-6 space-y-2.5">
            {[
              { value: "STUDENT" as const, icon: GraduationCap, title: "Student", desc: "Browse and apply to positions" },
              { value: "RECRUITER" as const, icon: Building2, title: "Recruiter", desc: "Post positions and manage applicants" },
            ].map((item) => (
              <button
                key={item.value}
                className="w-full rounded-lg border border-border p-3.5 text-left transition-colors hover:bg-muted/50 hover:border-muted-foreground/20 disabled:opacity-50"
                onClick={() => handleRoleSelect(item.value)}
                disabled={isLoading}
              >
                <div className="flex items-center gap-3">
                  <item.icon className="size-4 text-muted-foreground shrink-0" />
                  <div>
                    <span className="text-sm font-medium block">{item.title}</span>
                    <span className="text-xs text-muted-foreground">{item.desc}</span>
                  </div>
                </div>
              </button>
            ))}
            {error && <p className="text-sm text-red-500 text-center">{error}</p>}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">
      {/* Left side — brand panel */}
      <div className="hidden lg:flex lg:w-2/5 flex-col justify-between border-r border-border bg-muted/30 p-10">
        <div className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded bg-foreground text-background">
            <span className="text-xs font-bold">H</span>
          </div>
          <span className="text-sm font-semibold">HireHub</span>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground mb-3">
            Campus Recruitment
          </p>
          <h2 className="text-xl font-bold leading-snug">
            Sign in to access
            <br />
            your dashboard.
          </h2>
          <p className="mt-2 text-sm text-muted-foreground leading-relaxed max-w-xs">
            Manage applications, review candidates, and track the placement
            pipeline — all in one place.
          </p>
        </div>
        <p className="text-xs text-muted-foreground">
          Secured by{" "}
          <a href="https://freebuff.com" target="_blank" rel="noopener noreferrer" className="underline hover:text-foreground transition-colors">
            freebuff.com
          </a>
        </p>
      </div>

      {/* Right side — form */}
      <div className="flex flex-1 flex-col items-center justify-center px-5 py-12">
        <div className="w-full max-w-sm">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="flex size-7 items-center justify-center rounded bg-foreground text-background">
              <span className="text-xs font-bold">H</span>
            </div>
            <span className="text-sm font-semibold">HireHub</span>
          </div>

          {mode === "login" ? (
            <>
              <h1 className="text-xl font-bold tracking-tight">Welcome back</h1>
              <p className="mt-1.5 text-sm text-muted-foreground">
                Sign in with your email or continue with a provider.
              </p>

              <form onSubmit={handleLogin} className="mt-6 space-y-4">
                <div className="space-y-1.5">
                  <label className="text-sm font-medium">Email</label>
                  <Input type="email" placeholder="you@university.edu" value={email} onChange={(e) => setEmail(e.target.value)} disabled={isLoading} required />
                </div>
                <div className="space-y-1.5">
                  <label className="text-sm font-medium">Password</label>
                  <Input type="password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} disabled={isLoading} required minLength={8} />
                </div>
                {error && <p className="text-sm text-red-500">{error}</p>}
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : "Sign In"}
                </Button>

                <div className="relative">
                  <div className="absolute inset-0 flex items-center"><span className="w-full border-t" /></div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-background px-2 text-muted-foreground">or continue with</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <Button type="button" variant="outline" className="w-full" onClick={handleGoogleOAuth} disabled={isLoading}>
                    <SiGoogle className="mr-2 h-4 w-4" /> Google
                  </Button>
                  <Button type="button" variant="outline" className="w-full" onClick={handleGithubOAuth} disabled={isLoading}>
                    <SiGithub className="mr-2 h-4 w-4" /> GitHub
                  </Button>
                </div>

                <p className="text-center text-sm text-muted-foreground">
                  Don't have an account?{" "}
                  <button type="button" className="text-primary hover:underline font-medium" onClick={() => { setMode("register"); setError(null); }}>
                    Sign up
                  </button>
                </p>
              </form>
            </>
          ) : (
            <>
              <h1 className="text-xl font-bold tracking-tight">Create an account</h1>
              <p className="mt-1.5 text-sm text-muted-foreground">
                Fill in your details to get started.
              </p>

              <form onSubmit={handleRegister} className="mt-6 space-y-4">
                <div className="space-y-1.5">
                  <label className="text-sm font-medium">Full Name</label>
                  <Input type="text" placeholder="Your full name" value={name} onChange={(e) => setName(e.target.value)} disabled={isLoading} required />
                </div>
                <div className="space-y-1.5">
                  <label className="text-sm font-medium">Email</label>
                  <Input type="email" placeholder="you@university.edu" value={email} onChange={(e) => setEmail(e.target.value)} disabled={isLoading} required />
                </div>
                <div className="space-y-1.5">
                  <label className="text-sm font-medium">Password</label>
                  <Input type="password" placeholder="Min 8 characters" value={password} onChange={(e) => setPassword(e.target.value)} disabled={isLoading} required minLength={8} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">I am a</label>
                  <div className="grid grid-cols-2 gap-3">
                    <button type="button" className={`flex items-center justify-center gap-2 rounded-lg border p-3 text-sm font-medium transition-colors ${role === "STUDENT" ? "border-primary bg-primary/5 text-primary" : "border-border text-muted-foreground hover:bg-muted/50"}`} onClick={() => setRole("STUDENT")} disabled={isLoading}>
                      <GraduationCap className="size-4" /> Student
                    </button>
                    <button type="button" className={`flex items-center justify-center gap-2 rounded-lg border p-3 text-sm font-medium transition-colors ${role === "RECRUITER" ? "border-primary bg-primary/5 text-primary" : "border-border text-muted-foreground hover:bg-muted/50"}`} onClick={() => setRole("RECRUITER")} disabled={isLoading}>
                      <Building2 className="size-4" /> Recruiter
                    </button>
                  </div>
                </div>
                {error && <p className="text-sm text-red-500">{error}</p>}
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : "Create Account"}
                </Button>

                <div className="relative">
                  <div className="absolute inset-0 flex items-center"><span className="w-full border-t" /></div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-background px-2 text-muted-foreground">or continue with</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <Button type="button" variant="outline" className="w-full" onClick={handleGoogleOAuth} disabled={isLoading}>
                    <SiGoogle className="mr-2 h-4 w-4" /> Google
                  </Button>
                  <Button type="button" variant="outline" className="w-full" onClick={handleGithubOAuth} disabled={isLoading}>
                    <SiGithub className="mr-2 h-4 w-4" /> GitHub
                  </Button>
                </div>

                <p className="text-center text-sm text-muted-foreground">
                  Already have an account?{" "}
                  <button type="button" className="text-primary hover:underline font-medium" onClick={() => { setMode("login"); setError(null); }}>
                    Sign in
                  </button>
                </p>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function AuthPage(props: AuthProps) {
  return (
    <Suspense>
      <Auth {...props} />
    </Suspense>
  );
}
