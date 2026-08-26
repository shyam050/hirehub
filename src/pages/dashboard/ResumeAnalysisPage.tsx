import { useState, useEffect } from "react";
import { resumeService, type ResumeAnalysis } from "@/services/resumeService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ArrowLeft, TrendingUp, AlertTriangle, Target, Lightbulb, FileText, Calendar, GraduationCap, Briefcase, Award, FolderGit2, Wrench, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router";

export default function ResumeAnalysisPage() {
  const { id } = useParams<{ id: string }>();
  const [analyses, setAnalyses] = useState<ResumeAnalysis[] | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    resumeService.getAnalysesForResume(id)
      .then(setAnalyses)
      .catch(() => setAnalyses([]));
  }, [id]);

  if (!id) return null;
  const latest = analyses?.[0];

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/resumes")}><ArrowLeft className="size-4" /></Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold tracking-tight">Resume Analysis</h1>
          <p className="text-sm text-muted-foreground mt-1">{analyses?.length ?? 0} analysis{(analyses?.length ?? 0) !== 1 ? "es" : ""}</p>
        </div>
      </div>

      {analyses === null ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : analyses.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <FileText className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="font-semibold">No analysis yet</h3>
            <p className="text-sm text-muted-foreground mt-1 mb-4">Go back and click "Analyze with AI" to generate your first analysis.</p>
            <Button onClick={() => navigate("/dashboard/resumes")}>Back to Resumes</Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center gap-6">
                <div className="relative size-24">
                  <svg className="size-24 -rotate-90" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" className="text-muted/30" />
                    <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" strokeWidth="8" strokeLinecap="round" strokeDasharray={`${(latest!.overallScore / 100) * 264} 264`} className={latest!.overallScore >= 70 ? "text-green-500" : latest!.overallScore >= 40 ? "text-yellow-500" : "text-red-500"} />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center"><span className="text-2xl font-bold tabular-nums">{latest!.overallScore}</span></div>
                </div>
                <div>
                  <h2 className="text-lg font-semibold">Overall Score</h2>
                  <p className="text-sm text-muted-foreground mt-1">
                    {latest!.overallScore >= 80 ? "Excellent resume. Strong across all areas." : latest!.overallScore >= 60 ? "Good resume with room for improvement." : latest!.overallScore >= 40 ? "Fair resume. Several areas need work." : "Needs significant improvement."}
                  </p>
                  <p className="text-xs text-muted-foreground mt-2">Analyzed {new Date(latest!.createdAt).toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" })}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="grid gap-4 sm:grid-cols-2">
            <ExtractedSection icon={Wrench} title="Skills" items={latest!.extractedSkills} />
            <ExtractedSection icon={GraduationCap} title="Education" items={latest!.extractedEducation} />
            <ExtractedSection icon={FolderGit2} title="Projects" items={latest!.extractedProjects} />
            <ExtractedSection icon={Briefcase} title="Experience" items={latest!.extractedExperience} />
            <ExtractedSection icon={Award} title="Certifications" items={latest!.extractedCertifications} />
            <ExtractedSection icon={TrendingUp} title="Achievements" items={latest!.extractedAchievements} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><TrendingUp className="size-4 text-green-600" /> Strengths</CardTitle></CardHeader>
              <CardContent><ul className="space-y-2">{latest!.strengths.map((item, i) => <li key={i} className="text-sm flex items-start gap-2"><span className="mt-1.5 size-1.5 rounded-full bg-green-500 shrink-0" /><span>{item}</span></li>)}</ul></CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><AlertTriangle className="size-4 text-amber-600" /> Weaknesses</CardTitle></CardHeader>
              <CardContent><ul className="space-y-2">{latest!.weaknesses.map((item, i) => <li key={i} className="text-sm flex items-start gap-2"><span className="mt-1.5 size-1.5 rounded-full bg-amber-500 shrink-0" /><span>{item}</span></li>)}</ul></CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Target className="size-4 text-blue-600" /> Missing Skills</CardTitle></CardHeader>
            <CardContent>
              {latest!.missingSkills.length > 0 ? <div className="flex flex-wrap gap-2">{latest!.missingSkills.map((skill, i) => <Badge key={i} variant="outline" className="text-xs">{skill}</Badge>)}</div> : <p className="text-sm text-muted-foreground">No notable missing skills detected.</p>}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Lightbulb className="size-4 text-primary" /> Recommendations</CardTitle></CardHeader>
            <CardContent>
              <ol className="space-y-3">{latest!.recommendations.map((item, i) => <li key={i} className="flex items-start gap-3 text-sm"><span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary text-xs font-semibold">{i + 1}</span><span className="pt-0.5">{item}</span></li>)}</ol>
            </CardContent>
          </Card>

          {analyses.length > 1 && (
            <Card>
              <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Calendar className="size-4 text-muted-foreground" /> Analysis History</CardTitle></CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {analyses.map((a) => (
                    <div key={a.id} className="flex items-center justify-between rounded-lg border p-3 text-sm">
                      <div className="flex items-center gap-3">
                        <div className={`size-8 rounded-full flex items-center justify-center text-xs font-bold ${a.overallScore >= 70 ? "bg-green-100 text-green-700" : a.overallScore >= 40 ? "bg-yellow-100 text-yellow-700" : "bg-red-100 text-red-700"}`}>{a.overallScore}</div>
                        <div>
                          <p className="text-xs text-muted-foreground">{new Date(a.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" })}</p>
                          <p className="text-xs text-muted-foreground">{a.strengths.length} strengths · {a.weaknesses.length} weaknesses · {a.recommendations.length} recommendations</p>
                        </div>
                      </div>
                      <Badge variant="secondary">{a.overallScore}/100</Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

function ExtractedSection({ icon: Icon, title, items }: { icon: React.ElementType; title: string; items: string[] }) {
  return (
    <Card>
      <CardHeader className="pb-3"><CardTitle className="text-sm font-medium flex items-center gap-2"><Icon className="size-4 text-muted-foreground" /> {title}</CardTitle></CardHeader>
      <CardContent>
        {items.length > 0 ? <div className="flex flex-wrap gap-1.5">{items.map((item, i) => <Badge key={i} variant="secondary" className="text-xs">{item}</Badge>)}</div> : <p className="text-sm text-muted-foreground">None detected.</p>}
      </CardContent>
    </Card>
  );
}
