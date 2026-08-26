import { useState, useEffect } from "react";
import { userService, type RecruiterProfile } from "@/services/userService";
import { jobService, type Job } from "@/services/jobService";
import { interviewService, type Interview } from "@/services/interviewService";
import { companyService, type Company } from "@/services/jobService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Briefcase, Users, ArrowRight, Calendar, Building2, CheckCircle, Clock, Plus } from "lucide-react";
import { useNavigate } from "react-router";

export function RecruiterDashboard() {
  const [company, setCompany] = useState<Company | null>(null);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [interviews, setInterviews] = useState<Interview[]>([]);
  const [loaded, setLoaded] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([
      companyService.getMyCompany().catch(() => null),
      jobService.getMyCompanyJobs().catch(() => []),
      interviewService.getRecruiterInterviews().catch(() => []),
    ]).then(([c, j, i]) => {
      setCompany(c);
      setJobs(j);
      setInterviews(i);
      setLoaded(true);
    });
  }, []);

  const totalApplicants = jobs.reduce((sum, j) => sum + j.applicationCount, 0);
  const activeJobs = jobs.filter((j) => j.status === "ACTIVE").length;
  const upcomingInterviews = interviews.filter((i) => i.status === "SCHEDULED" || i.status === "RESCHEDULED");

  if (!loaded) {
    return <div className="flex items-center justify-center py-12"><div className="animate-pulse text-muted-foreground">Loading...</div></div>;
  }

  if (!company) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Welcome to HireHub</h1>
          <p className="text-sm text-muted-foreground mt-1">Create your company profile to start posting positions.</p>
        </div>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Building2 className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">Set up your company</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Create a company profile before posting your first position.</p>
            <Button onClick={() => navigate("/dashboard/company")}><Plus className="mr-2 size-4" /> Create Company</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-bold tracking-tight">{company.name}</h1>
        <p className="text-sm text-muted-foreground mt-1">Recruitment overview at a glance.</p>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums">{activeJobs}</span>
          <span className="text-sm text-muted-foreground">active listings</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-green-600">{totalApplicants}</span>
          <span className="text-sm text-muted-foreground">total applicants</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-blue-600">{upcomingInterviews.length}</span>
          <span className="text-sm text-muted-foreground">upcoming interviews</span>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent jobs */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium">Your Listings</CardTitle>
              <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate("/dashboard/jobs-manage")}>
                Manage <ArrowRight className="ml-1 size-3" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {jobs.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-sm text-muted-foreground">No listings yet.</p>
                <Button variant="link" size="sm" className="mt-1" onClick={() => navigate("/dashboard/jobs-manage/new")}>Post your first position</Button>
              </div>
            ) : (
              <div className="space-y-0 divide-y divide-border">
                {jobs.slice(0, 5).map((job) => (
                  <div key={job.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{job.title}</p>
                      <p className="text-xs text-muted-foreground flex items-center gap-1">
                        <Users className="size-3" /> {job.applicationCount} applicants
                      </p>
                    </div>
                    <Badge variant={job.status === "ACTIVE" ? "default" : "secondary"}>{job.status}</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Upcoming interviews */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium">Upcoming Interviews</CardTitle>
              <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate("/dashboard/recruiter-interviews")}>
                View all <ArrowRight className="ml-1 size-3" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {upcomingInterviews.length === 0 ? (
              <div className="py-8 text-center"><p className="text-sm text-muted-foreground">No upcoming interviews.</p></div>
            ) : (
              <div className="space-y-0 divide-y divide-border">
                {upcomingInterviews.slice(0, 5).map((int) => (
                  <div key={int.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{int.job?.title ?? "Position"}</p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(int.scheduledAt).toLocaleDateString("en-US", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                      </p>
                    </div>
                    <Badge variant="secondary">{int.interviewType}</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
