import { useAuth } from "@/hooks/use-auth";
import { DashboardSidebar } from "@/components/DashboardSidebar";
import { Notifications } from "@/components/Notifications";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { LogOut, Loader2, User } from "lucide-react";
import { ThemeToggle } from "@/components/ThemeToggle";
import { useNavigate } from "react-router";
import { StudentDashboard } from "@/components/dashboards/StudentDashboard";
import { RecruiterDashboard } from "@/components/dashboards/RecruiterDashboard";
import { AdminDashboard } from "@/components/dashboards/AdminDashboard";

function mapRole(role: string | null | undefined): "student" | "recruiter" | "admin" {
  if (!role) return "student";
  const r = role.toUpperCase();
  if (r === "RECRUITER") return "recruiter";
  if (r === "ADMIN") return "admin";
  return "student";
}

export default function Dashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!user.role) {
    navigate("/auth");
    return null;
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

  const role = mapRole(user.role);

  return (
    <div className="flex min-h-screen bg-background">
      <DashboardSidebar role={role} />

      <div className="flex flex-1 flex-col">
        {/* Top header */}
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
                    {role}
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

        {/* Main content */}
        <main className="flex-1 overflow-auto p-4 sm:p-6 pb-20 md:pb-6">
          {role === "student" && <StudentDashboard />}
          {role === "recruiter" && <RecruiterDashboard />}
          {role === "admin" && <AdminDashboard />}
        </main>
      </div>
    </div>
  );
}
