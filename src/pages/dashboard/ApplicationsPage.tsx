import { useState, useEffect } from "react";
import { applicationService, type Application } from "@/services/applicationService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Clock, FileText, ChevronRight, Building2, MapPin } from "lucide-react";
import { useNavigate } from "react-router";
import { STAGE_LABELS_FIXED, STAGE_COLORS, V1_STAGES } from "@/lib/constants";
import type { ApplicationStage } from "@/lib/constants";

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[] | null>(null);
  const navigate = useNavigate();
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    applicationService.getMyApplications()
      .then(setApplications)
      .catch(() => setApplications([]));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">My Applications</h1>
        <p className="text-sm text-muted-foreground mt-1">Track the status and history of every position you've applied to.</p>
      </div>

      {applications === null ? (
        <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">Loading applications...</div>
      ) : applications.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <FileText className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">No applications yet</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Browse open positions and submit your first application.</p>
            <Button onClick={() => navigate("/dashboard/jobs")}>Browse Positions</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {applications.map((app) => (
            <Card key={app.id}>
              <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold truncate">{app.job?.title ?? "Untitled Job"}</h3>
                      <Badge className={STAGE_COLORS[app.stage] ?? ""} variant="secondary">
                        {STAGE_LABELS_FIXED[app.stage as ApplicationStage] ?? app.stage}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-3 text-sm text-muted-foreground mb-3">
                      <span className="flex items-center gap-1"><Building2 className="size-3.5" /> {app.company?.name ?? "Unknown"}</span>
                      {app.job?.location && <span className="flex items-center gap-1"><MapPin className="size-3.5" /> {app.job.location}</span>}
                      <span className="flex items-center gap-1"><Clock className="size-3.5" /> Applied {new Date(app.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  <Button variant="ghost" size="sm" onClick={() => setExpandedId(expandedId === app.id ? null : app.id)}>
                    Timeline
                    <ChevronRight className={`ml-1 size-4 transition-transform ${expandedId === app.id ? "rotate-90" : ""}`} />
                  </Button>
                </div>

                {expandedId === app.id && (
                  <div className="mt-4 border-t pt-4">
                    <ApplicationTimeline
                      timeline={(app.timeline ?? []) as any}
                      currentStage={app.stage as ApplicationStage}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function ApplicationTimeline({
  timeline,
  currentStage,
}: {
  timeline: Array<{ stage: ApplicationStage; timestamp: string; note?: string }>;
  currentStage: ApplicationStage;
}) {
  return (
    <div className="space-y-0">
      {V1_STAGES.map((stage, index) => {
        const entry = timeline.find((e) => e.stage === stage);
        const isCompleted = entry !== undefined;
        const isCurrent = stage === currentStage && isCompleted;
        const isPast = V1_STAGES.indexOf(stage) < V1_STAGES.indexOf(currentStage) || currentStage === "rejected";

        return (
          <div key={stage} className="flex gap-3">
            <div className="flex flex-col items-center">
              <div className={`size-3 rounded-full border-2 ${isCompleted ? "border-green-500 bg-green-500" : isPast && currentStage !== "rejected" ? "border-green-300 bg-green-100" : "border-border bg-background"}`} />
              {index < V1_STAGES.length - 1 && <div className={`w-0.5 flex-1 ${isPast ? "bg-green-200" : "bg-border"}`} />}
            </div>
            <div className="pb-4">
              <p className={`text-sm font-medium ${isCurrent ? "text-foreground" : isCompleted ? "text-green-700" : "text-muted-foreground"}`}>
                {STAGE_LABELS_FIXED[stage]}
              </p>
              {entry && <p className="text-xs text-muted-foreground">{new Date(entry.timestamp).toLocaleString()}</p>}
              {entry?.note && <p className="text-xs text-muted-foreground mt-0.5 italic">{entry.note}</p>}
            </div>
          </div>
        );
      })}
    </div>
  );
}
