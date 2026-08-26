import { useState, useEffect } from "react";
import { aiService, type AiInterview } from "@/services/aiService";
import { jobService, type Job } from "@/services/jobService";
import { resumeService, type Resume } from "@/services/resumeService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ArrowLeft, Brain, Loader2 } from "lucide-react";
import { useNavigate } from "react-router";
import { AI_INTERVIEW_TYPE_OPTIONS, AI_DIFFICULTY_OPTIONS } from "@/lib/constants";
import type { AIInterviewType, AIDifficulty } from "@/lib/constants";
import { toast } from "sonner";

export default function AIInterviewNewPage() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [resumes, setResumes] = useState<Resume[]>([]);
  const [selectedJob, setSelectedJob] = useState("");
  const [selectedResume, setSelectedResume] = useState("");
  const [interviewType, setInterviewType] = useState<AIInterviewType>("technical");
  const [difficulty, setDifficulty] = useState<AIDifficulty>("medium");
  const [totalQuestions, setTotalQuestions] = useState("5");
  const [starting, setStarting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      jobService.browseJobs({ size: 50 }).then((r) => r.content ?? []),
      resumeService.getMyResumes(),
    ]).then(([j, r]) => {
      setJobs(j);
      setResumes(r);
      const defaultResume = r.find((res) => res.isDefault);
      if (defaultResume) setSelectedResume(defaultResume.id);
      else if (r.length > 0) setSelectedResume(r[0].id);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const handleStart = async () => {
    if (!selectedJob) { toast.error("Please select a job"); return; }
    if (!selectedResume) { toast.error("Please select a resume"); return; }
    setStarting(true);
    try {
      const result = await aiService.startInterview({
        jobId: selectedJob,
        resumeId: selectedResume,
        interviewType,
        difficulty,
        totalQuestions: parseInt(totalQuestions),
      });
      toast.success("Interview started!");
      navigate(`/dashboard/ai-interviews/${result.interview.id}`);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to start interview");
    } finally {
      setStarting(false);
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>;
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/ai-interviews")}><ArrowLeft className="size-4" /></Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">New Mock Interview</h1>
          <p className="text-sm text-muted-foreground mt-1">Configure and start an AI-powered mock interview.</p>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle className="text-base flex items-center gap-2"><Brain className="size-4 text-primary" /> Interview Setup</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>Select Job</Label>
            <Select value={selectedJob} onValueChange={setSelectedJob}>
              <SelectTrigger><SelectValue placeholder="Choose a position to interview for" /></SelectTrigger>
              <SelectContent>
                {jobs.map((j) => <SelectItem key={j.id} value={j.id}>{j.title} — {j.company?.name ?? "Company"}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Select Resume</Label>
            <Select value={selectedResume} onValueChange={setSelectedResume}>
              <SelectTrigger><SelectValue placeholder="Choose your resume" /></SelectTrigger>
              <SelectContent>
                {resumes.map((r) => <SelectItem key={r.id} value={r.id}>{r.filename}{r.isDefault ? " (Default)" : ""}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Interview Type</Label>
            <Select value={interviewType} onValueChange={(v) => setInterviewType(v as AIInterviewType)}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {AI_INTERVIEW_TYPE_OPTIONS.map((opt) => <SelectItem key={opt.value} value={opt.value}>{opt.label} — {opt.description}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Difficulty</Label>
            <Select value={difficulty} onValueChange={(v) => setDifficulty(v as AIDifficulty)}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {AI_DIFFICULTY_OPTIONS.map((opt) => <SelectItem key={opt.value} value={opt.value}>{opt.label} — {opt.description}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Number of Questions</Label>
            <Select value={totalQuestions} onValueChange={setTotalQuestions}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="5">5 Questions</SelectItem>
                <SelectItem value="10">10 Questions</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button onClick={handleStart} disabled={starting || !selectedJob || !selectedResume} size="lg">
          {starting ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Brain className="mr-2 size-4" />}
          {starting ? "Starting..." : "Start Interview"}
        </Button>
      </div>
    </div>
  );
}
