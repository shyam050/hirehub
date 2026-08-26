import { useState } from "react";
import { jobService } from "@/services/jobService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Save, ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router";
import { toast } from "sonner";

export default function JobCreatePage() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [type, setType] = useState("FULL_TIME");
  const [remote, setRemote] = useState(false);
  const [experienceMin, setExperienceMin] = useState("");
  const [experienceMax, setExperienceMax] = useState("");
  const [educationRequired, setEducationRequired] = useState("");
  const [salaryMin, setSalaryMin] = useState("");
  const [salaryMax, setSalaryMax] = useState("");
  const [deadline, setDeadline] = useState("");
  const [skills, setSkills] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState("");
  const [preferredSkills, setPreferredSkills] = useState<string[]>([]);
  const [preferredSkillInput, setPreferredSkillInput] = useState("");

  const handleAddSkill = (target: "required" | "preferred") => {
    const input = target === "required" ? skillInput : preferredSkillInput;
    const setter = target === "required" ? setSkills : setPreferredSkills;
    const list = target === "required" ? skills : preferredSkills;
    if (input.trim() && !list.includes(input.trim())) {
      setter([...list, input.trim()]);
      target === "required" ? setSkillInput("") : setPreferredSkillInput("");
    }
  };

  const handleSave = async () => {
    if (!title.trim() || !description.trim() || !location.trim()) {
      toast.error("Title, description, and location are required");
      return;
    }
    setSaving(true);
    try {
      await jobService.createJob({
        title,
        description,
        location,
        type,
        remote,
        experienceMin: experienceMin ? parseInt(experienceMin) : undefined,
        experienceMax: experienceMax ? parseInt(experienceMax) : undefined,
        educationRequired: educationRequired || undefined,
        skills,
        preferredSkills,
        salaryMin: salaryMin ? parseInt(salaryMin) : undefined,
        salaryMax: salaryMax ? parseInt(salaryMax) : undefined,
        applicationDeadline: deadline || undefined,
      });
      toast.success("Position published!");
      navigate("/dashboard/jobs-manage");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to create position");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard/jobs-manage")}><ArrowLeft className="size-4" /></Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Post Position</h1>
          <p className="text-sm text-muted-foreground mt-1">Create a new job listing.</p>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle className="text-base">Position Details</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2"><Label>Title *</Label><Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Software Engineer" /></div>
          <div className="space-y-2"><Label>Description *</Label><Textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Describe the role, responsibilities, and requirements..." rows={5} /></div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2"><Label>Location *</Label><Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="San Francisco, CA" /></div>
            <div className="space-y-2"><Label>Job Type</Label>
              <Select value={type} onValueChange={setType}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="FULL_TIME">Full Time</SelectItem>
                  <SelectItem value="PART_TIME">Part Time</SelectItem>
                  <SelectItem value="INTERNSHIP">Internship</SelectItem>
                  <SelectItem value="CONTRACT">Contract</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2"><Label>Min Experience (years)</Label><Input type="number" value={experienceMin} onChange={(e) => setExperienceMin(e.target.value)} placeholder="0" /></div>
            <div className="space-y-2"><Label>Max Experience (years)</Label><Input type="number" value={experienceMax} onChange={(e) => setExperienceMax(e.target.value)} placeholder="5" /></div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2"><Label>Min Salary</Label><Input type="number" value={salaryMin} onChange={(e) => setSalaryMin(e.target.value)} placeholder="50000" /></div>
            <div className="space-y-2"><Label>Max Salary</Label><Input type="number" value={salaryMax} onChange={(e) => setSalaryMax(e.target.value)} placeholder="120000" /></div>
          </div>
          <div className="space-y-2"><Label>Application Deadline</Label><Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} /></div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-base">Required Skills</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="flex gap-2">
            <Input value={skillInput} onChange={(e) => setSkillInput(e.target.value)} placeholder="Add required skill" onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddSkill("required"); } }} />
            <Button variant="outline" onClick={() => handleAddSkill("required")}>Add</Button>
          </div>
          {skills.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {skills.map((skill) => (
                <span key={skill} className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-3 py-1 text-sm text-primary">
                  {skill}
                  <button onClick={() => setSkills(skills.filter((s) => s !== skill))} className="ml-0.5 hover:text-destructive">×</button>
                </span>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-base">Preferred Skills</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="flex gap-2">
            <Input value={preferredSkillInput} onChange={(e) => setPreferredSkillInput(e.target.value)} placeholder="Add preferred skill" onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddSkill("preferred"); } }} />
            <Button variant="outline" onClick={() => handleAddSkill("preferred")}>Add</Button>
          </div>
          {preferredSkills.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {preferredSkills.map((skill) => (
                <span key={skill} className="inline-flex items-center gap-1 rounded-full bg-muted px-3 py-1 text-sm">
                  {skill}
                  <button onClick={() => setPreferredSkills(preferredSkills.filter((s) => s !== skill))} className="ml-0.5 hover:text-destructive">×</button>
                </span>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex justify-end gap-3">
        <Button variant="outline" onClick={() => navigate("/dashboard/jobs-manage")}>Cancel</Button>
        <Button onClick={handleSave} disabled={saving}>
          {saving ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Save className="mr-2 size-4" />}
          Publish
        </Button>
      </div>
    </div>
  );
}
