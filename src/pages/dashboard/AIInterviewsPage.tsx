import { useState, useEffect } from "react";
import { aiService, type AiInterview } from "@/services/aiService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Brain, Plus, Loader2, ArrowRight } from "lucide-react";
import { useNavigate } from "react-router";
import { AI_INTERVIEW_TYPE_LABELS, AI_INTERVIEW_STATUS_LABELS, AI_INTERVIEW_STATUS_COLORS } from "@/lib/constants";
import type { AIInterviewType } from "@/lib/constants";

export default function AIInterviewsPage() {
  const [interviews, setInterviews] = useState<AiInterview[] | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    aiService.getMyInterviews()
      .then(setInterviews)
      .catch(() => setInterviews([]));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Mock Interviews</h1>
          <p className="text-sm text-muted-foreground mt-1">Practice AI-powered mock interviews tailored to your profile.</p>
        </div>
        <Button onClick={() => navigate("/dashboard/ai-interviews/new")}>
          <Plus className="mr-2 size-4" /> Start Interview
        </Button>
      </div>

      {interviews === null ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : interviews.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Brain className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">No interviews yet</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Start your first AI-powered mock interview.</p>
            <Button onClick={() => navigate("/dashboard/ai-interviews/new")}><Plus className="mr-2 size-4" /> Start Interview</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {interviews.map((interview) => (
            <Card key={interview.id} className="cursor-pointer hover:border-primary/20 transition-colors" onClick={() => navigate(interview.status === "COMPLETED" ? `/dashboard/ai-interviews/${interview.id}/report` : `/dashboard/ai-interviews/${interview.id}`)}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-medium">{interview.job?.title ?? "Mock Interview"}</p>
                    <Badge variant="secondary">{AI_INTERVIEW_TYPE_LABELS[interview.interviewType as AIInterviewType] ?? interview.interviewType}</Badge>
                    <Badge className={AI_INTERVIEW_STATUS_COLORS[interview.status] ?? ""} variant="secondary">
                      {AI_INTERVIEW_STATUS_LABELS[interview.status] ?? interview.status}
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {interview.totalQuestions} questions · {new Date(interview.createdAt).toLocaleDateString()}
                    {interview.overallScore != null && ` · Score: ${interview.overallScore}/100`}
                  </p>
                </div>
                <ArrowRight className="size-4 text-muted-foreground shrink-0" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
