import { useState, useEffect } from "react";
import { userService } from "@/services/userService";
import { companyService, type Company } from "@/services/jobService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Users, Building2, Briefcase, ClipboardList, ArrowRight } from "lucide-react";
import { useNavigate } from "react-router";

export function AdminDashboard() {
  const [stats, setStats] = useState<{
    totalStudents: number;
    totalRecruiters: number;
    totalCompanies: number;
    totalJobs: number;
    totalApplications: number;
    pendingCompanies: number;
  } | null>(null);
  const [loaded, setLoaded] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    userService.getAdminStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  if (!loaded || !stats) {
    return <div className="flex items-center justify-center py-12"><div className="animate-pulse text-muted-foreground">Loading...</div></div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Admin Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">Platform overview and management.</p>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums">{stats.totalStudents}</span>
          <span className="text-sm text-muted-foreground">students</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums">{stats.totalRecruiters}</span>
          <span className="text-sm text-muted-foreground">recruiters</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-green-600">{stats.totalJobs}</span>
          <span className="text-sm text-muted-foreground">jobs</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-blue-600">{stats.totalApplications}</span>
          <span className="text-sm text-muted-foreground">applications</span>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="cursor-pointer hover:border-primary/20 transition-colors" onClick={() => navigate("/dashboard/students")}>
          <CardContent className="flex items-center justify-between p-5">
            <div className="flex items-center gap-3">
              <Users className="size-5 text-muted-foreground" />
              <div>
                <p className="font-medium">Students</p>
                <p className="text-sm text-muted-foreground">{stats.totalStudents} registered</p>
              </div>
            </div>
            <ArrowRight className="size-4 text-muted-foreground" />
          </CardContent>
        </Card>

        <Card className="cursor-pointer hover:border-primary/20 transition-colors" onClick={() => navigate("/dashboard/companies")}>
          <CardContent className="flex items-center justify-between p-5">
            <div className="flex items-center gap-3">
              <Building2 className="size-5 text-muted-foreground" />
              <div>
                <p className="font-medium">Companies</p>
                <p className="text-sm text-muted-foreground">{stats.totalCompanies} registered{stats.pendingCompanies > 0 && ` · ${stats.pendingCompanies} pending`}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {stats.pendingCompanies > 0 && <Badge className="bg-yellow-100 text-yellow-700">{stats.pendingCompanies}</Badge>}
              <ArrowRight className="size-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>

        <Card className="cursor-pointer hover:border-primary/20 transition-colors" onClick={() => navigate("/dashboard/all-jobs")}>
          <CardContent className="flex items-center justify-between p-5">
            <div className="flex items-center gap-3">
              <Briefcase className="size-5 text-muted-foreground" />
              <div>
                <p className="font-medium">Jobs</p>
                <p className="text-sm text-muted-foreground">{stats.totalJobs} posted</p>
              </div>
            </div>
            <ArrowRight className="size-4 text-muted-foreground" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
