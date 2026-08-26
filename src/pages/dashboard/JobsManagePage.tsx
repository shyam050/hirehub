import { useState, useEffect } from "react";
import { jobService, type Job } from "@/services/jobService";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Plus, Briefcase, Users, ArrowRight, XCircle, Clock, Loader2 } from "lucide-react";
import { useNavigate } from "react-router";
import { JOB_TYPE_LABELS, JOB_STATUS_LABELS } from "@/lib/constants";
import type { JobType, JobStatus } from "@/lib/constants";
import { toast } from "sonner";

export default function JobsManagePage() {
  const [jobs, setJobs] = useState<Job[] | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    jobService.getMyCompanyJobs().then(setJobs).catch(() => setJobs([]));
  }, []);

  const handleClose = async (jobId: string) => {
    try {
      await jobService.closeJob(jobId);
      setJobs((prev) => prev?.map((j) => j.id === jobId ? { ...j, status: "CLOSED" } as Job : j) ?? null);
      toast.success("Job closed");
    } catch {
      toast.error("Failed to close job");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Positions</h1>
          <p className="text-sm text-muted-foreground mt-1">Manage your listings and review incoming applicants.</p>
        </div>
        <Button onClick={() => navigate("/dashboard/jobs-manage/new")}><Plus className="mr-2 size-4" /> Post Position</Button>
      </div>

      {jobs === null ? (
        <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">Loading jobs...</div>
      ) : jobs.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Briefcase className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">No positions posted yet</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Create your first listing to start receiving applications.</p>
            <Button onClick={() => navigate("/dashboard/jobs-manage/new")}><Plus className="mr-2 size-4" /> Post Position</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {jobs.map((job) => (
            <Card key={job.id} className="hover:border-primary/20 transition-colors cursor-pointer" onClick={() => navigate(`/dashboard/jobs-manage/${job.id}`)}>
              <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold">{job.title}</h3>
                      <Badge variant={job.status === "ACTIVE" ? "default" : "secondary"}>{JOB_STATUS_LABELS[job.status as JobStatus] ?? job.status}</Badge>
                      <Badge variant="outline">{JOB_TYPE_LABELS[job.type as JobType] ?? job.type}</Badge>
                    </div>
                    <div className="flex items-center gap-4 text-sm text-muted-foreground mb-2">
                      <span className="flex items-center gap-1"><Users className="size-3.5" /> {job.applicationCount} applicants</span>
                      <span className="flex items-center gap-1"><Clock className="size-3.5" /> {new Date(job.createdAt).toLocaleDateString()}</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {job.skills.slice(0, 5).map((skill) => <Badge key={skill} variant="outline" className="text-xs">{skill}</Badge>)}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {job.status === "ACTIVE" && (
                      <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" onClick={(e) => { e.stopPropagation(); handleClose(job.id); }}>
                        <XCircle className="mr-1 size-3" /> Close
                      </Button>
                    )}
                    <ArrowRight className="size-4 text-muted-foreground" />
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
