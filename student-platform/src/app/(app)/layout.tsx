"use client";

import type { ReactNode } from "react";
import { AppNav } from "@/components/AppNav";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

export default function AppLayout({ children }: { children: ReactNode }) {
  const me = useLoad(() => api.get<User>("auth/me"));

  return (
    <>
      <AppNav fullName={me.data?.fullName ?? null} />
      <main id="main" className="container section">{children}</main>
    </>
  );
}
