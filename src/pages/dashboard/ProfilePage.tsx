import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/use-auth";
import { userService, type StudentProfile, type RecruiterProfile } from "@/services/userService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Loader2, Save, Plus, Trash2 } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router";
import { toast } from "sonner";

interface Education {
  institution: string;
  degree: string;
  fieldOfStudy: string;
  startYear: number;
  endYear: number;
  gpa?: string;
}

interface Project {
  name: string;
  description: string;
  technologies: string[];
  link?: string;
}

export default function ProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isSetup = searchParams.get("setup") === "true";

  const [studentProfile, setStudentProfile] = useState<StudentProfile | null>(null);
  const [recruiterProfile, setRecruiterProfile] = useState<RecruiterProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const isStudent = user?.role?.toUpperCase() === "STUDENT";

  useEffect(() => {
    async function load() {
      try {
        if (isStudent) {
          const profile = await userService.getStudentProfile();
          setStudentProfile(profile);
        } else {
          const profile = await userService.getRecruiterProfile();
          setRecruiterProfile(profile);
        }
      } catch {
        // Profile may not exist yet for new users
      } finally {
        setLoading(false);
      }
    }
    if (user) load();
  }, [user, isStudent]);

  // Student form state
  const [name, setName] = useState(user?.name ?? "");
  const [phone, setPhone] = useState("");
  const [university, setUniversity] = useState("");
  const [degree, setDegree] = useState("");
  const [fieldOfStudy, setFieldOfStudy] = useState("");
  const [graduationYear, setGraduationYear] = useState("");
  const [gpa, setGpa] = useState("");
  const [bio, setBio] = useState("");
  const [location, setLocation] = useState("");
  const [linkedin, setLinkedin] = useState("");
  const [github, setGithub] = useState("");
  const [portfolio, setPortfolio] = useState("");
  const [skills, setSkills] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState("");
  const [education, setEducation] = useState<Education[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);

  // Recruiter form state
  const [jobTitle, setJobTitle] = useState("");
  const [recruiterPhone, setRecruiterPhone] = useState("");
  const [recruiterBio, setRecruiterBio] = useState("");

  // Populate form from profile once loaded
  useEffect(() => {
    if (studentProfile) {
      setName(user?.name ?? "");
      setPhone(studentProfile.phone ?? "");
      setUniversity(studentProfile.university ?? "");
      setDegree(studentProfile.degree ?? "");
      setFieldOfStudy(studentProfile.fieldOfStudy ?? "");
      setGraduationYear(studentProfile.graduationYear?.toString() ?? "");
      setGpa(studentProfile.gpa ?? "");
      setBio(studentProfile.bio ?? "");
      setLocation(studentProfile.location ?? "");
      setLinkedin(studentProfile.linkedin ?? "");
      setGithub(studentProfile.github ?? "");
      setPortfolio(studentProfile.portfolio ?? "");
      setSkills(studentProfile.skills ?? []);
      setEducation(studentProfile.education ?? []);
      setProjects(studentProfile.projects ?? []);
    }
    if (recruiterProfile) {
      setJobTitle(recruiterProfile.jobTitle ?? "");
      setRecruiterPhone(recruiterProfile.phone ?? "");
      setRecruiterBio(recruiterProfile.bio ?? "");
      setName(user?.name ?? "");
    }
  }, [studentProfile, recruiterProfile, user]);

  const handleAddSkill = () => {
    if (skillInput.trim() && !skills.includes(skillInput.trim())) {
      setSkills([...skills, skillInput.trim()]);
      setSkillInput("");
    }
  };

  const handleRemoveSkill = (skill: string) => {
    setSkills(skills.filter((s) => s !== skill));
  };

  const handleAddEducation = () => {
    setEducation([
      ...education,
      {
        institution: "",
        degree: "",
        fieldOfStudy: "",
        startYear: new Date().getFullYear(),
        endYear: new Date().getFullYear() + 4,
      },
    ]);
  };

  const handleRemoveEducation = (index: number) => {
    setEducation(education.filter((_, i) => i !== index));
  };

  const handleUpdateEducation = (
    index: number,
    field: keyof Education,
    value: string | number
  ) => {
    const updated = [...education];
    (updated[index] as any)[field] = value;
    setEducation(updated);
  };

  const handleAddProject = () => {
    setProjects([...projects, { name: "", description: "", technologies: [] }]);
  };

  const handleRemoveProject = (index: number) => {
    setProjects(projects.filter((_, i) => i !== index));
  };

  const handleUpdateProject = (
    index: number,
    field: keyof Project,
    value: string | string[]
  ) => {
    const updated = [...projects];
    (updated[index] as any)[field] = value;
    setProjects(updated);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await userService.updateMe({ name });

      if (isStudent) {
        await userService.updateStudentProfile({
          phone,
          university,
          degree,
          fieldOfStudy,
          graduationYear: graduationYear ? parseInt(graduationYear) : undefined,
          gpa,
          bio,
          location,
          linkedin,
          github,
          portfolio,
          skills,
          education,
          projects,
        });
      } else {
        await userService.updateRecruiterProfile({
          jobTitle,
          phone: recruiterPhone,
          bio: recruiterBio,
        });
      }

      toast.success("Profile saved successfully!");
      if (isSetup) {
        if (isStudent) {
          navigate("/dashboard/resumes");
        } else {
          navigate("/dashboard/company");
        }
      }
    } catch (error) {
      console.error("Save error:", error);
      toast.error(
        error instanceof Error ? error.message : "Failed to save profile"
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">
          {isSetup ? "Complete Your Profile" : "Profile"}
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          {isSetup
            ? "Fill in your details to complete your profile and start applying."
            : "Update your personal information and preferences."}
        </p>
      </div>

      {/* Basic Info */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Personal Information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Full Name</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Your full name" />
            </div>
            <div className="space-y-2">
              <Label>Email</Label>
              <Input value={user?.email ?? ""} disabled />
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Phone</Label>
              <Input
                value={isStudent ? phone : recruiterPhone}
                onChange={(e) => isStudent ? setPhone(e.target.value) : setRecruiterPhone(e.target.value)}
                placeholder="+1 (555) 000-0000"
              />
            </div>
            <div className="space-y-2">
              <Label>Location</Label>
              <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="City, Country" disabled={!isStudent} />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Bio</Label>
            <Textarea
              value={isStudent ? bio : recruiterBio}
              onChange={(e) => isStudent ? setBio(e.target.value) : setRecruiterBio(e.target.value)}
              placeholder="Tell us about yourself..."
              rows={3}
            />
          </div>
        </CardContent>
      </Card>

      {/* Student-specific sections */}
      {isStudent && (
        <>
          {/* Education */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-base">Education</CardTitle>
              <Button variant="outline" size="sm" onClick={handleAddEducation}>
                <Plus className="mr-1 size-3" /> Add
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              {education.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No education entries yet.</p>
              ) : (
                education.map((edu, index) => (
                  <div key={index} className="rounded-lg border p-4 space-y-3">
                    <div className="flex justify-between items-start">
                      <p className="text-sm font-medium">Education #{index + 1}</p>
                      <Button variant="ghost" size="icon" className="size-7 text-destructive" onClick={() => handleRemoveEducation(index)}>
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="space-y-1.5">
                        <Label className="text-xs">Institution</Label>
                        <Input value={edu.institution} onChange={(e) => handleUpdateEducation(index, "institution", e.target.value)} placeholder="University name" />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">Degree</Label>
                        <Input value={edu.degree} onChange={(e) => handleUpdateEducation(index, "degree", e.target.value)} placeholder="B.S., M.S., etc." />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">Field of Study</Label>
                        <Input value={edu.fieldOfStudy} onChange={(e) => handleUpdateEducation(index, "fieldOfStudy", e.target.value)} placeholder="Computer Science" />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">GPA</Label>
                        <Input value={edu.gpa ?? ""} onChange={(e) => handleUpdateEducation(index, "gpa", e.target.value)} placeholder="3.8/4.0" />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">Start Year</Label>
                        <Input type="number" value={edu.startYear} onChange={(e) => handleUpdateEducation(index, "startYear", parseInt(e.target.value))} />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">End Year</Label>
                        <Input type="number" value={edu.endYear} onChange={(e) => handleUpdateEducation(index, "endYear", parseInt(e.target.value))} />
                      </div>
                    </div>
                  </div>
                ))
              )}
            </CardContent>
          </Card>

          {/* Skills */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Skills</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex gap-2">
                <Input
                  value={skillInput}
                  onChange={(e) => setSkillInput(e.target.value)}
                  placeholder="Add a skill (e.g., React, Python)"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") { e.preventDefault(); handleAddSkill(); }
                  }}
                />
                <Button variant="outline" onClick={handleAddSkill}>Add</Button>
              </div>
              {skills.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {skills.map((skill) => (
                    <span key={skill} className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-3 py-1 text-sm text-primary">
                      {skill}
                      <button onClick={() => handleRemoveSkill(skill)} className="ml-0.5 hover:text-destructive">×</button>
                    </span>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Projects */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-base">Projects</CardTitle>
              <Button variant="outline" size="sm" onClick={handleAddProject}>
                <Plus className="mr-1 size-3" /> Add
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              {projects.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No projects yet.</p>
              ) : (
                projects.map((project, index) => (
                  <div key={index} className="rounded-lg border p-4 space-y-3">
                    <div className="flex justify-between items-start">
                      <p className="text-sm font-medium">Project #{index + 1}</p>
                      <Button variant="ghost" size="icon" className="size-7 text-destructive" onClick={() => handleRemoveProject(index)}>
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="space-y-1.5">
                        <Label className="text-xs">Name</Label>
                        <Input value={project.name} onChange={(e) => handleUpdateProject(index, "name", e.target.value)} placeholder="Project name" />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">Technologies</Label>
                        <Input
                          value={project.technologies.join(", ")}
                          onChange={(e) => handleUpdateProject(index, "technologies", e.target.value.split(",").map((t) => t.trim()).filter(Boolean))}
                          placeholder="React, TypeScript, etc."
                        />
                      </div>
                      <div className="space-y-1.5 sm:col-span-2">
                        <Label className="text-xs">Description</Label>
                        <Textarea value={project.description} onChange={(e) => handleUpdateProject(index, "description", e.target.value)} placeholder="Brief description" rows={2} />
                      </div>
                      <div className="space-y-1.5 sm:col-span-2">
                        <Label className="text-xs">Link (optional)</Label>
                        <Input value={project.link ?? ""} onChange={(e) => handleUpdateProject(index, "link", e.target.value)} placeholder="https://github.com/..." />
                      </div>
                    </div>
                  </div>
                ))
              )}
            </CardContent>
          </Card>

          {/* Social Links */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Links</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>LinkedIn</Label>
                <Input value={linkedin} onChange={(e) => setLinkedin(e.target.value)} placeholder="https://linkedin.com/in/..." />
              </div>
              <div className="space-y-2">
                <Label>GitHub</Label>
                <Input value={github} onChange={(e) => setGithub(e.target.value)} placeholder="https://github.com/..." />
              </div>
              <div className="space-y-2">
                <Label>Portfolio</Label>
                <Input value={portfolio} onChange={(e) => setPortfolio(e.target.value)} placeholder="https://..." />
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {/* Recruiter-specific */}
      {!isStudent && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Recruiter Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Job Title</Label>
              <Input value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} placeholder="HR Manager, Talent Acquisition..." />
            </div>
          </CardContent>
        </Card>
      )}

      {/* Save */}
      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={saving}>
          {saving ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Save className="mr-2 size-4" />}
          {isSetup ? "Save & Continue" : "Save Profile"}
        </Button>
      </div>
    </div>
  );
}
