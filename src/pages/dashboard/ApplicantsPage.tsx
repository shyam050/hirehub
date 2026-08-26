import { useState, useEffect } from "react";
import { jobService, type Job } from "@/services/jobService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Users, ArrowRight, Briefcase, Loader2 } from "lucide-react";
import { useNavigate } from "react-router";

export default function ApplicantsPage() {
  const [jobs, setJobs] = useState<Job[] | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    jobService.getMyCompanyJobs().then(setJobs).catch(() => setJobs([]));
  }, []);

  const jobsWithApplicants = jobs?.filter((j) => j.applicationCount > 0) ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Applicants</h1>
        <p className="text-sm text-muted-foreground mt-1">Review applicants across your active listings.</p>
      </div>

      {jobs === null ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : jobsWithApplicants.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Users className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">No applicants yet</h3>
            <p className="text-sm text-muted-foreground mt-1">Applicants will appear here once students start applying to your listings.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {jobsWithApplicants.map((job) => (
            <Card key={job.id} className="cursor-pointer hover:border-primary/20 transition-colors" onClick={() => navigate(`/dashboard/jobs-manage/${job.id}`)}>
              <CardContent className="flex items-center justify-between p-5">
                <div className="flex items-center gap-3">
                  <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary"><Briefcase className="size-5" /></div>
                  <div>
                    <p className="font-medium">{job.title}</p>
                    <p className="text-sm text-muted-foreground">{job.applicationCount} applicant{job.applicationCount !== 1 ? "s" : ""}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">{job.applicationCount}</Badge>
                  <ArrowRight className="size-4 text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
