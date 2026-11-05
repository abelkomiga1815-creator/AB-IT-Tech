import Link from "next/link";
import { COMPANY_NAME, CORPORATE_SITE_URL, SITE_NAME } from "@/config/site";

const FEATURES = [
  { title: "Manage your project", text: "Keep your title, supervisor, deadline and status in one place." },
  { title: "Track your progress", text: "Record each stage from topic selection to submission and see how far you have come." },
  { title: "Keep your documents together", text: "Upload proposals, drafts, references and data to your project." },
  { title: "Prepare your defense", text: "Write the questions you expect, prepare answers and mark what you have practised." },
  { title: "Research assistance", text: "Find related papers, see how close they are to your project and get evidence-based suggestions about possible gaps." },
];

export default function LandingPage() {
  return (
    <>
      <header className="landing-hero">
        <div className="container">
          <p className="eyebrow">{COMPANY_NAME}</p>
          <h1>{SITE_NAME}</h1>
          <p className="lead">Everything you need to organise your university or college project, from your first idea to your defense.</p>
          <div className="hero-actions">
            <Link href="/register" className="btn btn-primary">Create a student account</Link>
            <Link href="/login" className="btn btn-outline-light">Sign in</Link>
          </div>
        </div>
      </header>
      <main id="main" className="container section">
        <h2>What you can do</h2>
        <div className="grid">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="card">
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </div>
          ))}
        </div>
        <p className="muted note">
          Research assistance gives indicators and suggestions based on the papers it finds. It supports your own work and never replaces your supervisor&apos;s judgment.
        </p>
      </main>
      <footer className="footer">
        <div className="container">
          <p>&copy; {new Date().getFullYear()} {COMPANY_NAME}. This platform is for students. Looking for our company services?{" "}
            <a href={CORPORATE_SITE_URL}>Visit our main website</a>.</p>
        </div>
      </footer>
    </>
  );
}
