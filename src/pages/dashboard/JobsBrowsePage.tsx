import { useState, useEffect, useCallback } from "react";
import { jobService, type Job, type JobMatch } from "@/services/jobService";
import { applicationService } from "@/services/applicationService";
import { resumeService } from "@/services/resumeService";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Search,
  MapPin,
  Clock,
  Building2,
  Briefcase,
  Sparkles,
  Loader2,
} from "lucide-react";
import { useNavigate } from "react-router";
import { JOB_TYPE_LABELS } from "@/lib/constants";
import type { JobType } from "@/lib/constants";
import { toast } from "sonner";

export default function JobsBrowsePage() {
  const [search, setSearch] = useState("");
  const [location, setLocation] = useState("all");
  const [type, setType] = useState<string>("all");
  const [jobs, setJobs] = useState<Job[]>([]);
  const [matches, setMatches] = useState<JobMatch[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchJobs = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { size: 50 };
      if (search) params.search = search;
      if (location !== "all") params.location = location;
      if (type !== "all") params.type = type;
      const result = await jobService.browseJobs(params);
      setJobs(result.content ?? []);
    } catch {
      setJobs([]);
    } finally {
      setLoading(false);
    }
  }, [search, location, type]);

  useEffect(() => { fetchJobs(); }, [fetchJobs]);

  useEffect(() => {
    jobService.getMyJobMatches().then(setMatches).catch(() => setMatches([]));
  }, []);

  const matchMap = new Map<string, JobMatch>();
  for (const m of matches) matchMap.set(m.jobId, m);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Browse Positions</h1>
        <p className="text-sm text-muted-foreground mt-1">Search and filter open positions across all registered companies.</p>
      </div>

      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input placeholder="Search by title, skills, or description..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
            </div>
            <div className="flex gap-3">
              <Select value={location} onValueChange={setLocation}>
                <SelectTrigger className="w-36"><SelectValue placeholder="Location" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Locations</SelectItem>
                  <SelectItem value="New York">New York</SelectItem>
                  <SelectItem value="San Francisco">San Francisco</SelectItem>
                  <SelectItem value="London">London</SelectItem>
                  <SelectItem value="Remote">Remote</SelectItem>
                </SelectContent>
              </Select>
              <Select value={type} onValueChange={setType}>
                <SelectTrigger className="w-36"><SelectValue placeholder="Job Type" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  <SelectItem value="FULL_TIME">Full Time</SelectItem>
                  <SelectItem value="PART_TIME">Part Time</SelectItem>
                  <SelectItem value="INTERNSHIP">Internship</SelectItem>
                  <SelectItem value="CONTRACT">Contract</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : jobs.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Briefcase className="size-12 text-muted-foreground/30 mb-3" />
          <p className="text-sm text-muted-foreground">No positions match your search criteria.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {jobs.map((job) => (
            <JobCard key={job.id} job={job} match={matchMap.get(job.id)} />
          ))}
        </div>
      )}
    </div>
  );
}

function JobCard({ job, match }: { job: Job; match?: JobMatch }) {
  const [applying, setApplying] = useState(false);
  const [hasApplied, setHasApplied] = useState(false);
  const [matching, setMatching] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    applicationService.getMyApplications().then((apps) => {
      setHasApplied(apps.some((a) => a.jobId === job.id));
    }).catch(() => {});
  }, [job.id]);

  const handleApply = async () => {
    setApplying(true);
    try {
      await applicationService.applyToJob(job.id);
      setHasApplied(true);
      toast.success("Application submitted!");
      navigate("/dashboard/applications");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to apply");
    } finally {
      setApplying(false);
    }
  };

  const handleMatch = async () => {
    setMatching(true);
    try {
      const result = await jobService.calculateMatch(job.id);
      toast.success("Match calculated!");
      // Force refresh matches
      window.location.reload();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to calculate match");
    } finally {
      setMatching(false);
    }
  };

  const matchScore = match?.matchScore;

  return (
    <Card className="hover:border-primary/20 transition-colors">
      <CardContent className="p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <h3 className="font-semibold text-lg truncate">{job.title}</h3>
              <Badge variant="secondary" className="shrink-0">{JOB_TYPE_LABELS[job.type as JobType] ?? job.type}</Badge>
              {matchScore !== undefined && (
                <Badge className={`shrink-0 ${matchScore >= 70 ? "bg-green-100 text-green-700" : matchScore >= 40 ? "bg-yellow-100 text-yellow-700" : "bg-red-100 text-red-700"}`}>
                  {matchScore}% Match
                </Badge>
              )}
            </div>
            <div className="flex items-center gap-3 text-sm text-muted-foreground mb-3">
              <span className="flex items-center gap-1"><Building2 className="size-3.5" /> {job.company?.name ?? "Unknown"}</span>
              <span className="flex items-center gap-1"><MapPin className="size-3.5" /> {job.location}</span>
              <span className="flex items-center gap-1"><Clock className="size-3.5" /> {new Date(job.createdAt).toLocaleDateString()}</span>
            </div>
            <p className="text-sm text-muted-foreground line-clamp-2 mb-3">{job.description}</p>
            <div className="flex flex-wrap gap-1.5">
              {job.skills.slice(0, 6).map((skill: string) => (
                <Badge key={skill} variant={match?.matchedSkills?.includes(skill) ? "default" : "outline"} className="text-xs">{skill}</Badge>
              ))}
              {job.skills.length > 6 && <Badge variant="outline" className="text-xs">+{job.skills.length - 6}</Badge>}
            </div>
            {(job.salaryMin || job.salaryMax) && (
              <p className="mt-2 text-sm font-medium text-green-600">
                {job.salaryMin && job.salaryMax ? `$${job.salaryMin.toLocaleString()} - $${job.salaryMax.toLocaleString()}` : job.salaryMin ? `From $${job.salaryMin.toLocaleString()}` : `Up to $${job.salaryMax?.toLocaleString()}`}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-2 shrink-0 items-end">
            {!match && (
              <Button variant="outline" size="sm" disabled={matching} onClick={handleMatch}>
                {matching ? <Loader2 className="mr-1 size-3 animate-spin" /> : <Sparkles className="mr-1 size-3" />}
                {matching ? "Analyzing..." : "Analyze Match"}
              </Button>
            )}
            <Button onClick={handleApply} disabled={hasApplied || applying} size="sm">
              {applying && <Loader2 className="mr-1 size-3 animate-spin" />}
              {hasApplied ? "Applied" : "Apply"}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
