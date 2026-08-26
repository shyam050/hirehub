import { useState, useEffect } from "react";
import { userService, type UserProfile } from "@/services/userService";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Users, Loader2 } from "lucide-react";

export default function AdminStudentsPage() {
  const [students, setStudents] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    userService.getAllStudents()
      .then(setStudents)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Students</h1>
        <p className="text-sm text-muted-foreground mt-1">All registered students on the platform.</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : students.length === 0 ? (
        <Card className="border-dashed"><CardContent className="flex flex-col items-center justify-center py-12"><Users className="size-12 text-muted-foreground/30 mb-3" /><p className="text-sm text-muted-foreground">No students registered yet.</p></CardContent></Card>
      ) : (
        <div className="space-y-3">
          {students.map((s) => (
            <Card key={s.id}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium">{s.name || "Unnamed"}</p>
                  <p className="text-sm text-muted-foreground">{s.email}</p>
                </div>
                <Badge variant="secondary">Student</Badge>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
