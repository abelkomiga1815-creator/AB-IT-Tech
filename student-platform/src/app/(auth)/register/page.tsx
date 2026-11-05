"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { auth, fieldErrorsOf, messageOf } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFields({});
    if (password !== confirm) {
      setFields({ confirm: "The passwords do not match." });
      return;
    }
    setBusy(true);
    try {
      const { signedIn } = await auth.register(fullName.trim(), email.trim(), password);
      router.push(signedIn ? "/profile" : "/login");
      router.refresh();
    } catch (failure) {
      setError(messageOf(failure));
      setFields(fieldErrorsOf(failure));
      setBusy(false);
    }
  }

  return (
    <>
      <h1>Create a student account</h1>
      {error && <Alert>{error}</Alert>}
      <form className="form" onSubmit={onSubmit} noValidate>
        <div className="form-row">
          <label htmlFor="fullName">Full name</label>
          <input id="fullName" type="text" autoComplete="name" required value={fullName} onChange={(e) => setFullName(e.target.value)} />
          {fields.fullName && <p className="field-error">{fields.fullName}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          {fields.email && <p className="field-error">{fields.email}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={password} onChange={(e) => setPassword(e.target.value)} />
          <p className="hint">8 to 72 characters.</p>
          {fields.password && <p className="field-error">{fields.password}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="confirm">Confirm password</label>
          <input id="confirm" type="password" autoComplete="new-password" required value={confirm} onChange={(e) => setConfirm(e.target.value)} />
          {fields.confirm && <p className="field-error">{fields.confirm}</p>}
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Creating account..." : "Create account"}</button>
      </form>
      <p className="muted">Already registered? <Link href="/login">Sign in</Link></p>
    </>
  );
}
