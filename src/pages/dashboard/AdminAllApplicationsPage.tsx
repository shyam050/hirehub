import { useState, useEffect } from "react";
import { applicationService, type Application } from "@/services/applicationService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ClipboardList, Loader2, Building2 } from "lucide-react";
import { STAGE_LABELS_FIXED, STAGE_COLORS } from "@/lib/constants";

export default function AdminAllApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    applicationService.getAllApplications()
      .then(setApplications)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">All Applications</h1>
        <p className="text-sm text-muted-foreground mt-1">All job applications across the platform.</p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : applications.length === 0 ? (
        <Card className="border-dashed"><CardContent className="flex flex-col items-center justify-center py-12"><ClipboardList className="size-12 text-muted-foreground/30 mb-3" /><p className="text-sm text-muted-foreground">No applications yet.</p></CardContent></Card>
      ) : (
        <div className="space-y-3">
          {applications.map((a) => (
            <Card key={a.id}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium">{a.job?.title ?? "Untitled"}</p>
                  <p className="text-sm text-muted-foreground flex items-center gap-1"><Building2 className="size-3" /> {a.company?.name ?? "—"}</p>
                </div>
                <Badge className={STAGE_COLORS[a.stage] ?? ""} variant="secondary">
                  {STAGE_LABELS_FIXED[a.stage] ?? a.stage}
                </Badge>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
