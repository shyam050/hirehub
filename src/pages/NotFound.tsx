import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="min-h-screen flex flex-col"
    >
      <div className="flex-1 flex flex-col items-center justify-center">
        <div className="max-w-lg mx-auto relative px-4 text-center">
          <h1 className="text-6xl font-bold tracking-tight text-primary mb-4">
            404
          </h1>
          <p className="text-lg text-muted-foreground mb-6">
            The page you're looking for doesn't exist or has been moved.
          </p>
          <Button onClick={() => navigate("/")}>
            <ArrowLeft className="mr-2 size-4" />
            Back to Home
          </Button>
        </div>
      </div>
    </motion.div>
  );
}
