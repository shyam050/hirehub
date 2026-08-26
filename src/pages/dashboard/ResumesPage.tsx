import { useState, useRef, useEffect } from "react";
import { resumeService, type Resume, type ResumeAnalysis } from "@/services/resumeService";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Upload, FileText, Trash2, Star, CheckCircle, Sparkles, Loader2, BarChart3, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import { useNavigate } from "react-router";

export default function ResumesPage() {
  const [resumes, setResumes] = useState<Resume[] | null>(null);
  const [analyses, setAnalyses] = useState<ResumeAnalysis[]>([]);
  const [uploading, setUploading] = useState(false);
  const [analyzingId, setAnalyzingId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  const fetchResumes = async () => {
    try {
      const data = await resumeService.getMyResumes();
      setResumes(data);
    } catch { setResumes([]); }
  };

  const fetchAnalyses = async () => {
    try {
      const data = await resumeService.getMyAnalyses();
      setAnalyses(data);
    } catch { setAnalyses([]); }
  };

  useEffect(() => { fetchResumes(); fetchAnalyses(); }, []);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.type !== "application/pdf") { toast.error("Only PDF files are accepted"); return; }
    if (file.size > 10 * 1024 * 1024) { toast.error("File size must be under 10MB"); return; }
    setUploading(true);
    try {
      await resumeService.uploadResume(file);
      toast.success("Resume uploaded successfully!");
      if (fileInputRef.current) fileInputRef.current.value = "";
      fetchResumes();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to upload resume");
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await resumeService.deleteResume(id);
      toast.success("Resume deleted");
      fetchResumes();
    } catch { toast.error("Failed to delete resume"); }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await resumeService.setDefaultResume(id);
      toast.success("Default resume updated");
      fetchResumes();
    } catch { toast.error("Failed to update default resume"); }
  };

  const handleAnalyze = async (resumeId: string) => {
    setAnalyzingId(resumeId);
    toast.info("Analyzing your resume with AI...");
    try {
      await resumeService.analyzeResume(resumeId);
      toast.success("Analysis complete!");
      fetchAnalyses();
      navigate(`/dashboard/resumes/${resumeId}/analysis`);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Analysis failed. Please try again.");
    } finally {
      setAnalyzingId(null);
    }
  };

  const analysesByResume = new Map<string, ResumeAnalysis[]>();
  for (const a of analyses) {
    const list = analysesByResume.get(a.resumeId) ?? [];
    list.push(a);
    analysesByResume.set(a.resumeId, list);
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Resumes</h1>
          <p className="text-sm text-muted-foreground mt-1">Upload and manage your resumes. Get AI-powered analysis to improve them.</p>
        </div>
        <Button onClick={() => fileInputRef.current?.click()} disabled={uploading}>
          <Upload className="mr-2 size-4" />
          {uploading ? "Uploading..." : "Upload Resume"}
        </Button>
        <input ref={fileInputRef} type="file" accept=".pdf" className="hidden" onChange={handleUpload} />
      </div>

      {resumes === null ? (
        <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">Loading resumes...</div>
      ) : resumes.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary mb-3"><FileText className="size-6" /></div>
            <h3 className="font-semibold">No resumes yet</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Upload your resume in PDF format to attach it to applications and get AI analysis.</p>
            <Button onClick={() => fileInputRef.current?.click()}><Upload className="mr-2 size-4" /> Upload Your First Resume</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {resumes.map((resume) => {
            const resumeAnalyses = analysesByResume.get(resume.id) ?? [];
            const latestAnalysis = resumeAnalyses[0];
            const isAnalyzing = analyzingId === resume.id;
            return (
              <Card key={resume.id}>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary shrink-0"><FileText className="size-5" /></div>
                      <div className="min-w-0">
                        <p className="font-medium text-sm truncate">{resume.filename}</p>
                        <p className="text-xs text-muted-foreground">Uploaded {new Date(resume.createdAt).toLocaleDateString()}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {resume.isDefault ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700">
                          <CheckCircle className="size-3" /> Default
                        </span>
                      ) : (
                        <Button variant="ghost" size="sm" onClick={() => handleSetDefault(resume.id)}>
                          <Star className="mr-1 size-3" /> Set Default
                        </Button>
                      )}
                      <Button size="sm" variant="outline" disabled={isAnalyzing} onClick={() => handleAnalyze(resume.id)}>
                        {isAnalyzing ? <><Loader2 className="mr-1.5 size-3 animate-spin" /> Analyzing...</> : <><Sparkles className="mr-1.5 size-3" /> Analyze with AI</>}
                      </Button>
                      <Button variant="ghost" size="icon" className="size-8 text-destructive hover:text-destructive" onClick={() => handleDelete(resume.id)}>
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  </div>
                  {resumeAnalyses.length > 0 && (
                    <div className="mt-3 border-t pt-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <BarChart3 className="size-3.5 text-muted-foreground" />
                          <span className="text-xs text-muted-foreground">{resumeAnalyses.length} analysis{resumeAnalyses.length !== 1 ? "es" : ""}</span>
                          {latestAnalysis && <Badge variant="secondary" className="text-xs">Score: {latestAnalysis.overallScore}/100</Badge>}
                        </div>
                        <Button variant="ghost" size="sm" className="h-6 text-xs" onClick={() => navigate(`/dashboard/resumes/${resume.id}/analysis`)}>
                          View <ChevronRight className="ml-0.5 size-3" />
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
