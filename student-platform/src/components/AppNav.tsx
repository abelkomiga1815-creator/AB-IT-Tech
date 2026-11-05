"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { auth } from "@/lib/api";
import { SITE_NAME } from "@/config/site";

const LINKS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/projects", label: "My Projects" },
  { href: "/profile", label: "Profile" },
];

export function AppNav({ fullName }: { fullName: string | null }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  async function signOut() {
    await auth.logout();
    router.push("/login");
    router.refresh();
  }

  return (
    <header className="app-header">
      <div className="container header-inner">
        <Link href="/dashboard" className="brand">{SITE_NAME}</Link>
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
          <div className="nav-user">
            {fullName && <span className="muted">{fullName}</span>}
            <button type="button" className="btn btn-outline btn-small" onClick={signOut}>Sign out</button>
          </div>
        </nav>
      </div>
    </header>
  );
}
