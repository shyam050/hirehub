import { useState, useEffect } from "react";
import { interviewService, type Interview } from "@/services/interviewService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Calendar, Clock, MapPin, Building2, Loader2 } from "lucide-react";
import { INTERVIEW_TYPE_LABELS, INTERVIEW_STATUS_LABELS, INTERVIEW_STATUS_COLORS, INTERVIEW_TYPE_COLORS } from "@/lib/constants";
import type { InterviewType, InterviewStatus } from "@/lib/constants";

export default function StudentInterviewsPage() {
  const [interviews, setInterviews] = useState<Interview[] | null>(null);

  useEffect(() => {
    interviewService.getMyStudentInterviews()
      .then((res) => {
        const data = Array.isArray(res) ? res : (res as any)?.content || [];
        setInterviews(data);
      })
      .catch(() => setInterviews([]));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Interviews</h1>
        <p className="text-sm text-muted-foreground mt-1">Your scheduled interviews and upcoming sessions.</p>
      </div>

      {interviews === null ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : interviews.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Calendar className="size-12 text-muted-foreground/30 mb-3" />
            <p className="text-sm text-muted-foreground">No interviews scheduled yet.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {(Array.isArray(interviews) ? interviews : []).map((interview) => (
            <Card key={interview.id}>
              <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold">{interview.job?.title ?? "Position"}</h3>
                      <Badge className={INTERVIEW_TYPE_COLORS[interview.interviewType] ?? ""} variant="secondary">
                        {INTERVIEW_TYPE_LABELS[interview.interviewType as InterviewType] ?? interview.interviewType}
                      </Badge>
                      <Badge className={INTERVIEW_STATUS_COLORS[interview.status] ?? ""} variant="secondary">
                        {INTERVIEW_STATUS_LABELS[interview.status as InterviewStatus] ?? interview.status}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-3 text-sm text-muted-foreground mt-1">
                      <span className="flex items-center gap-1"><Building2 className="size-3.5" /> {interview.company?.name ?? "Company"}</span>
                      <span className="flex items-center gap-1"><Clock className="size-3.5" /> {new Date(interview.scheduledAt).toLocaleDateString("en-US", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}</span>
                      <span>{interview.duration} min</span>
                    </div>
                    {interview.meetingLink && (
                      <p className="text-xs text-muted-foreground mt-2">
                        Meeting: <a href={interview.meetingLink} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">{interview.meetingLink}</a>
                      </p>
                    )}
                    {interview.feedback && (
                      <div className="mt-3 p-3 rounded-lg bg-muted/50">
                        <p className="text-xs font-medium mb-1">Feedback</p>
                        <p className="text-sm text-muted-foreground">{interview.feedback}</p>
                      </div>
                    )}
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