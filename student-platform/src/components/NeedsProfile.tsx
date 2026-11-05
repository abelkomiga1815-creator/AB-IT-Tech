import Link from "next/link";

/** Shown wherever the backend answers STUDENT_PROFILE_NOT_FOUND. */
export function NeedsProfile() {
  return (
    <div className="card onboarding">
      <h2>Complete your student profile</h2>
      <p>Before you can create projects we need a few details about your studies: university and programme.</p>
      <Link href="/profile" className="btn btn-primary">
        Create student profile
      </Link>
    </div>
  );
}
