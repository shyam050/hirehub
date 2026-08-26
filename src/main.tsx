import '@vly-ai/integrations';
import { Toaster } from "@/components/ui/sonner";
import { RequireAuth } from "@/components/RequireAuth";
import { AuthProvider } from "@/hooks/use-auth";
import { ThemeProvider } from "next-themes";
import React, { StrictMode, useEffect, lazy, Suspense } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes, useLocation, useNavigate } from "react-router";
import "./index.css";

// Lazy load route components
const Landing = lazy(() => import("./pages/Landing.tsx"));
const AuthPage = lazy(() => import("./pages/Auth.tsx"));
const Dashboard = lazy(() => import("./pages/Dashboard.tsx"));
const ProfilePage = lazy(() => import("./pages/dashboard/ProfilePage.tsx"));
const ResumesPage = lazy(() => import("./pages/dashboard/ResumesPage.tsx"));
const JobsBrowsePage = lazy(
  () => import("./pages/dashboard/JobsBrowsePage.tsx")
);
const ApplicationsPage = lazy(
  () => import("./pages/dashboard/ApplicationsPage.tsx")
);
const CompanyPage = lazy(() => import("./pages/dashboard/CompanyPage.tsx"));
const JobsManagePage = lazy(() => import("./pages/dashboard/JobsManagePage.tsx"));
const JobCreatePage = lazy(() => import("./pages/dashboard/JobCreatePage.tsx"));
const JobDetailPage = lazy(() => import("./pages/dashboard/JobDetailPage.tsx"));
const ApplicantsPage = lazy(() => import("./pages/dashboard/ApplicantsPage.tsx"));
const AdminStudentsPage = lazy(() => import("./pages/dashboard/AdminStudentsPage.tsx"));
const AdminRecruitersPage = lazy(() => import("./pages/dashboard/AdminRecruitersPage.tsx"));
const AdminCompaniesPage = lazy(() => import("./pages/dashboard/AdminCompaniesPage.tsx"));
const AdminAllJobsPage = lazy(() => import("./pages/dashboard/AdminAllJobsPage.tsx"));
const AdminAllApplicationsPage = lazy(() => import("./pages/dashboard/AdminAllApplicationsPage.tsx"));
const StudentInterviewsPage = lazy(() => import("./pages/dashboard/StudentInterviewsPage.tsx"));
const RecruiterInterviewsPage = lazy(() => import("./pages/dashboard/RecruiterInterviewsPage.tsx"));
const ResumeAnalysisPage = lazy(() => import("./pages/dashboard/ResumeAnalysisPage.tsx"));
const AIInterviewsPage = lazy(() => import("./pages/dashboard/AIInterviewsPage.tsx"));
const AIInterviewNewPage = lazy(() => import("./pages/dashboard/AIInterviewNewPage.tsx"));
const AIInterviewSessionPage = lazy(() => import("./pages/dashboard/AIInterviewSessionPage.tsx"));
const AIInterviewReportPage = lazy(() => import("./pages/dashboard/AIInterviewReportPage.tsx"));
const NotFound = lazy(() => import("./pages/NotFound.tsx"));

function RouteLoading() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-pulse text-muted-foreground">Loading...</div>
    </div>
  );
}



class RootErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; message: string; stack: string }
> {
  state = { hasError: false, message: "", stack: "" };
  static getDerivedStateFromError(error: Error) {
    return {
      hasError: true,
      message: error.message || "Unknown runtime error",
      stack: error.stack || "",
    };
  }
  componentDidCatch(err: Error) {
    console.error("[WebContainer preview] Root crash:", err);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-background text-foreground p-6">
          <div className="max-w-lg text-center">
            <p className="text-sm font-semibold">Preview runtime error</p>
            <p className="mt-2 text-xs text-muted-foreground break-words">
              {this.state.message}
            </p>
            {this.state.stack && (
              <pre className="mt-3 text-left text-[10px] leading-4 text-muted-foreground/80 max-h-40 overflow-auto rounded border border-border/60 p-2">
                {this.state.stack}
              </pre>
            )}
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

function RouteSyncer() {
  const location = useLocation();
  useEffect(() => {
    window.parent.postMessage(
      { type: "iframe-route-change", path: location.pathname },
      "*"
    );
  }, [location.pathname]);

  useEffect(() => {
    function handleMessage(event: MessageEvent) {
      if (event.data?.type === "navigate") {
        if (event.data.direction === "back") window.history.back();
        if (event.data.direction === "forward") window.history.forward();
      }
    }
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, []);

  return null;
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RootErrorBoundary>
      <AuthProvider>
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
        <BrowserRouter>
          <RouteSyncer />
          <Suspense fallback={<RouteLoading />}>
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route
                path="/auth"
                element={<AuthPage redirectAfterAuth="/dashboard" />}
              />

              {/* Dashboard wrapper route */}
              <Route
                path="/dashboard"
                element={
                  <RequireAuth>
                    <Dashboard />
                  </RequireAuth>
                }
              />

              {/* All dashboard sub-routes rendered as modals or inline */}
              <Route
                path="/dashboard/profile"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="profile" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/resumes"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="resumes" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/jobs"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="jobs" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/applications"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="applications" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/company"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="company" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/jobs-manage"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="jobs-manage" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/jobs-manage/new"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="job-create" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/jobs-manage/:id"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="job-detail" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/applicants"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="applicants" />
                  </RequireAuth>
                }
              />

              {/* Resume analysis route */}
              <Route
                path="/dashboard/resumes/:id/analysis"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="resume-analysis" />
                  </RequireAuth>
                }
              />

              {/* AI Mock Interview routes */}
              <Route
                path="/dashboard/ai-interviews"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="ai-interviews" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/ai-interviews/new"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="ai-interview-new" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/ai-interviews/:id/report"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="ai-interview-report" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/ai-interviews/:id"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="ai-interview-session" />
                  </RequireAuth>
                }
              />

              {/* Interview routes */}
              <Route
                path="/dashboard/interviews"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="student-interviews" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/recruiter-interviews"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="recruiter-interviews" />
                  </RequireAuth>
                }
              />

              {/* Admin routes */}
              <Route
                path="/dashboard/students"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="admin-students" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/recruiters"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="admin-recruiters" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/companies"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="admin-companies" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/all-jobs"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="admin-all-jobs" />
                  </RequireAuth>
                }
              />
              <Route
                path="/dashboard/all-applications"
                element={
                  <RequireAuth>
                    <DashboardWithPage page="admin-all-applications" />
                  </RequireAuth>
                }
              />

              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
        <Toaster />
        </ThemeProvider>
      </AuthProvider>
    </RootErrorBoundary>
  </StrictMode>
);

// Dashboard wrapper that shows sidebar + a specific page
import { DashboardSidebar } from "@/components/DashboardSidebar";
import { Notifications } from "@/components/Notifications";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { LogOut, User } from "lucide-react";
import { ThemeToggle } from "@/components/ThemeToggle";

type PageName =
  | "profile"
  | "resumes"
  | "jobs"
  | "applications"
  | "company"
  | "jobs-manage"
  | "job-create"
  | "job-detail"
  | "applicants"
  | "admin-students"
  | "admin-recruiters"
  | "admin-companies"
  | "admin-all-jobs"
  | "admin-all-applications"
  | "student-interviews"
  | "recruiter-interviews"
  | "resume-analysis"
  | "ai-interviews"
  | "ai-interview-new"
  | "ai-interview-session"
  | "ai-interview-report";

function DashboardWithPage({ page }: { page: PageName }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  if (!user || !user.role) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="animate-pulse text-muted-foreground">Loading...</div>
      </div>
    );
  }

  const handleSignOut = async () => {
    await signOut();
    navigate("/");
  };

  const initials = user.name
    ? user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "??";

  const PageComponent = PAGE_COMPONENTS[page];

  return (
    <div className="flex min-h-screen bg-background">
      <DashboardSidebar role={user.role === "STUDENT" ? "student" : user.role === "RECRUITER" ? "recruiter" : "admin"} />
      <div className="flex flex-1 flex-col">
        <header className="sticky top-0 z-40 flex h-14 items-center justify-between border-b border-border bg-card/80 px-4 backdrop-blur-sm">
          <div className="md:hidden flex items-center gap-2">
            <span className="text-sm font-bold">HireHub</span>
          </div>
          <div className="hidden md:block" />
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <Notifications />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="relative size-9 rounded-full"
                  size="icon"
                >
                  <Avatar className="size-8">
                    <AvatarFallback className="text-xs">
                      {initials}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <div className="px-2 py-1.5">
                  <p className="text-sm font-medium">{user.name || "User"}</p>
                  <p className="text-xs text-muted-foreground capitalize">
                    {user.role?.toLowerCase()}
                  </p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={() => navigate("/dashboard/profile")}
                >
                  <User className="mr-2 size-4" />
                  Profile
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleSignOut}>
                  <LogOut className="mr-2 size-4" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <main className="flex-1 overflow-auto p-4 sm:p-6 pb-20 md:pb-6">
          <PageComponent />
        </main>
      </div>
    </div>
  );
}

const PAGE_COMPONENTS: Record<PageName, React.ComponentType> = {
  profile: ProfilePage,
  resumes: ResumesPage,
  "resume-analysis": ResumeAnalysisPage,
  jobs: JobsBrowsePage,
  applications: ApplicationsPage,
  company: CompanyPage,
  "jobs-manage": JobsManagePage,
  "job-create": JobCreatePage,
  "job-detail": JobDetailPage,
  applicants: ApplicantsPage,
  "admin-students": AdminStudentsPage,
  "admin-recruiters": AdminRecruitersPage,
  "admin-companies": AdminCompaniesPage,
  "admin-all-jobs": AdminAllJobsPage,
  "admin-all-applications": AdminAllApplicationsPage,
  "student-interviews": StudentInterviewsPage,
  "recruiter-interviews": RecruiterInterviewsPage,
  "ai-interviews": AIInterviewsPage,
  "ai-interview-new": AIInterviewNewPage,
  "ai-interview-session": AIInterviewSessionPage,
  "ai-interview-report": AIInterviewReportPage,
};
