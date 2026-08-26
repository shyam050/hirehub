import api, { uploadApi, unwrap } from "@/lib/api";

export interface Resume {
  id: string;
  studentId: string;
  filename: string;
  fileSize: number | null;
  contentType: string | null;
  extractedText: string | null;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeAnalysis {
  id: string;
  studentId: string;
  resumeId: string;
  overallScore: number;
  extractedSkills: string[];
  extractedEducation: string[];
  extractedProjects: string[];
  extractedExperience: string[];
  extractedCertifications: string[];
  extractedAchievements: string[];
  strengths: string[];
  weaknesses: string[];
  missingSkills: string[];
  recommendations: string[];
  createdAt: string;
  updatedAt: string;
}

export const resumeService = {
  async getMyResumes(): Promise<Resume[]> {
    const res = await api.get("/resumes");
    return unwrap<Resume[]>(res);
  },

  async getResume(id: string): Promise<Resume> {
    const res = await api.get(`/resumes/${id}`);
    return unwrap<Resume>(res);
  },

  async uploadResume(file: File): Promise<Resume> {
    const formData = new FormData();
    formData.append("file", file);
    const res = await uploadApi.post("/resumes", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return unwrap<Resume>(res);
  },

  async deleteResume(id: string): Promise<void> {
    await api.delete(`/resumes/${id}`);
  },

  async setDefaultResume(id: string): Promise<Resume> {
    const res = await api.post(`/resumes/${id}/default`);
    return unwrap<Resume>(res);
  },

  async downloadResume(id: string): Promise<Blob> {
    const token = localStorage.getItem("hirehub_access_token");
    const res = await fetch(
      `${import.meta.env.VITE_API_URL || "/api/v1"}/resumes/${id}/download`,
      {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      }
    );
    if (!res.ok) throw new Error("Download failed");
    return res.blob();
  },

  // AI Resume Analysis
  async analyzeResume(resumeId: string): Promise<ResumeAnalysis> {
    const res = await api.post(`/resumes/${resumeId}/analyze`);
    return unwrap<ResumeAnalysis>(res);
  },

  async getAnalysesForResume(resumeId: string): Promise<ResumeAnalysis[]> {
    const res = await api.get(`/resumes/${resumeId}/analyses`);
    return unwrap<ResumeAnalysis[]>(res);
  },

  async getLatestAnalysis(resumeId: string): Promise<ResumeAnalysis | null> {
    try {
      const res = await api.get(`/resumes/${resumeId}/analyses/latest`);
      return unwrap<ResumeAnalysis>(res);
    } catch {
      return null;
    }
  },

  async getMyAnalyses(): Promise<ResumeAnalysis[]> {
    try {
      const res = await api.get("/resume-analyses/me");
      return unwrap<ResumeAnalysis[]>(res);
    } catch {
      return [];
    }
  },
};
