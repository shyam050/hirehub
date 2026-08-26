import { useState, useEffect } from "react";
import { jobService, type Job } from "@/services/jobService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Briefcase, Loader2, Building2 } from "lucide-react";

export default function AdminAllJobsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    jobService.browseJobs({ size: 100 })
      .then((r) => setJobs(r.content ?? []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">All Positions</h1>
        <p className="text-sm text-muted-foreground mt-1">All job postings across the platform.</p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : jobs.length === 0 ? (
        <Card className="border-dashed"><CardContent className="flex flex-col items-center justify-center py-12"><Briefcase className="size-12 text-muted-foreground/30 mb-3" /><p className="text-sm text-muted-foreground">No jobs posted yet.</p></CardContent></Card>
      ) : (
        <div className="space-y-3">
          {jobs.map((j) => (
            <Card key={j.id}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium">{j.title}</p>
                  <p className="text-sm text-muted-foreground flex items-center gap-1"><Building2 className="size-3" /> {j.company?.name ?? "—"}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={j.status === "ACTIVE" ? "default" : "secondary"}>{j.status}</Badge>
                  <Badge variant="outline">{j.applicationCount} applicants</Badge>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
