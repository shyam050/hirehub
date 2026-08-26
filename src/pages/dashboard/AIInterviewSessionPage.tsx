import { useState, useEffect } from "react";
import { aiService, type AiInterview, type AiInterviewQuestion } from "@/services/aiService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, Loader2, Send, CheckCircle } from "lucide-react";
import { useNavigate, useParams } from "react-router";
import { toast } from "sonner";

export default function AIInterviewSessionPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [interview, setInterview] = useState<AiInterview | null>(null);
  const [questions, setQuestions] = useState<AiInterviewQuestion[]>([]);
  const [currentQuestion, setCurrentQuestion] = useState<AiInterviewQuestion | null>(null);
  const [answer, setAnswer] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [evaluation, setEvaluation] = useState<{ score: number; strengths: string[]; weaknesses: string[]; feedback: string } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      aiService.getInterview(id),
      aiService.getInterviewQuestions(id),
    ]).then(([int, qs]) => {
      setInterview(int);
      setQuestions(qs);
      if (qs.length > 0) {
        setCurrentQuestion(qs[qs.length - 1]);
        if (qs[qs.length - 1].studentAnswer) {
          setEvaluation({
            score: qs[qs.length - 1].score ?? 0,
            strengths: qs[qs.length - 1].strengths ?? [],
            weaknesses: qs[qs.length - 1].weaknesses ?? [],
            feedback: qs[qs.length - 1].feedback ?? "",
          });
        }
      }
      setLoading(false);
    }).catch(() => {
      toast.error("Failed to load interview");
      navigate("/dashboard/ai-interviews");
    });
  }, [id, navigate]);

  const handleSubmitAnswer = async () => {
    if (!id || !currentQuestion || !answer.trim()) return;
    setSubmitting(true);
    try {
      const result = await aiService.submitAnswer(id, currentQuestion.questionNumber, answer);
      setEvaluation(result);
      // Refresh questions
      const updatedQuestions = await aiService.getInterviewQuestions(id);
      setQuestions(updatedQuestions);
      setAnswer("");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to submit answer");
    } finally {
      setSubmitting(false);
    }
  };

  const handleNextQuestion = async () => {
    if (!id) return;
    setGenerating(true);
    try {
      const newQuestion = await aiService.generateNextQuestion(id);
      setCurrentQuestion(newQuestion);
      setQuestions((prev) => [...prev, newQuestion]);
      setEvaluation(null);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to generate next question");
    } finally {
      setGenerating(false);
    }
  };

  const handleComplete = async () => {
    if (!id) return;
    try {
      await aiService.completeInterview(id);
      toast.success("Interview completed!");
      navigate(`/dashboard/ai-interviews/${id}/report`);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to complete interview");
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>;
  }

  if (!interview || !currentQuestion) {
    return <div className="text-center py-12"><p className="text-muted-foreground">No questions available.</p></div>;
  }

  const isLastQuestion = currentQuestion.questionNumber >= interview.totalQuestions;
  const hasAnswer = !!currentQuestion.studentAnswer;

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/ai-interviews")}><ArrowLeft className="size-4" /></Button>
        <div className="flex-1">
          <h1 className="text-lg font-bold tracking-tight">Mock Interview</h1>
          <p className="text-sm text-muted-foreground">Question {currentQuestion.questionNumber} of {interview.totalQuestions}</p>
        </div>
        <Badge variant="secondary">{interview.interviewType}</Badge>
      </div>

      {/* Progress */}
      <div className="w-full bg-muted rounded-full h-2">
        <div className="bg-primary h-2 rounded-full transition-all" style={{ width: `${(currentQuestion.questionNumber / interview.totalQuestions) * 100}%` }} />
      </div>

      {/* Question */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Question {currentQuestion.questionNumber}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-base">{currentQuestion.question}</p>
          {currentQuestion.category && <Badge variant="outline" className="mt-2 text-xs">{currentQuestion.category}</Badge>}
        </CardContent>
      </Card>

      {/* Answer area */}
      {!hasAnswer && !evaluation ? (
        <Card>
          <CardContent className="space-y-4 pt-6">
            <Textarea placeholder="Type your answer here..." value={answer} onChange={(e) => setAnswer(e.target.value)} rows={6} disabled={submitting} />
            <Button onClick={handleSubmitAnswer} disabled={!answer.trim() || submitting}>
              {submitting ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Send className="mr-2 size-4" />}
              {submitting ? "Evaluating..." : "Submit Answer"}
            </Button>
          </CardContent>
        </Card>
      ) : evaluation ? (
        <Card>
          <CardHeader><CardTitle className="text-sm font-medium">Evaluation</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-4">
              <div className={`size-16 rounded-full flex items-center justify-center text-lg font-bold ${evaluation.score >= 70 ? "bg-green-100 text-green-700" : evaluation.score >= 40 ? "bg-yellow-100 text-yellow-700" : "bg-red-100 text-red-700"}`}>
                {evaluation.score}
              </div>
              <div>
                <p className="font-medium">Score: {evaluation.score}/100</p>
                <p className="text-sm text-muted-foreground">{evaluation.feedback}</p>
              </div>
            </div>
            {evaluation.strengths.length > 0 && (
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-1">Strengths</p>
                <div className="flex flex-wrap gap-1.5">{evaluation.strengths.map((s, i) => <Badge key={i} className="text-xs bg-green-100 text-green-700">{s}</Badge>)}</div>
              </div>
            )}
            {evaluation.weaknesses.length > 0 && (
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-1">Areas to improve</p>
                <div className="flex flex-wrap gap-1.5">{evaluation.weaknesses.map((w, i) => <Badge key={i} variant="outline" className="text-xs">{w}</Badge>)}</div>
              </div>
            )}
            <div className="flex gap-3 pt-2">
              {!isLastQuestion && (
                <Button onClick={handleNextQuestion} disabled={generating}>
                  {generating ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
                  {generating ? "Generating..." : "Next Question"}
                </Button>
              )}
              {isLastQuestion && (
                <Button onClick={handleComplete}>
                  <CheckCircle className="mr-2 size-4" /> Complete Interview
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
