import Link from "next/link";
import type { ReactNode } from "react";
import { SITE_NAME } from "@/config/site";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main id="main" className="auth-page">
      <div className="auth-card">
        <Link href="/" className="brand brand-dark">{SITE_NAME}</Link>
        {children}
      </div>
    </main>
  );
}
