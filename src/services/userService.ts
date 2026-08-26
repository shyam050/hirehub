import api, { unwrap } from "@/lib/api";

export interface UserProfile {
  id: string;
  email: string;
  name: string | null;
  image: string | null;
  role: string;
  createdAt: string;
}

export interface StudentProfile {
  id: string;
  userId: string;
  phone: string | null;
  location: string | null;
  headline: string | null;
  bio: string | null;
  skills: string[];
  education: any[];
  projects: any[];
  experience: any[];
  certifications: any[];
  achievements: any[];
  github: string | null;
  linkedin: string | null;
  portfolio: string | null;
  university: string | null;
  degree: string | null;
  fieldOfStudy: string | null;
  graduationYear: number | null;
  gpa: string | null;
}

export interface RecruiterProfile {
  id: string;
  userId: string;
  phone: string | null;
  bio: string | null;
  jobTitle: string | null;
  companyId: string | null;
}

export interface UpdateUserRequest {
  name?: string;
  phone?: string;
  image?: string;
}

export interface UpdateStudentProfileRequest {
  phone?: string;
  location?: string;
  headline?: string;
  bio?: string;
  skills?: string[];
  education?: any[];
  projects?: any[];
  experience?: any[];
  certifications?: any[];
  achievements?: any[];
  github?: string | null;
  linkedin?: string | null;
  portfolio?: string | null;
  university?: string;
  degree?: string;
  fieldOfStudy?: string;
  graduationYear?: number;
  gpa?: string;
}

export interface UpdateRecruiterProfileRequest {
  phone?: string;
  bio?: string;
  jobTitle?: string;
}

export const userService = {
  async getMe(): Promise<UserProfile> {
    const res = await api.get("/users/me");
    return unwrap<UserProfile>(res);
  },

  async updateMe(data: UpdateUserRequest): Promise<UserProfile> {
    const res = await api.patch("/users/me", data);
    return unwrap<UserProfile>(res);
  },

  // Student profile
  async getStudentProfile(): Promise<StudentProfile> {
    const res = await api.get("/students/me");
    return unwrap<StudentProfile>(res);
  },

  async updateStudentProfile(data: UpdateStudentProfileRequest): Promise<StudentProfile> {
    const res = await api.put("/students/me", data);
    return unwrap<StudentProfile>(res);
  },

  // Recruiter profile
  async getRecruiterProfile(): Promise<RecruiterProfile> {
    const res = await api.get("/recruiters/me");
    return unwrap<RecruiterProfile>(res);
  },

  async updateRecruiterProfile(data: UpdateRecruiterProfileRequest): Promise<RecruiterProfile> {
    const res = await api.put("/recruiters/me", data);
    return unwrap<RecruiterProfile>(res);
  },

  // Admin endpoints
  async getAllStudents(): Promise<UserProfile[]> {
    const res = await api.get("/users/students");
    return unwrap<UserProfile[]>(res);
  },

  async getAllRecruiters(): Promise<UserProfile[]> {
    const res = await api.get("/users/recruiters");
    return unwrap<UserProfile[]>(res);
  },

  async getAdminStats(): Promise<{ totalStudents: number; totalRecruiters: number; totalCompanies: number; totalJobs: number; totalApplications: number; pendingCompanies: number }> {
    const res = await api.get("/users/stats");
    return unwrap(res);
  },
};
