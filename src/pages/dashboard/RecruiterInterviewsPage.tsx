import { useState, useEffect } from "react";
import { interviewService, type Interview } from "@/services/interviewService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Calendar, Clock, Building2, Loader2, CheckCircle, XCircle, MessageSquare } from "lucide-react";
import { INTERVIEW_TYPE_LABELS, INTERVIEW_STATUS_LABELS, INTERVIEW_STATUS_COLORS, INTERVIEW_TYPE_COLORS } from "@/lib/constants";
import type { InterviewType, InterviewStatus } from "@/lib/constants";
import { toast } from "sonner";

export default function RecruiterInterviewsPage() {
  const [interviews, setInterviews] = useState<Interview[] | null>(null);
  const [feedbackId, setFeedbackId] = useState<string | null>(null);
  const [feedbackText, setFeedbackText] = useState("");

  useEffect(() => {
    interviewService.getRecruiterInterviews()
      .then(setInterviews)
      .catch(() => setInterviews([]));
  }, []);

  const handleComplete = async (id: string) => {
    try {
      await interviewService.completeInterview(id);
      setInterviews((prev) => prev?.map((i) => i.id === id ? { ...i, status: "COMPLETED" } as Interview : i) ?? null);
      toast.success("Interview marked as completed");
    } catch { toast.error("Failed to complete interview"); }
  };

  const handleCancel = async (id: string) => {
    try {
      await interviewService.cancelInterview(id);
      setInterviews((prev) => prev?.map((i) => i.id === id ? { ...i, status: "CANCELLED" } as Interview : i) ?? null);
      toast.success("Interview cancelled");
    } catch { toast.error("Failed to cancel interview"); }
  };

  const handleSubmitFeedback = async (id: string) => {
    if (!feedbackText.trim()) { toast.error("Please enter feedback"); return; }
    try {
      await interviewService.submitFeedback(id, { feedback: feedbackText });
      setInterviews((prev) => prev?.map((i) => i.id === id ? { ...i, feedback: feedbackText } as Interview : i) ?? null);
      setFeedbackId(null);
      setFeedbackText("");
      toast.success("Feedback submitted");
    } catch { toast.error("Failed to submit feedback"); }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Interviews</h1>
        <p className="text-sm text-muted-foreground mt-1">Manage scheduled interviews for your applicants.</p>
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
          {interviews.map((interview) => (
            <Card key={interview.id}>
              <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
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
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {interview.status === "SCHEDULED" && (
                      <>
                        <Button size="sm" variant="outline" onClick={() => handleComplete(interview.id)}><CheckCircle className="mr-1 size-3" /> Complete</Button>
                        <Button size="sm" variant="outline" className="text-destructive" onClick={() => handleCancel(interview.id)}><XCircle className="mr-1 size-3" /> Cancel</Button>
                      </>
                    )}
                    {interview.status === "COMPLETED" && !interview.feedback && (
                      <Button size="sm" variant="outline" onClick={() => { setFeedbackId(interview.id); setFeedbackText(""); }}>
                        <MessageSquare className="mr-1 size-3" /> Feedback
                      </Button>
                    )}
                  </div>
                </div>
                {feedbackId === interview.id && (
                  <div className="mt-3 space-y-2">
                    <Textarea placeholder="Enter interview feedback..." value={feedbackText} onChange={(e) => setFeedbackText(e.target.value)} rows={3} />
                    <div className="flex gap-2">
                      <Button size="sm" onClick={() => handleSubmitFeedback(interview.id)}>Submit</Button>
                      <Button size="sm" variant="ghost" onClick={() => setFeedbackId(null)}>Cancel</Button>
                    </div>
                  </div>
                )}
                {interview.feedback && (
                  <div className="mt-3 p-3 rounded-lg bg-muted/50">
                    <p className="text-xs font-medium mb-1">Feedback</p>
                    <p className="text-sm text-muted-foreground">{interview.feedback}</p>
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
