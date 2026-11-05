"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { api, ApiError, fieldErrorsOf, messageOf } from "@/lib/api";
import type { StudentProfile, User } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

async function loadProfile(): Promise<{ user: User; profile: StudentProfile | null }> {
  const user = await api.get<User>("profile");
  try {
    return { user, profile: await api.get<StudentProfile>("student/profile") };
  } catch (failure) {
    if (failure instanceof ApiError && failure.code === "STUDENT_PROFILE_NOT_FOUND") {
      return { user, profile: null };
    }
    throw failure;
  }
}

export default function ProfilePage() {
  const { data, error, loading, reload } = useLoad(loadProfile);

  if (loading) {
    return <Loading />;
  }
  if (error || !data) {
    return <Alert>{error ?? "Could not load your profile."}</Alert>;
  }
  return (
    <>
      <h1>My profile</h1>
      <AccountForm user={data.user} onSaved={reload} />
      <StudentProfileForm profile={data.profile} onSaved={reload} />
    </>
  );
}

function AccountForm({ user, onSaved }: { user: User; onSaved: () => void }) {
  const [fullName, setFullName] = useState(user.fullName);
  const [message, setMessage] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      await api.put<User>("profile", { fullName });
      setMessage({ kind: "success", text: "Your name was updated." });
      onSaved();
    } catch (failure) {
      setMessage({ kind: "error", text: messageOf(failure) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card section-block">
      <h2>Account</h2>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <form className="form" onSubmit={onSubmit}>
        <div className="form-row">
          <label htmlFor="fullName">Full name</label>
          <input id="fullName" value={fullName} maxLength={150} required onChange={(e) => setFullName(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="accountEmail">Email</label>
          <input id="accountEmail" value={user.email} disabled readOnly />
          <p className="hint">Your email cannot be changed here.</p>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>Save name</button>
      </form>
    </section>
  );
}

function StudentProfileForm({ profile, onSaved }: { profile: StudentProfile | null; onSaved: () => void }) {
  const [university, setUniversity] = useState(profile?.university ?? "");
  const [programme, setProgramme] = useState(profile?.programme ?? "");
  const [yearOfStudy, setYearOfStudy] = useState(profile?.yearOfStudy?.toString() ?? "");
  const [studentNumber, setStudentNumber] = useState(profile?.studentNumber ?? "");
  const [phone, setPhone] = useState(profile?.phone ?? "");
  const [message, setMessage] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setUniversity(profile?.university ?? "");
    setProgramme(profile?.programme ?? "");
    setYearOfStudy(profile?.yearOfStudy?.toString() ?? "");
    setStudentNumber(profile?.studentNumber ?? "");
    setPhone(profile?.phone ?? "");
  }, [profile]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    setFields({});
    const body = {
      university,
      programme,
      yearOfStudy: yearOfStudy === "" ? null : Number(yearOfStudy),
      studentNumber: studentNumber.trim() === "" ? null : studentNumber.trim(),
      phone: phone.trim() === "" ? null : phone.trim(),
    };
    try {
      if (profile) {
        await api.put<StudentProfile>("student/profile", body);
      } else {
        await api.post<StudentProfile>("student/profile", body);
      }
      setMessage({ kind: "success", text: "Your student profile was saved." });
      onSaved();
    } catch (failure) {
      setMessage({ kind: "error", text: messageOf(failure) });
      setFields(fieldErrorsOf(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card section-block">
      <h2>Student profile</h2>
      {!profile && <Alert kind="info">Create your student profile to start adding projects.</Alert>}
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <form className="form" onSubmit={onSubmit} noValidate>
        <div className="form-row">
          <label htmlFor="university">University or college</label>
          <input id="university" value={university} maxLength={200} required onChange={(e) => setUniversity(e.target.value)} />
          {fields.university && <p className="field-error">{fields.university}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="programme">Programme</label>
          <input id="programme" value={programme} maxLength={200} required onChange={(e) => setProgramme(e.target.value)} />
          {fields.programme && <p className="field-error">{fields.programme}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="yearOfStudy">Year of study (optional)</label>
          <input id="yearOfStudy" type="number" min={1} max={10} value={yearOfStudy} onChange={(e) => setYearOfStudy(e.target.value)} />
          {fields.yearOfStudy && <p className="field-error">{fields.yearOfStudy}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="studentNumber">Student number (optional)</label>
          <input id="studentNumber" value={studentNumber} maxLength={50} onChange={(e) => setStudentNumber(e.target.value)} />
          {fields.studentNumber && <p className="field-error">{fields.studentNumber}</p>}
        </div>
        <div className="form-row">
          <label htmlFor="phone">Phone (optional)</label>
          <input id="phone" type="tel" value={phone} maxLength={30} onChange={(e) => setPhone(e.target.value)} />
          {fields.phone && <p className="field-error">{fields.phone}</p>}
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>{profile ? "Save profile" : "Create profile"}</button>
      </form>
    </section>
  );
}
