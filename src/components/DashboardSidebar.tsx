import { NavLink } from "react-router";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Briefcase,
  LayoutDashboard,
  User,
  FileText,
  Building2,
  Users,
  BarChart3,
  Settings,
  Search,
  Bookmark,
  ClipboardList,
  PanelLeftClose,
  PanelLeft,
  Calendar,
  Brain,
} from "lucide-react";
import type { UserRole } from "@/lib/constants";
import { useState } from "react";

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
}

const studentNav: NavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Browse Jobs", href: "/dashboard/jobs", icon: Search },
  { label: "Applications", href: "/dashboard/applications", icon: ClipboardList },
  { label: "Interviews", href: "/dashboard/interviews", icon: Calendar },
  { label: "Mock Interviews", href: "/dashboard/ai-interviews", icon: Brain },
  { label: "Profile", href: "/dashboard/profile", icon: User },
  { label: "Resumes", href: "/dashboard/resumes", icon: FileText },
];

const recruiterNav: NavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "My Company", href: "/dashboard/company", icon: Building2 },
  { label: "Job Postings", href: "/dashboard/jobs-manage", icon: Briefcase },
  { label: "Applicants", href: "/dashboard/applicants", icon: Users },
  { label: "Interviews", href: "/dashboard/recruiter-interviews", icon: Calendar },
  { label: "Profile", href: "/dashboard/profile", icon: User },
];

const adminNav: NavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Students", href: "/dashboard/students", icon: Users },
  { label: "Recruiters", href: "/dashboard/recruiters", icon: Building2 },
  { label: "Companies", href: "/dashboard/companies", icon: Building2 },
  { label: "Jobs", href: "/dashboard/all-jobs", icon: Briefcase },
  { label: "Applications", href: "/dashboard/all-applications", icon: ClipboardList },
];

const navByRole: Record<UserRole, NavItem[]> = {
  student: studentNav,
  recruiter: recruiterNav,
  admin: adminNav,
};

interface DashboardSidebarProps {
  role: UserRole;
}

export function DashboardSidebar({ role }: DashboardSidebarProps) {
  const [collapsed, setCollapsed] = useState(false);
  const items = navByRole[role];

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        className={cn(
          "hidden md:flex flex-col border-r border-border bg-card transition-all duration-200",
          collapsed ? "w-16" : "w-60"
        )}
      >
        <div className="flex h-14 items-center gap-2.5 border-b border-border px-4">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Briefcase className="size-4" />
          </div>
          {!collapsed && (
            <span className="text-sm font-bold tracking-tight">HireHub</span>
          )}
        </div>

        <nav className="flex-1 overflow-auto p-2">
          {items.map((item) => (
            <NavLink
              key={item.href}
              to={item.href}
              end={item.href === "/dashboard"}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  collapsed && "justify-center px-2"
                )
              }
            >
              <item.icon className="size-4 shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-border p-2">
          <Button
            variant="ghost"
            size="sm"
            className={cn(
              "w-full justify-start text-muted-foreground",
              collapsed && "justify-center px-2"
            )}
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? (
              <PanelLeft className="size-4" />
            ) : (
              <>
                <PanelLeftClose className="mr-2 size-4" />
                <span>Collapse</span>
              </>
            )}
          </Button>
        </div>
      </aside>

      {/* Mobile bottom nav */}
      <nav className="fixed bottom-0 left-0 right-0 z-50 flex border-t border-border bg-card md:hidden">
        {items.slice(0, 4).map((item) => (
          <NavLink
            key={item.href}
            to={item.href}
            end={item.href === "/dashboard"}
            className={({ isActive }) =>
              cn(
                "flex flex-1 flex-col items-center gap-1 py-2.5 text-[10px] font-medium transition-colors",
                isActive ? "text-primary" : "text-muted-foreground"
              )
            }
          >
            <item.icon className="size-4" />
            <span>{item.label.split(" ")[0]}</span>
          </NavLink>
        ))}
        {items.length > 4 && (
          <NavLink
            to={items[items.length - 1].href}
            className={({ isActive }) =>
              cn(
                "flex flex-1 flex-col items-center gap-1 py-2.5 text-[10px] font-medium transition-colors",
                isActive ? "text-primary" : "text-muted-foreground"
              )
            }
          >
            <Settings className="size-4" />
            <span>More</span>
          </NavLink>
        )}
      </nav>
    </>
  );
}
