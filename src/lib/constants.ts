export type UserRole = "student" | "recruiter" | "admin";

export type ApplicationStage =
  | "applied"
  | "screening"
  | "shortlisted"
  | "technical_interview"
  | "hr_interview"
  | "offered"
  | "selected"
  | "rejected";

export type JobType = "full_time" | "part_time" | "internship" | "contract";

export type JobStatus = "active" | "closed" | "draft";

export const STAGE_LABELS: Record<ApplicationStage, string> = {
  applied: "Applied",
  screening: "Screening",
  shortlisted: "Shortlisted",
  technical_interview: "Technical Interview",
  hr_interview: "HR Interview",
  offered: "Offered",
  selected: "Selected",
  rejected: "Rejected",
};

export const STAGE_LABELS_FIXED: Record<string, string> = {
  applied: "Applied",
  screening: "Screening",
  shortlisted: "Shortlisted",
  technical_interview: "Technical Interview",
  hr_interview: "HR Interview",
  offered: "Offered",
  selected: "Selected",
  rejected: "Rejected",
};

export const JOB_TYPE_LABELS: Record<JobType, string> = {
  full_time: "Full Time",
  part_time: "Part Time",
  internship: "Internship",
  contract: "Contract",
};

export const JOB_STATUS_LABELS: Record<JobStatus, string> = {
  active: "Active",
  closed: "Closed",
  draft: "Draft",
};

export const STAGE_COLORS: Record<string, string> = {
  applied: "bg-blue-100 text-blue-800",
  screening: "bg-yellow-100 text-yellow-800",
  shortlisted: "bg-green-100 text-green-800",
  technical_interview: "bg-purple-100 text-purple-800",
  hr_interview: "bg-indigo-100 text-indigo-800",
  offered: "bg-emerald-100 text-emerald-800",
  selected: "bg-green-100 text-green-800",
  rejected: "bg-red-100 text-red-800",
};

export const V1_STAGES: ApplicationStage[] = [
  "applied",
  "screening",
  "shortlisted",
  "rejected",
];

export const ALL_STAGES: ApplicationStage[] = [
  "applied",
  "screening",
  "shortlisted",
  "technical_interview",
  "hr_interview",
  "offered",
  "selected",
  "rejected",
];

// Interview Types
export type InterviewType = "online_test" | "technical" | "hr" | "managerial";

export const INTERVIEW_TYPE_LABELS: Record<InterviewType, string> = {
  online_test: "Online Test",
  technical: "Technical",
  hr: "HR",
  managerial: "Managerial",
};

export const INTERVIEW_TYPE_OPTIONS: { value: InterviewType; label: string }[] = [
  { value: "online_test", label: "Online Test" },
  { value: "technical", label: "Technical" },
  { value: "hr", label: "HR" },
  { value: "managerial", label: "Managerial" },
];

// Interview Statuses
export type InterviewStatus = "scheduled" | "completed" | "cancelled" | "rescheduled";

export const INTERVIEW_STATUS_LABELS: Record<InterviewStatus, string> = {
  scheduled: "Scheduled",
  completed: "Completed",
  cancelled: "Cancelled",
  rescheduled: "Rescheduled",
};

export const INTERVIEW_STATUS_COLORS: Record<string, string> = {
  scheduled: "bg-blue-100 text-blue-800",
  completed: "bg-green-100 text-green-800",
  cancelled: "bg-red-100 text-red-800",
  rescheduled: "bg-yellow-100 text-yellow-800",
};

export const INTERVIEW_TYPE_COLORS: Record<string, string> = {
  online_test: "bg-violet-100 text-violet-800",
  technical: "bg-purple-100 text-purple-800",
  hr: "bg-indigo-100 text-indigo-800",
  managerial: "bg-teal-100 text-teal-800",
};

// AI Mock Interview Types
export type AIInterviewType = "technical" | "hr" | "behavioral" | "mixed";

export const AI_INTERVIEW_TYPE_LABELS: Record<AIInterviewType, string> = {
  technical: "Technical",
  hr: "HR",
  behavioral: "Behavioral",
  mixed: "Mixed",
};

export const AI_INTERVIEW_TYPE_OPTIONS: { value: AIInterviewType; label: string; description: string }[] = [
  { value: "technical", label: "Technical", description: "Coding concepts, system design, and technical problem-solving" },
  { value: "hr", label: "HR", description: "Soft skills, career goals, cultural fit, and professional behavior" },
  { value: "behavioral", label: "Behavioral", description: "Past experiences, teamwork, leadership, and conflict resolution" },
  { value: "mixed", label: "Mixed", description: "A balanced mix of technical and behavioral questions" },
];

export type AIDifficulty = "easy" | "medium" | "hard";

export const AI_DIFFICULTY_OPTIONS: { value: AIDifficulty; label: string; description: string }[] = [
  { value: "easy", label: "Easy", description: "Basic introductory level" },
  { value: "medium", label: "Medium", description: "Intermediate level (default)" },
  { value: "hard", label: "Hard", description: "Advanced expert level" },
];

export const AI_INTERVIEW_STATUS_LABELS: Record<string, string> = {
  not_started: "Not Started",
  in_progress: "In Progress",
  completed: "Completed",
  abandoned: "Abandoned",
};

export const AI_INTERVIEW_STATUS_COLORS: Record<string, string> = {
  not_started: "bg-gray-100 text-gray-800",
  in_progress: "bg-blue-100 text-blue-800",
  completed: "bg-green-100 text-green-800",
  abandoned: "bg-red-100 text-red-800",
};

export const AI_QUESTION_CATEGORY_LABELS: Record<string, string> = {
  technical: "Technical",
  behavioral: "Behavioral",
  hr: "HR",
  project: "Project",
  resume: "Resume",
};
