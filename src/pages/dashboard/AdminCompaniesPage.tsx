import { useState, useEffect } from "react";
import { companyService, type Company } from "@/services/jobService";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Building2, Loader2, CheckCircle, XCircle } from "lucide-react";
import { toast } from "sonner";

export default function AdminCompaniesPage() {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchCompanies = async () => {
    try {
      const data = await companyService.getAllCompanies();
      setCompanies(data);
    } catch {} finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCompanies(); }, []);

  const handleApprove = async (id: string) => {
    try {
      await companyService.approveCompany(id);
      toast.success("Company approved");
      fetchCompanies();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Failed to approve");
    }
  };

  const handleReject = async (id: string) => {
    try {
      await companyService.rejectCompany(id);
      toast.success("Company rejected");
      fetchCompanies();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Failed to reject");
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Companies</h1>
        <p className="text-sm text-muted-foreground mt-1">Manage company registrations.</p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : companies.length === 0 ? (
        <Card className="border-dashed"><CardContent className="flex flex-col items-center justify-center py-12"><Building2 className="size-12 text-muted-foreground/30 mb-3" /><p className="text-sm text-muted-foreground">No companies yet.</p></CardContent></Card>
      ) : (
        <div className="space-y-3">
          {companies.map((c) => (
            <Card key={c.id}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium">{c.name}</p>
                  <p className="text-sm text-muted-foreground">{c.industry ?? "—"}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={c.status === "APPROVED" ? "default" : c.status === "PENDING" ? "secondary" : "destructive"}>
                    {c.status}
                  </Badge>
                  {c.status === "PENDING" && (
                    <>
                      <Button size="sm" variant="outline" onClick={() => handleApprove(c.id)}>
                        <CheckCircle className="mr-1 size-3" /> Approve
                      </Button>
                      <Button size="sm" variant="outline" className="text-destructive" onClick={() => handleReject(c.id)}>
                        <XCircle className="mr-1 size-3" /> Reject
                      </Button>
                    </>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
