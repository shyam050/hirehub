import api, { unwrap } from "@/lib/api";

export interface AiInterview {
  id: string;
  studentId: string;
  jobId: string;
  resumeId: string;
  interviewType: string;
  difficulty: string;
  totalQuestions: number;
  currentQuestionNumber: number;
  status: string;
  overallScore: number | null;
  technicalScore: number | null;
  communicationScore: number | null;
  problemSolvingScore: number | null;
  strengths: string[] | null;
  weaknesses: string[] | null;
  missingConcepts: string[] | null;
  recommendedTopics: string[] | null;
  overallFeedback: string | null;
  job?: { id: string; title: string; company?: { name: string } };
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}

export interface AiInterviewQuestion {
  id: string;
  interviewId: string;
  questionNumber: number;
  question: string;
  category: string;
  expectedTopics: string[] | null;
  studentAnswer: string | null;
  score: number | null;
  strengths: string[] | null;
  weaknesses: string[] | null;
  feedback: string | null;
  missingConcepts: string[] | null;
  idealAnswerPoints: string[] | null;
}

export const aiService = {
  // AI Mock Interviews
  async startInterview(data: {
    jobId: string;
    resumeId: string;
    interviewType: string;
    difficulty?: string;
    totalQuestions?: number;
  }): Promise<{ interview: AiInterview; question: AiInterviewQuestion }> {
    const res = await api.post("/ai-interviews", data);
    return unwrap(res);
  },

  async getMyInterviews(): Promise<AiInterview[]> {
    const res = await api.get("/ai-interviews/me");
    return unwrap<AiInterview[]>(res);
  },

  async getInterview(id: string): Promise<AiInterview> {
    const res = await api.get(`/ai-interviews/${id}`);
    return unwrap<AiInterview>(res);
  },

  async getInterviewQuestions(id: string): Promise<AiInterviewQuestion[]> {
    const res = await api.get(`/ai-interviews/${id}/questions`);
    return unwrap<AiInterviewQuestion[]>(res);
  },

  async getInterviewReport(id: string): Promise<AiInterview> {
    const res = await api.get(`/ai-interviews/${id}/report`);
    return unwrap<AiInterview>(res);
  },

  async generateNextQuestion(id: string): Promise<AiInterviewQuestion> {
    const res = await api.post(`/ai-interviews/${id}/next-question`);
    return unwrap<AiInterviewQuestion>(res);
  },

  async submitAnswer(
    id: string,
    questionNumber: number,
    studentAnswer: string
  ): Promise<{
    score: number;
    strengths: string[];
    weaknesses: string[];
    feedback: string;
    missingConcepts: string[];
  }> {
    const res = await api.post(
      `/ai-interviews/${id}/questions/${questionNumber}/answer`,
      { studentAnswer }
    );
    return unwrap(res);
  },

  async completeInterview(id: string): Promise<AiInterview> {
    const res = await api.post(`/ai-interviews/${id}/complete`);
    return unwrap<AiInterview>(res);
  },

  async abandonInterview(id: string): Promise<void> {
    await api.post(`/ai-interviews/${id}/abandon`);
  },

  // Job matching helpers (using resumeService for the actual calls)
  async getMyDefaultResumeId(): Promise<string | null> {
    try {
      const res = await api.get("/resumes");
      const resumes = unwrap<any[]>(res);
      const defaultResume = resumes.find((r: any) => r.isDefault);
      return defaultResume?.id ?? resumes[0]?.id ?? null;
    } catch {
      return null;
    }
  },
};
