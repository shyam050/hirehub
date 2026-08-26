import api, { unwrap } from "@/lib/api";

export interface Application {
  id: string;
  jobId: string;
  studentId: string;
  stage: string;
  status: string;
  coverLetter: string | null;
  timeline: Array<{
    stage: string;
    timestamp: string;
    note?: string;
  }>;
  job?: {
    id: string;
    title: string;
    location: string;
    type: string;
    skills: string[];
  };
  company?: {
    id: string;
    name: string;
  };
  student?: {
    id: string;
    userId: string;
    skills: string[];
    university: string | null;
    degree: string | null;
  };
  studentUser?: {
    id: string;
    email: string;
    name: string | null;
  };
  createdAt: string;
  updatedAt: string;
}

export const applicationService = {
  async applyToJob(jobId: string, coverLetter?: string): Promise<Application> {
    const res = await api.post(`/jobs/${jobId}/applications`, { coverLetter });
    return unwrap<Application>(res);
  },

  async getMyApplications(): Promise<Application[]> {
    const res = await api.get("/applications/me", { params: { size: 100 } });
    const data = unwrap<{ content: Application[] }>(res);
    return data.content ?? [];
  },

  async getApplication(id: string): Promise<Application> {
    const res = await api.get(`/applications/${id}`);
    return unwrap<Application>(res);
  },

  async getJobApplicants(jobId: string): Promise<Application[]> {
    const res = await api.get(`/jobs/${jobId}/applications`, { params: { size: 100 } });
    const data = unwrap<{ content: Application[] }>(res);
    return data.content ?? [];
  },

  async updateApplicationStatus(
    applicationId: string,
    stage: string
  ): Promise<Application> {
    const res = await api.patch(`/applications/${applicationId}/status`, { stage });
    return unwrap<Application>(res);
  },

  async getAllApplications(): Promise<Application[]> {
    const res = await api.get("/applications", { params: { size: 100 } });
    const data = unwrap<{ content: Application[] }>(res);
    return data.content ?? [];
  },
};
