import api, { unwrap } from "@/lib/api";

export interface Company {
  id: string;
  name: string;
  description: string | null;
  industry: string | null;
  location: string | null;
  website: string | null;
  logo: string | null;
  size: string | null;
  founded: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
}

export interface Job {
  id: string;
  title: string;
  description: string;
  location: string;
  type: string;
  remote: boolean;
  status: string;
  experienceMin: number | null;
  experienceMax: number | null;
  educationRequired: string | null;
  skills: string[];
  preferredSkills: string[];
  salaryMin: number | null;
  salaryMax: number | null;
  applicationDeadline: string | null;
  applicationCount: number;
  companyId: string;
  postedBy: string;
  company?: Company;
  createdAt: string;
  updatedAt: string;
}

export interface JobMatch {
  id: string;
  jobId: string;
  studentId: string;
  resumeId: string;
  matchScore: number;
  matchedSkills: string[];
  missingSkills: string[];
  strengths: string[];
  recommendations: string[];
  explanation: string | null;
  job?: Job;
  company?: Company;
  createdAt: string;
  updatedAt: string;
}

export interface SkillGap {
  skill: string;
  count: number;
}

export const companyService = {
  async getMyCompany(): Promise<Company | null> {
    try {
      const res = await api.get("/companies/me");
      return unwrap<Company>(res);
    } catch (e: any) {
      if (e?.response?.status === 404) return null;
      throw e;
    }
  },

  async createCompany(data: {
    name: string;
    description?: string;
    industry?: string;
    location?: string;
    website?: string;
    size?: string;
  }): Promise<Company> {
    const res = await api.post("/companies", data);
    return unwrap<Company>(res);
  },

  async updateCompany(id: string, data: {
    name?: string;
    description?: string;
    industry?: string;
    location?: string;
    website?: string;
    size?: string;
  }): Promise<Company> {
    const res = await api.put(`/companies/${id}`, data);
    return unwrap<Company>(res);
  },

  async getAllCompanies(): Promise<Company[]> {
    const res = await api.get("/companies");
    return unwrap<Company[]>(res);
  },

  async approveCompany(id: string): Promise<Company> {
    const res = await api.post(`/companies/${id}/approve`);
    return unwrap<Company>(res);
  },

  async rejectCompany(id: string): Promise<Company> {
    const res = await api.post(`/companies/${id}/reject`);
    return unwrap<Company>(res);
  },
};

export const jobService = {
  async browseJobs(params?: {
    search?: string;
    location?: string;
    type?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: Job[]; totalElements: number; totalPages: number }> {
    const queryParams: Record<string, any> = {};
    if (params?.search) queryParams.search = params.search;
    if (params?.location) queryParams.location = params.location;
    if (params?.type) queryParams.type = params.type;
    if (params?.status) queryParams.status = params.status;
    queryParams.page = params?.page ?? 0;
    queryParams.size = params?.size ?? 50;
    const res = await api.get("/jobs", { params: queryParams });
    return unwrap(res);
  },

  async getJob(id: string): Promise<Job> {
    const res = await api.get(`/jobs/${id}`);
    return unwrap<Job>(res);
  },

  async createJob(data: {
    title: string;
    description: string;
    location: string;
    type: string;
    remote?: boolean;
    experienceMin?: number;
    experienceMax?: number;
    educationRequired?: string;
    skills: string[];
    preferredSkills?: string[];
    salaryMin?: number;
    salaryMax?: number;
    applicationDeadline?: string;
  }): Promise<Job> {
    const res = await api.post("/jobs", data);
    return unwrap<Job>(res);
  },

  async updateJob(id: string, data: Record<string, any>): Promise<Job> {
    const res = await api.put(`/jobs/${id}`, data);
    return unwrap<Job>(res);
  },

  async closeJob(id: string): Promise<Job> {
    const res = await api.post(`/jobs/${id}/close`);
    return unwrap<Job>(res);
  },

  async getMyCompanyJobs(): Promise<Job[]> {
    const res = await api.get("/jobs", { params: { scope: "mine", size: 100 } });
    const data = unwrap<{ content: Job[] }>(res);
    return data.content ?? [];
  },

  // Job Matching
  async calculateMatch(jobId: string, resumeId?: string): Promise<JobMatch> {
    const params: Record<string, string> = {};
    if (resumeId) params.resumeId = resumeId;
    const res = await api.post(`/jobs/${jobId}/match`, null, { params });
    return unwrap<JobMatch>(res);
  },

  async getJobMatch(jobId: string): Promise<JobMatch | null> {
    try {
      const res = await api.get(`/jobs/${jobId}/match`);
      return unwrap<JobMatch>(res);
    } catch (e: any) {
      if (e?.response?.status === 404) return null;
      throw e;
    }
  },

  async getMyJobMatches(): Promise<JobMatch[]> {
    try {
      const res = await api.get("/job-matches/me", { params: { size: 100 } });
      const data = unwrap<{ content: JobMatch[] }>(res);
      return data.content ?? [];
    } catch {
      return [];
    }
  },

  async getMySkillGaps(): Promise<SkillGap[]> {
    try {
      const res = await api.get("/job-matches/me/skill-gaps");
      return unwrap<SkillGap[]>(res);
    } catch {
      return [];
    }
  },
};
