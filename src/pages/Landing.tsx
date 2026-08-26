import { motion } from "framer-motion";
import {
  Briefcase,
  CheckCircle,
  ArrowRight,
  Building2,
  GraduationCap,
  Shield,
  Users,
  FileText,
  Search,
  Clock,
  ChevronRight,
  Brain,
  Target,
  Zap,
  BarChart3,
  MessageSquare,
} from "lucide-react";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/ThemeToggle";

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Nav */}
      <nav className="sticky top-0 z-50 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-5">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded bg-foreground text-background">
              <Briefcase className="size-3.5" />
            </div>
            <span className="text-sm font-semibold tracking-tight">HireHub</span>
          </div>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <Button
              variant="ghost"
              size="sm"
              className="hidden sm:inline-flex"
              onClick={() => navigate("/auth")}
            >
              Sign in
            </Button>
            <Button
              size="sm"
              className="ml-1"
              onClick={() => navigate("/auth")}
            >
              Get started
            </Button>
          </div>
        </div>
      </nav>

      {/* Hero — two-column, left-aligned, AI-focused */}
      <section className="px-5">
        <div className="mx-auto max-w-5xl pt-20 pb-24 sm:pt-28 sm:pb-32">
          <div className="grid gap-12 lg:grid-cols-[1fr_340px] lg:items-start">
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4 }}
            >
              <p className="mb-4 text-xs font-medium uppercase tracking-widest text-muted-foreground">
                AI-Powered Campus Recruitment
              </p>
              <h1 className="text-4xl font-bold tracking-tight leading-[1.1] sm:text-5xl">
                Smarter hiring.
                <br />
                Better candidates.
                <br />
                One platform.
              </h1>
              <p className="mt-5 max-w-lg text-base leading-relaxed text-muted-foreground">
                HireHub combines AI resume analysis, intelligent job matching,
                and mock interview coaching with a full recruitment pipeline —
                so students land better roles and recruiters find the right fit
                faster.
              </p>
              <div className="mt-8 flex items-center gap-3">
                <Button size="lg" onClick={() => navigate("/auth")}>
                  Create an account
                  <ArrowRight className="ml-2 size-4" />
                </Button>
                <Button
                  size="lg"
                  variant="ghost"
                  onClick={() => navigate("/auth")}
                >
                  Sign in
                </Button>
              </div>
            </motion.div>

            {/* Terminal-style product preview — AI focused */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.15 }}
              className="hidden lg:block"
            >
              <div className="rounded-lg border border-border bg-card overflow-hidden">
                <div className="flex items-center gap-1.5 border-b border-border px-3 py-2">
                  <div className="size-2.5 rounded-full bg-muted-foreground/20" />
                  <div className="size-2.5 rounded-full bg-muted-foreground/20" />
                  <div className="size-2.5 rounded-full bg-muted-foreground/20" />
                  <span className="ml-2 text-[10px] text-muted-foreground font-mono">
                    hirehub — ai engine
                  </span>
                </div>
                <div className="p-4 font-mono text-xs leading-5 text-muted-foreground">
                  <p className="text-foreground">
                    <span className="text-muted-foreground">$</span> hirehub
                    analyze resume.pdf
                  </p>
                  <p className="mt-2">
                    <span className="text-green-600">●</span> Score:{" "}
                    <span className="text-foreground font-semibold">87/100</span>
                  </p>
                  <p>
                    <span className="text-muted-foreground/60"> strengths:</span>{" "}
                    Spring Boot, REST APIs, SQL
                  </p>
                  <p>
                    <span className="text-muted-foreground/60"> gaps:</span>{" "}
                    Docker, Kubernetes, CI/CD
                  </p>
                  <p className="mt-3 text-foreground">
                    <span className="text-muted-foreground">$</span> hirehub
                    match --job "Backend Engineer"
                  </p>
                  <p className="mt-2">
                    <span className="text-blue-600">●</span> Match:{" "}
                    <span className="text-foreground font-semibold">82%</span>{" "}
                    with Acme Corp
                  </p>
                  <p>
                    <span className="text-muted-foreground/60"> missing:</span>{"{"}{" "}
                    Kubernetes, AWS, gRPC
                  </p>
                  <p className="mt-3 text-foreground">
                    <span className="text-muted-foreground">$</span> hirehub
                    mock-interview --type technical
                  </p>
                  <p className="mt-2">
                    <span className="text-amber-600">●</span> Question 1/5:{" "}
                    <span className="text-foreground">
                      Explain dependency injection...
                    </span>
                  </p>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* AI Features — three feature blocks */}
      <section className="border-y border-border bg-muted/30">
        <div className="mx-auto max-w-5xl px-5 py-16">
          <p className="mb-3 text-xs font-medium uppercase tracking-widest text-muted-foreground">
            AI-powered features
          </p>
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl mb-10">
            Intelligence at every step.
          </h2>

          <div className="grid gap-8 md:grid-cols-3">
            {/* Resume Analyzer */}
            <div className="group">
              <div className="mb-4 flex size-10 items-center justify-center rounded-lg border border-border bg-background">
                <FileText className="size-5 text-muted-foreground" />
              </div>
              <h3 className="text-base font-semibold mb-2">Resume Analyzer</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">
                Upload a PDF resume and get an instant AI-powered evaluation —
                overall score, extracted skills, strengths, weaknesses, and
                actionable recommendations for improvement.
              </p>
              <div className="mt-4 flex flex-wrap gap-1.5">
                {["Score 0–100", "Skill extraction", "Gap analysis"].map(
                  (tag) => (
                    <span
                      key={tag}
                      className="rounded border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                    >
                      {tag}
                    </span>
                  )
                )}
              </div>
            </div>

            {/* Job Matching */}
            <div className="group">
              <div className="mb-4 flex size-10 items-center justify-center rounded-lg border border-border bg-background">
                <Target className="size-5 text-muted-foreground" />
              </div>
              <h3 className="text-base font-semibold mb-2">Job Matching</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">
                See how well you match each job based on your skills, education,
                and experience. The AI blends deterministic skill overlap with
                semantic understanding for accurate, explainable scores.
              </p>
              <div className="mt-4 flex flex-wrap gap-1.5">
                {[
                  "Match percentage",
                  "Missing skills",
                  "Recommended jobs",
                ].map((tag) => (
                  <span
                    key={tag}
                    className="rounded border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            {/* Mock Interview Coach */}
            <div className="group">
              <div className="mb-4 flex size-10 items-center justify-center rounded-lg border border-border bg-background">
                <Brain className="size-5 text-muted-foreground" />
              </div>
              <h3 className="text-base font-semibold mb-2">
                Mock Interview Coach
              </h3>
              <p className="text-sm text-muted-foreground leading-relaxed">
                Practice with an AI interviewer that adapts to your answers.
                Choose technical, HR, behavioral, or mixed formats. Get scored,
                evaluated, and a detailed report with improvement areas.
              </p>
              <div className="mt-4 flex flex-wrap gap-1.5">
                {["Adaptive difficulty", "Real-time scoring", "Final report"].map(
                  (tag) => (
                    <span
                      key={tag}
                      className="rounded border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                    >
                      {tag}
                    </span>
                  )
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Roles — horizontal strip, not cards */}
      <section className="px-5">
        <div className="mx-auto max-w-5xl py-16">
          <p className="mb-6 text-xs font-medium uppercase tracking-widest text-muted-foreground">
            Three roles, one system
          </p>
          <div className="grid gap-6 sm:grid-cols-3">
            {[
              {
                icon: GraduationCap,
                role: "Students",
                items: [
                  "Upload resume and get AI analysis",
                  "Browse jobs with match scores",
                  "Practice with AI mock interviews",
                  "Apply and track applications",
                ],
              },
              {
                icon: Building2,
                role: "Recruiters",
                items: [
                  "Post positions with skill requirements",
                  "Review applicants and shortlist",
                  "Schedule and manage interviews",
                  "Track recruitment pipeline",
                ],
              },
              {
                icon: Shield,
                role: "Placement admins",
                items: [
                  "Approve companies and recruiters",
                  "Monitor all applications",
                  "View platform statistics",
                  "Manage user accounts",
                ],
              },
            ].map((col) => (
              <div key={col.role}>
                <div className="mb-3 flex items-center gap-2">
                  <col.icon className="size-4 text-muted-foreground" />
                  <h3 className="text-sm font-semibold">{col.role}</h3>
                </div>
                <ul className="space-y-1.5">
                  {col.items.map((item) => (
                    <li
                      key={item}
                      className="flex items-center gap-2 text-sm text-muted-foreground"
                    >
                      <ChevronRight className="size-3 shrink-0" />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Pipeline — single wide section, not a card grid */}
      <section className="border-y border-border bg-muted/30 px-5">
        <div className="mx-auto max-w-5xl py-16">
          <div className="max-w-lg">
            <p className="mb-3 text-xs font-medium uppercase tracking-widest text-muted-foreground">
              Application pipeline
            </p>
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
              Every stage, tracked.
            </h2>
            <p className="mt-3 text-muted-foreground leading-relaxed">
              Applications move through a transparent pipeline. Students see
              where they stand. Recruiters manage each step. AI-powered insights
              help both sides make better decisions.
            </p>
          </div>

          <div className="mt-10 flex flex-wrap items-center gap-2 text-sm font-medium">
            {[
              "Applied",
              "Screening",
              "Shortlisted",
              "Interview",
              "Offered",
              "Selected",
            ].map((stage, i) => (
              <span key={stage} className="flex items-center gap-2">
                <span className="rounded border border-border bg-background px-3 py-1.5 text-xs">
                  {stage}
                </span>
                {i < 5 && (
                  <span className="text-muted-foreground/40">→</span>
                )}
              </span>
            ))}
          </div>

          <div className="mt-12 grid gap-8 sm:grid-cols-3">
            <div className="flex items-start gap-3">
              <FileText className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <div>
                <h4 className="text-sm font-medium">Full timeline history</h4>
                <p className="mt-1 text-sm text-muted-foreground leading-relaxed">
                  Every status change is logged with a timestamp and optional
                  note. No ambiguity about what happened and when.
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Clock className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <div>
                <h4 className="text-sm font-medium">Instant notifications</h4>
                <p className="mt-1 text-sm text-muted-foreground leading-relaxed">
                  Students get notified the moment their application moves
                  forward. Recruiters see new applicants as they come in.
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Brain className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <div>
                <h4 className="text-sm font-medium">AI-powered insights</h4>
                <p className="mt-1 text-sm text-muted-foreground leading-relaxed">
                  Get matched to jobs based on your profile. Practice with AI
                  interviews before the real thing. Improve your resume with
                  data-driven feedback.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats — numbers that matter */}
      <section className="px-5">
        <div className="mx-auto max-w-5xl py-16">
          <div className="grid gap-8 sm:grid-cols-4">
            {[
              { label: "Resume analyses", value: "AI-powered", icon: FileText },
              { label: "Job matching", value: "Skill-based", icon: Target },
              { label: "Interview prep", value: "Adaptive AI", icon: Brain },
              { label: "Application tracking", value: "Real-time", icon: Zap },
            ].map((stat) => (
              <div key={stat.label} className="text-center">
                <stat.icon className="mx-auto size-5 text-muted-foreground mb-2" />
                <div className="text-lg font-bold">{stat.value}</div>
                <div className="text-xs text-muted-foreground mt-1">
                  {stat.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Tech — minimal, honest */}
      <section className="border-t border-border px-5">
        <div className="mx-auto max-w-5xl py-16">
          <div className="grid gap-12 lg:grid-cols-[1fr_1fr] lg:items-start">
            <div>
              <p className="mb-3 text-xs font-medium uppercase tracking-widest text-muted-foreground">
                Under the hood
              </p>
              <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
                Built with tools you already know.
              </h2>
              <p className="mt-3 text-muted-foreground leading-relaxed max-w-md">
                No proprietary frameworks. No lock-in. The stack is
                straightforward, fully typed, and easy to extend — including the
                AI layer.
              </p>
            </div>
            <div className="space-y-4">
              {[
                {
                  name: "React + TypeScript",
                  detail: "End-to-end type safety from database to UI.",
                },
                {
                  name: "Spring Boot",
                  detail:
                    "Production-grade Java backend with PostgreSQL, JWT auth, and REST APIs.",
                },
                {
                  name: "OpenAI (server-side)",
                  detail:
                    "AI resume analysis, job matching, and interview coaching — all server-side with API keys kept private.",
                },
                {
                  name: "Role-based access control",
                  detail:
                    "Server-side authorization on every query and mutation. No client-side role checks.",
                },
              ].map((item) => (
                <div key={item.name} className="group">
                  <h4 className="text-sm font-medium">{item.name}</h4>
                  <p className="mt-0.5 text-sm text-muted-foreground leading-relaxed">
                    {item.detail}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="border-t border-border px-5">
        <div className="mx-auto max-w-5xl py-16 sm:text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            Ready to hire smarter?
          </h2>
          <p className="mt-3 text-muted-foreground">
            Free to use. AI-powered. No credit card required.
          </p>
          <div className="mt-8 flex items-center justify-center gap-3">
            <Button size="lg" onClick={() => navigate("/auth")}>
              Create an account
              <ArrowRight className="ml-2 size-4" />
            </Button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border px-5">
        <div className="mx-auto flex max-w-5xl items-center justify-between py-6">
          <div className="flex items-center gap-2">
            <div className="flex size-5 items-center justify-center rounded bg-foreground text-background">
              <Briefcase className="size-2.5" />
            </div>
            <span className="text-xs font-semibold">HireHub</span>
          </div>
          <p className="text-xs text-muted-foreground">
            © {new Date().getFullYear()} HireHub
          </p>
        </div>
      </footer>
    </div>
  );
}
