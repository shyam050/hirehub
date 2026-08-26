import api, { unwrap } from "@/lib/api";

export interface Interview {
  id: string;
  applicationId: string;
  studentId: string;
  companyId: string;
  jobId: string;
  interviewType: string;
  scheduledAt: string;
  duration: number;
  meetingLink: string | null;
  interviewerName: string | null;
  status: string;
  notes: string | null;
  feedback: string | null;
  job?: {
    id: string;
    title: string;
  };
  company?: {
    id: string;
    name: string;
  };
  student?: {
    id: string;
    userId: string;
  };
  studentUser?: {
    id: string;
    name: string | null;
    email: string;
  };
  createdAt: string;
  updatedAt: string;
}

export const interviewService = {
  async scheduleInterview(data: {
    applicationId: string;
    interviewType: string;
    scheduledAt: string;
    duration: number;
    meetingLink?: string;
    interviewerName?: string;
    notes?: string;
  }): Promise<Interview> {
    const res = await api.post("/interviews", data);
    return unwrap<Interview>(res);
  },

  async getMyStudentInterviews(): Promise<Interview[]> {
    const res = await api.get("/interviews/me");
    return unwrap<Interview[]>(res);
  },

  async getRecruiterInterviews(): Promise<Interview[]> {
    const res = await api.get("/interviews/recruiter", { params: { size: 100 } });
    const data = unwrap<{ content: Interview[] }>(res);
    return data.content ?? [];
  },

  async getInterview(id: string): Promise<Interview> {
    const res = await api.get(`/interviews/${id}`);
    return unwrap<Interview>(res);
  },

  async rescheduleInterview(
    id: string,
    data: { scheduledAt: string; duration?: number; meetingLink?: string }
  ): Promise<Interview> {
    const res = await api.patch(`/interviews/${id}/reschedule`, data);
    return unwrap<Interview>(res);
  },

  async cancelInterview(id: string): Promise<Interview> {
    const res = await api.post(`/interviews/${id}/cancel`);
    return unwrap<Interview>(res);
  },

  async completeInterview(id: string): Promise<Interview> {
    const res = await api.post(`/interviews/${id}/complete`);
    return unwrap<Interview>(res);
  },

  async submitFeedback(id: string, data: { feedback: string; notes?: string }): Promise<Interview> {
    const res = await api.post(`/interviews/${id}/feedback`, data);
    return unwrap<Interview>(res);
  },

  async getApplicationInterviews(applicationId: string): Promise<Interview[]> {
    const res = await api.get(`/interviews/application/${applicationId}`);
    return unwrap<Interview[]>(res);
  },
};
