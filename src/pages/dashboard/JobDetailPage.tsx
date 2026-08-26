import { useState, useEffect } from "react";
import { jobService, type Job, type JobMatch } from "@/services/jobService";
import { applicationService, type Application } from "@/services/applicationService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ArrowLeft, Users, MapPin, Clock, DollarSign, Briefcase, XCircle, Loader2, Calendar, Sparkles, TrendingUp, Target, Lightbulb } from "lucide-react";
import { useNavigate, useParams } from "react-router";
import { JOB_TYPE_LABELS, STAGE_LABELS_FIXED, STAGE_COLORS, V1_STAGES } from "@/lib/constants";
import type { ApplicationStage, JobType } from "@/lib/constants";
import { toast } from "sonner";
import { ScheduleInterviewDialog } from "@/components/ScheduleInterviewDialog";

export default function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [job, setJob] = useState<Job | null>(null);
  const [applicants, setApplicants] = useState<Application[]>([]);
  const [match, setMatch] = useState<JobMatch | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [scheduleDialogApp, setScheduleDialogApp] = useState<string | null>(null);
  const [matching, setMatching] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    Promise.all([
      jobService.getJob(id).catch(() => null),
      applicationService.getJobApplicants(id).catch(() => []),
      jobService.getJobMatch(id).catch(() => null),
    ]).then(([j, apps, m]) => {
      setJob(j);
      setApplicants(apps);
      setMatch(m);
      setLoading(false);
    });
  }, [id]);

  const handleClose = async () => {
    if (!id) return;
    try {
      await jobService.closeJob(id);
      setJob((prev) => prev ? { ...prev, status: "CLOSED" } as Job : null);
      toast.success("Job closed");
    } catch {
      toast.error("Failed to close job");
    }
  };

  const handleStatusChange = async (applicationId: string, newStatus: ApplicationStage) => {
    setUpdatingId(applicationId);
    try {
      await applicationService.updateApplicationStatus(applicationId, newStatus);
      setApplicants((prev) => prev.map((a) => a.id === applicationId ? { ...a, stage: newStatus } as Application : a));
      toast.success(`Status updated to ${STAGE_LABELS_FIXED[newStatus]}`);
    } catch {
      toast.error("Failed to update status");
    } finally {
      setUpdatingId(null);
    }
  };

  const handleMatch = async () => {
    if (!id) return;
    setMatching(true);
    try {
      const result = await jobService.calculateMatch(id);
      setMatch(result);
      toast.success("Match calculated!");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to calculate match");
    } finally {
      setMatching(false);
    }
  };

  if (!id || loading) {
    return <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/jobs-manage")}><ArrowLeft className="size-4" /></Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold tracking-tight">{job?.title ?? "Loading..."}</h1>
          {job && (
            <div className="flex items-center gap-3 text-sm text-muted-foreground mt-1">
              <Badge variant={job.status === "ACTIVE" ? "default" : "secondary"}>{job.status}</Badge>
              <span className="flex items-center gap-1"><Users className="size-3.5" /> {job.applicationCount} applicants</span>
            </div>
          )}
        </div>
        {job?.status === "ACTIVE" && (
          <Button variant="outline" className="text-destructive hover:text-destructive" onClick={handleClose}>
            <XCircle className="mr-2 size-4" /> Close Job
          </Button>
        )}
      </div>

      {job && (
        <Card>
          <CardContent className="p-6">
            <div className="grid gap-4 sm:grid-cols-3 text-sm mb-4">
              <span className="flex items-center gap-1.5 text-muted-foreground"><MapPin className="size-3.5" /> {job.location}</span>
              <span className="flex items-center gap-1.5 text-muted-foreground"><Briefcase className="size-3.5" /> {JOB_TYPE_LABELS[job.type as JobType] ?? job.type}</span>
              {(job.salaryMin || job.salaryMax) && (
                <span className="flex items-center gap-1.5 text-green-600">
                  <DollarSign className="size-3.5" />
                  {job.salaryMin && job.salaryMax ? `$${job.salaryMin.toLocaleString()} - $${job.salaryMax.toLocaleString()}` : job.salaryMin ? `From $${job.salaryMin.toLocaleString()}` : `Up to $${job.salaryMax?.toLocaleString()}`}
                </span>
              )}
            </div>
            <p className="text-sm whitespace-pre-wrap">{job.description}</p>
            {job.skills.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-4">
                {job.skills.map((skill) => <Badge key={skill} variant="outline" className="text-xs">{skill}</Badge>)}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* AI Match Section */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Sparkles className="size-4 text-primary" /> AI Job Match
          </CardTitle>
        </CardHeader>
        <CardContent>
          {match ? (
            <div className="space-y-4">
              <div className="flex items-center gap-4">
                <div className="relative size-16">
                  <svg className="size-16 -rotate-90" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" className="text-muted/30" />
                    <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" strokeLinecap="round" strokeDasharray={`${(match.matchScore / 100) * 264} 264`} className={match.matchScore >= 70 ? "text-green-500" : match.matchScore >= 40 ? "text-yellow-500" : "text-red-500"} />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center"><span className="text-lg font-bold tabular-nums">{match.matchScore}%</span></div>
                </div>
                <div>
                  <p className="text-sm font-medium">{match.matchScore}% Match</p>
                  <p className="text-xs text-muted-foreground mt-0.5">{match.explanation}</p>
                </div>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-2 flex items-center gap-1"><TrendingUp className="size-3" /> Matched Skills</p>
                  <div className="flex flex-wrap gap-1.5">
                    {match.matchedSkills.length > 0 ? match.matchedSkills.map((s) => <Badge key={s} className="text-xs bg-green-100 text-green-700">{s}</Badge>) : <p className="text-xs text-muted-foreground">None identified</p>}
                  </div>
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-2 flex items-center gap-1"><Target className="size-3" /> Missing Skills</p>
                  <div className="flex flex-wrap gap-1.5">
                    {match.missingSkills.length > 0 ? match.missingSkills.map((s) => <Badge key={s} variant="outline" className="text-xs">{s}</Badge>) : <p className="text-xs text-muted-foreground">None identified</p>}
                  </div>
                </div>
              </div>
              {match.recommendations.length > 0 && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-2 flex items-center gap-1"><Lightbulb className="size-3" /> Recommendations</p>
                  <ul className="space-y-1">
                    {match.recommendations.map((r, i) => <li key={i} className="text-xs text-muted-foreground flex items-start gap-2"><span className="mt-0.5 size-1 rounded-full bg-primary shrink-0" />{r}</li>)}
                  </ul>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">See how well your profile matches this position.</p>
              <Button variant="outline" size="sm" disabled={matching} onClick={handleMatch}>
                {matching ? <Loader2 className="mr-1.5 size-3 animate-spin" /> : <Sparkles className="mr-1.5 size-3" />}
                {matching ? "Analyzing..." : "Analyze Match"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Applicants */}
      <Card>
        <CardHeader><CardTitle className="text-base">Applicants ({applicants.length})</CardTitle></CardHeader>
        <CardContent>
          {applicants.length === 0 ? (
            <div className="py-8 text-center text-sm text-muted-foreground">No applicants yet.</div>
          ) : (
            <div className="space-y-3">
              {applicants.map((app) => (
                <div key={app.id} className="rounded-lg border p-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <p className="font-medium">{app.studentUser?.name ?? "Anonymous Student"}</p>
                      <p className="text-sm text-muted-foreground">{app.studentUser?.email ?? "No email provided"}</p>
                      {app.student?.skills && app.student.skills.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-2">
                          {app.student.skills.slice(0, 8).map((s) => <Badge key={s} variant="outline" className="text-[10px]">{s}</Badge>)}
                        </div>
                      )}
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <Badge className={STAGE_COLORS[app.stage] ?? ""} variant="secondary">{STAGE_LABELS_FIXED[app.stage as ApplicationStage]}</Badge>
                      <Button size="sm" variant="outline" className="h-8 text-xs" onClick={() => setScheduleDialogApp(app.id)}>
                        <Calendar className="mr-1 size-3" /> Schedule
                      </Button>
                      <Select value={app.stage} onValueChange={(v) => handleStatusChange(app.id, v as ApplicationStage)} disabled={updatingId === app.id}>
                        <SelectTrigger className="w-36 h-8 text-xs">
                          <SelectValue />
                          {updatingId === app.id && <Loader2 className="size-3 animate-spin ml-1" />}
                        </SelectTrigger>
                        <SelectContent>
                          {V1_STAGES.map((stage) => <SelectItem key={stage} value={stage}>{STAGE_LABELS_FIXED[stage]}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {scheduleDialogApp && job && (
        <ScheduleInterviewDialog open={!!scheduleDialogApp} onOpenChange={(open) => { if (!open) setScheduleDialogApp(null); }} applicationId={scheduleDialogApp} jobTitle={job.title} />
      )}
    </div>
  );
}
