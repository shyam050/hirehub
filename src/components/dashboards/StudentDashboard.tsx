import { useState, useEffect } from "react";
import { applicationService, type Application } from "@/services/applicationService";
import { interviewService, type Interview } from "@/services/interviewService";
import { jobService, type JobMatch, type SkillGap } from "@/services/jobService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Briefcase,
  FileText,
  ArrowRight,
  Upload,
  Calendar,
  MapPin,
  Building2,
  Sparkles,
  Target,
} from "lucide-react";
import { useNavigate } from "react-router";
import { STAGE_LABELS_FIXED, STAGE_COLORS, INTERVIEW_TYPE_LABELS, INTERVIEW_TYPE_COLORS } from "@/lib/constants";
import type { ApplicationStage, InterviewType } from "@/lib/constants";

export function StudentDashboard() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [interviews, setInterviews] = useState<Interview[]>([]);
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [skillGaps, setSkillGaps] = useState<SkillGap[]>([]);
  const [loaded, setLoaded] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([
      applicationService.getMyApplications().catch(() => []),
      interviewService.getMyStudentInterviews().catch(() => []),
      jobService.getMyJobMatches().catch(() => []),
      jobService.getMySkillGaps().catch(() => []),
    ]).then(([apps, ints, m, gaps]) => {
      setApplications(apps);
      setInterviews(ints);
      setMatches(m);
      setSkillGaps(gaps);
      setLoaded(true);
    });
  }, []);

  const upcomingInterviews =
    interviews?.filter(
      (i) => i.status === "SCHEDULED" || i.status === "RESCHEDULED"
    ) ?? [];

  const stats = {
    total: applications.length,
    shortlisted: applications.filter((a) => a.stage === "SHORTLISTED").length,
    applied: applications.filter((a) => a.stage === "APPLIED").length,
    rejected: applications.filter((a) => a.stage === "REJECTED").length,
  };

  if (!loaded) {
    return <div className="flex items-center justify-center py-12"><div className="animate-pulse text-muted-foreground">Loading...</div></div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">Your application activity at a glance.</p>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums">{stats.total}</span>
          <span className="text-sm text-muted-foreground">applied</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-green-600">{stats.shortlisted}</span>
          <span className="text-sm text-muted-foreground">shortlisted</span>
        </div>
        <span className="text-border">·</span>
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold tabular-nums text-muted-foreground/50">{stats.rejected}</span>
          <span className="text-sm text-muted-foreground">rejected</span>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        {/* Recent applications */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium">Recent applications</CardTitle>
              <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate("/dashboard/applications")}>
                View all <ArrowRight className="ml-1 size-3" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {applications.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-sm text-muted-foreground">No applications yet.</p>
                <Button variant="link" size="sm" className="mt-1" onClick={() => navigate("/dashboard/jobs")}>Browse open positions</Button>
              </div>
            ) : (
              <div className="space-y-0 divide-y divide-border">
                {applications.slice(0, 5).map((app) => (
                  <div key={app.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{app.job?.title ?? "Untitled"}</p>
                      <p className="text-xs text-muted-foreground">{app.company?.name ?? "Unknown company"}</p>
                    </div>
                    <Badge className={STAGE_COLORS[app.stage] ?? ""} variant="secondary">
                      {STAGE_LABELS_FIXED[app.stage] ?? app.stage}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Upcoming interviews */}
        {upcomingInterviews.length > 0 && (
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-medium">Upcoming interviews</CardTitle>
                <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate("/dashboard/interviews")}>
                  View all <ArrowRight className="ml-1 size-3" />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-0 divide-y divide-border">
                {upcomingInterviews.slice(0, 3).map((interview) => (
                  <div key={interview.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{interview.job?.title ?? "Position"}</p>
                      <p className="text-xs text-muted-foreground">
                        {interview.company?.name} · {new Date(interview.scheduledAt).toLocaleDateString("en-US", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                      </p>
                    </div>
                    <Badge className={INTERVIEW_TYPE_COLORS[interview.interviewType] ?? ""} variant="secondary">
                      {INTERVIEW_TYPE_LABELS[interview.interviewType as InterviewType]}
                    </Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Recommended Jobs */}
        {matches && matches.length > 0 && (
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Sparkles className="size-4 text-primary" /> Recommended Positions
                </CardTitle>
                <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate("/dashboard/jobs")}>
                  Browse all <ArrowRight className="ml-1 size-3" />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-0 divide-y divide-border">
                {matches.slice(0, 4).map((m) => (
                  <button key={m.id} className="flex w-full items-center justify-between py-3 text-left first:pt-0 last:pb-0" onClick={() => navigate("/dashboard/jobs")}>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium truncate">{m.job?.title}</p>
                        <Badge variant="secondary" className={`text-xs shrink-0 ${m.matchScore >= 70 ? "bg-green-100 text-green-700" : m.matchScore >= 40 ? "bg-yellow-100 text-yellow-700" : "bg-red-100 text-red-700"}`}>
                          {m.matchScore}%
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {m.job?.company?.name} · {m.job?.location}
                        {m.matchedSkills.length > 0 && ` · ${m.matchedSkills.slice(0, 3).join(", ")}`}
                      </p>
                    </div>
                    <ArrowRight className="size-3.5 text-muted-foreground shrink-0" />
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Skills to Learn */}
        {skillGaps && skillGaps.length > 0 && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Target className="size-4 text-amber-600" /> Skills You Should Learn
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {skillGaps.slice(0, 5).map((gap) => (
                  <div key={gap.skill} className="flex items-center justify-between">
                    <span className="text-sm">{gap.skill}</span>
                    <span className="text-xs text-muted-foreground">missing in {gap.count} matched job{gap.count !== 1 ? "s" : ""}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Quick links */}
        <div className="space-y-3">
          <button onClick={() => navigate("/dashboard/jobs")} className="flex w-full items-center justify-between rounded-lg border border-border p-4 text-left transition-colors hover:bg-muted/50">
            <div className="flex items-center gap-3"><Briefcase className="size-4 text-muted-foreground" /><span className="text-sm font-medium">Browse positions</span></div>
            <ArrowRight className="size-3.5 text-muted-foreground" />
          </button>
          <button onClick={() => navigate("/dashboard/profile")} className="flex w-full items-center justify-between rounded-lg border border-border p-4 text-left transition-colors hover:bg-muted/50">
            <div className="flex items-center gap-3"><FileText className="size-4 text-muted-foreground" /><span className="text-sm font-medium">Edit profile</span></div>
            <ArrowRight className="size-3.5 text-muted-foreground" />
          </button>
          <button onClick={() => navigate("/dashboard/resumes")} className="flex w-full items-center justify-between rounded-lg border border-border p-4 text-left transition-colors hover:bg-muted/50">
            <div className="flex items-center gap-3"><Upload className="size-4 text-muted-foreground" /><span className="text-sm font-medium">Upload resume</span></div>
            <ArrowRight className="size-3.5 text-muted-foreground" />
          </button>
        </div>
      </div>
    </div>
  );
}
