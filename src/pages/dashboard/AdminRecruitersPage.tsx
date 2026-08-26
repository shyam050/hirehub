import { useState, useEffect } from "react";
import { userService, type UserProfile } from "@/services/userService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Users, Loader2 } from "lucide-react";

export default function AdminRecruitersPage() {
  const [recruiters, setRecruiters] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    userService.getAllRecruiters()
      .then(setRecruiters)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Recruiters</h1>
        <p className="text-sm text-muted-foreground mt-1">All registered recruiters.</p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : recruiters.length === 0 ? (
        <Card className="border-dashed"><CardContent className="flex flex-col items-center justify-center py-12"><Users className="size-12 text-muted-foreground/30 mb-3" /><p className="text-sm text-muted-foreground">No recruiters yet.</p></CardContent></Card>
      ) : (
        <div className="space-y-3">
          {recruiters.map((r) => (
            <Card key={r.id}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium">{r.name || "Unnamed"}</p>
                  <p className="text-sm text-muted-foreground">{r.email}</p>
                </div>
                <Badge variant="secondary">Recruiter</Badge>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
