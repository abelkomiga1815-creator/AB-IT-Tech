"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { useLoad } from "@/hooks/useLoad";
import { api, auth } from "@/lib/api";
import type { User } from "@/lib/types";

const LINKS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/projects", label: "Projects" },
  { href: "/profile", label: "Profile" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const { data: user } = useLoad(() => api.get<User>("auth/me"));

  async function logout() {
    await auth.logout().catch(() => undefined);
    router.push("/login");
    router.refresh();
  }

  return (
    <>
      <a href="#main" className="skip-link">Skip to main content</a>
      <header className="app-header">
        <div className="container header-inner">
          <Link href="/dashboard" className="brand">AB IT Tech Solution <span>Student Platform</span></Link>
          <button type="button" className="menu-toggle" aria-expanded={open} aria-controls="app-nav" onClick={() => setOpen((v) => !v)}>
            <span className="sr-only">{open ? "Close menu" : "Open menu"}</span>
            <span aria-hidden="true">{open ? "\u2715" : "\u2630"}</span>
          </button>
          <nav id="app-nav" aria-label="Main" className={open ? "app-nav open" : "app-nav"}>
            <ul>
              {LINKS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    aria-current={pathname === link.href || pathname.startsWith(`${link.href}/`) ? "page" : undefined}
                    onClick={() => setOpen(false)}
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <span className="user-name">{user?.fullName}</span>
            <button type="button" className="btn btn-outline btn-small" onClick={logout}>Sign out</button>
          </nav>
        </div>
      </header>
      <main id="main" className="container page">{children}</main>
    </>
  );
}
