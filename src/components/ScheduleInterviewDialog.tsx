import { interviewService } from "@/services/interviewService";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { INTERVIEW_TYPE_OPTIONS } from "@/lib/constants";
import type { InterviewType } from "@/lib/constants";
import { useState } from "react";
import { toast } from "sonner";

interface ScheduleInterviewDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  applicationId: string;
  jobTitle: string;
}

export function ScheduleInterviewDialog({
  open,
  onOpenChange,
  applicationId,
  jobTitle,
}: ScheduleInterviewDialogProps) {
  const [interviewType, setInterviewType] = useState<InterviewType>("technical");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [duration, setDuration] = useState("60");
  const [meetingLink, setMeetingLink] = useState("");
  const [interviewerName, setInterviewerName] = useState("");
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(false);

  const resetForm = () => {
    setInterviewType("technical");
    setDate("");
    setTime("");
    setDuration("60");
    setMeetingLink("");
    setInterviewerName("");
    setNotes("");
  };

  const handleSubmit = async () => {
    if (!date || !time) {
      toast.error("Please select a date and time");
      return;
    }

    const scheduledAt = new Date(`${date}T${time}`).toISOString();
    if (isNaN(new Date(scheduledAt).getTime()) || new Date(scheduledAt) < new Date()) {
      toast.error("Please select a valid future date and time");
      return;
    }

    setLoading(true);
    try {
      await interviewService.scheduleInterview({
        applicationId,
        interviewType,
        scheduledAt,
        duration: parseInt(duration),
        meetingLink: meetingLink || undefined,
        interviewerName: interviewerName || undefined,
        notes: notes || undefined,
      });
      toast.success("Interview scheduled");
      resetForm();
      onOpenChange(false);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error.message || "Failed to schedule interview");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        onOpenChange(o);
        if (!o) resetForm();
      }}
    >
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Schedule Interview</DialogTitle>
          <p className="text-sm text-muted-foreground">
            For application to {jobTitle}
          </p>
        </DialogHeader>

        <div className="space-y-4">
          {/* Interview Type */}
          <div className="space-y-2">
            <Label>Interview Type</Label>
            <Select
              value={interviewType}
              onValueChange={(v) => setInterviewType(v as InterviewType)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {INTERVIEW_TYPE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Date + Time */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label>Date</Label>
              <Input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                min={new Date().toISOString().split("T")[0]}
              />
            </div>
            <div className="space-y-2">
              <Label>Time</Label>
              <Input
                type="time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
              />
            </div>
          </div>

          {/* Duration */}
          <div className="space-y-2">
            <Label>Duration (minutes)</Label>
            <Select value={duration} onValueChange={setDuration}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="30">30 minutes</SelectItem>
                <SelectItem value="45">45 minutes</SelectItem>
                <SelectItem value="60">60 minutes</SelectItem>
                <SelectItem value="90">90 minutes</SelectItem>
                <SelectItem value="120">120 minutes</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Meeting Link */}
          <div className="space-y-2">
            <Label>Meeting Link (optional)</Label>
            <Input
              placeholder="https://meet.google.com/..."
              value={meetingLink}
              onChange={(e) => setMeetingLink(e.target.value)}
            />
          </div>

          {/* Interviewer Name */}
          <div className="space-y-2">
            <Label>Interviewer Name (optional)</Label>
            <Input
              placeholder="e.g., Jane Smith"
              value={interviewerName}
              onChange={(e) => setInterviewerName(e.target.value)}
            />
          </div>

          {/* Notes */}
          <div className="space-y-2">
            <Label>Notes (optional)</Label>
            <Textarea
              placeholder="Any preparation instructions or notes for the candidate..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? "Scheduling..." : "Schedule Interview"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
