import { useState, useEffect } from "react";
import { companyService, type Company } from "@/services/jobService";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Loader2, Save, Building2, CheckCircle, Clock } from "lucide-react";
import { toast } from "sonner";

export default function CompanyPage() {
  const [company, setCompany] = useState<Company | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [industry, setIndustry] = useState("");
  const [location, setLocation] = useState("");
  const [website, setWebsite] = useState("");
  const [size, setSize] = useState("");

  useEffect(() => {
    companyService.getMyCompany()
      .then((c) => {
        setCompany(c);
        if (c) {
          setName(c.name);
          setDescription(c.description ?? "");
          setIndustry(c.industry ?? "");
          setLocation(c.location ?? "");
          setWebsite(c.website ?? "");
          setSize(c.size ?? "");
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!name.trim()) {
      toast.error("Company name is required");
      return;
    }
    setSaving(true);
    try {
      if (company) {
        await companyService.updateCompany(company.id, { name, description, industry, location, website, size });
      } else {
        const newCompany = await companyService.createCompany({ name, description, industry, location, website, size });
        setCompany(newCompany);
      }
      toast.success("Company profile saved!");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to save company profile");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>;
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Company Profile</h1>
        <p className="text-sm text-muted-foreground mt-1">
          {company ? "Update your company information." : "Create your company profile to start posting positions."}
        </p>
      </div>

      {company && (
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className={`flex size-10 items-center justify-center rounded-lg ${company.status === "APPROVED" ? "bg-green-100 text-green-600" : company.status === "REJECTED" ? "bg-red-100 text-red-600" : "bg-yellow-100 text-yellow-600"}`}>
                {company.status === "APPROVED" ? <CheckCircle className="size-5" /> : <Clock className="size-5" />}
              </div>
              <div>
                <p className="font-medium">Company Status: {company.status}</p>
                <p className="text-sm text-muted-foreground">
                  {company.status === "APPROVED" ? "Your company is approved and active." : company.status === "REJECTED" ? "Your company was rejected. Contact support." : "Awaiting admin approval."}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle className="text-base">Company Details</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>Company Name *</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Acme Corp" />
          </div>
          <div className="space-y-2">
            <Label>Description</Label>
            <Textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Tell candidates about your company..." rows={3} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Industry</Label>
              <Input value={industry} onChange={(e) => setIndustry(e.target.value)} placeholder="Technology, Finance..." />
            </div>
            <div className="space-y-2">
              <Label>Location</Label>
              <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="San Francisco, CA" />
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Website</Label>
              <Input value={website} onChange={(e) => setWebsite(e.target.value)} placeholder="https://company.com" />
            </div>
            <div className="space-y-2">
              <Label>Company Size</Label>
              <Input value={size} onChange={(e) => setSize(e.target.value)} placeholder="1-50, 51-200..." />
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={saving}>
          {saving ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Save className="mr-2 size-4" />}
          {company ? "Save Changes" : "Create Company"}
        </Button>
      </div>
    </div>
  );
}
