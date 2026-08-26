import { useState, useEffect } from "react";
import { aiService, type AiInterview, type AiInterviewQuestion } from "@/services/aiService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, Loader2, TrendingUp, Target, Lightbulb } from "lucide-react";
import { useNavigate, useParams } from "react-router";
import { AI_INTERVIEW_TYPE_LABELS } from "@/lib/constants";
import type { AIInterviewType } from "@/lib/constants";

export default function AIInterviewReportPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [interview, setInterview] = useState<AiInterview | null>(null);
  const [questions, setQuestions] = useState<AiInterviewQuestion[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      aiService.getInterviewReport(id).catch(() => aiService.getInterview(id)),
      aiService.getInterviewQuestions(id),
    ]).then(([int, qs]) => {
      setInterview(int);
      setQuestions(qs);
      setLoading(false);
    }).catch(() => {
      navigate("/dashboard/ai-interviews");
    });
  }, [id, navigate]);

  if (loading) {
    return <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>;
  }

  if (!interview) return null;

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/ai-interviews")}><ArrowLeft className="size-4" /></Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold tracking-tight">Interview Report</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {AI_INTERVIEW_TYPE_LABELS[interview.interviewType as AIInterviewType]} · {interview.totalQuestions} questions
          </p>
        </div>
      </div>

      {/* Overall Score */}
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-6">
            <div className="relative size-28">
              <svg className="size-28 -rotate-90" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" className="text-muted/30" />
                <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" strokeLinecap="round" strokeDasharray={`${((interview.overallScore ?? 0) / 100) * 264} 264`} className={(interview.overallScore ?? 0) >= 70 ? "text-green-500" : (interview.overallScore ?? 0) >= 40 ? "text-yellow-500" : "text-red-500"} />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center"><span className="text-3xl font-bold tabular-nums">{interview.overallScore ?? "—"}</span></div>
            </div>
            <div className="space-y-1">
              <h2 className="text-lg font-semibold">Overall Score: {interview.overallScore ?? "—"}/100</h2>
              {interview.technicalScore != null && <p className="text-sm text-muted-foreground">Technical: {interview.technicalScore} · Communication: {interview.communicationScore} · Problem Solving: {interview.problemSolvingScore}</p>}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Category scores */}
      {(interview.technicalScore != null || interview.communicationScore != null) && (
        <div className="grid gap-4 sm:grid-cols-3">
          <Card><CardContent className="p-4 text-center"><p className="text-xs text-muted-foreground mb-1">Technical</p><p className="text-2xl font-bold">{interview.technicalScore}</p></CardContent></Card>
          <Card><CardContent className="p-4 text-center"><p className="text-xs text-muted-foreground mb-1">Communication</p><p className="text-2xl font-bold">{interview.communicationScore}</p></CardContent></Card>
          <Card><CardContent className="p-4 text-center"><p className="text-xs text-muted-foreground mb-1">Problem Solving</p><p className="text-2xl font-bold">{interview.problemSolvingScore}</p></CardContent></Card>
        </div>
      )}

      {/* Strengths & Weaknesses */}
      <div className="grid gap-4 sm:grid-cols-2">
        {interview.strengths && interview.strengths.length > 0 && (
          <Card>
            <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><TrendingUp className="size-4 text-green-600" /> Strong Areas</CardTitle></CardHeader>
            <CardContent><div className="flex flex-wrap gap-1.5">{interview.strengths.map((s, i) => <Badge key={i} className="text-xs bg-green-100 text-green-700">{s}</Badge>)}</div></CardContent>
          </Card>
        )}
        {interview.weaknesses && interview.weaknesses.length > 0 && (
          <Card>
            <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Target className="size-4 text-amber-600" /> Needs Improvement</CardTitle></CardHeader>
            <CardContent><div className="flex flex-wrap gap-1.5">{interview.weaknesses.map((w, i) => <Badge key={i} variant="outline" className="text-xs">{w}</Badge>)}</div></CardContent>
          </Card>
        )}
      </div>

      {/* Missing concepts & recommendations */}
      {interview.missingConcepts && interview.missingConcepts.length > 0 && (
        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Lightbulb className="size-4 text-blue-600" /> Missing Concepts</CardTitle></CardHeader>
          <CardContent><div className="flex flex-wrap gap-1.5">{interview.missingConcepts.map((m, i) => <Badge key={i} variant="outline" className="text-xs">{m}</Badge>)}</div></CardContent>
        </Card>
      )}

      {interview.recommendedTopics && interview.recommendedTopics.length > 0 && (
        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Lightbulb className="size-4 text-primary" /> Recommended Topics</CardTitle></CardHeader>
          <CardContent><ul className="space-y-1">{interview.recommendedTopics.map((r, i) => <li key={i} className="text-sm flex items-start gap-2"><span className="mt-0.5 size-1 rounded-full bg-primary shrink-0" />{r}</li>)}</ul></CardContent>
        </Card>
      )}

      {/* Overall Feedback */}
      {interview.overallFeedback && (
        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-sm font-medium">Overall Feedback</CardTitle></CardHeader>
          <CardContent><p className="text-sm text-muted-foreground">{interview.overallFeedback}</p></CardContent>
        </Card>
      )}

      {/* Question-by-question */}
      {questions.length > 0 && (
        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-sm font-medium">Question Details</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {questions.map((q) => (
              <div key={q.id} className="rounded-lg border p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium">Q{q.questionNumber}: {q.question}</p>
                  {q.score != null && <Badge variant={q.score >= 70 ? "default" : "secondary"}>{q.score}/100</Badge>}
                </div>
                {q.studentAnswer && <p className="text-sm text-muted-foreground">Answer: {q.studentAnswer}</p>}
                {q.feedback && <p className="text-xs text-muted-foreground italic">{q.feedback}</p>}
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
