import type { Metadata } from "next";
import { PageHero } from "@/components/PageHero";
import { STUDENT_PLATFORM_URL } from "@/config/site";

export const metadata: Metadata = {
  title: "Student Project Platform",
  description: "A dedicated platform for university and college students to manage projects, research and defense preparation.",
};

const FEATURES = [
  { title: "Project management", text: "Organize your project from idea to final submission in one place." },
  { title: "Research assistance", text: "Find related work and explore your topic with source links you can verify." },
  { title: "Research gap analysis", text: "Get evidence-based suggestions about where your work can contribute." },
  { title: "Documents", text: "Keep drafts and supporting files together with your project." },
  { title: "Defense preparation", text: "Practise likely questions and get ready to present with confidence." },
  { title: "Progress tracking", text: "See what is done, what is next and how far you have come." },
];

export default function StudentPlatformPage() {
  return (
    <>
      <PageHero
        title="Student Project Platform"
        intro="A dedicated platform from AB IT Tech Solution for university and college students."
      />
      <section className="section">
        <div className="container">
          <div className="notice notice-info">
            The Student Platform is a separate product for students. Our company services on this website are
            for all customers.
          </div>
          <div className="grid grid-3">
            {FEATURES.map((feature) => (
              <div key={feature.title} className="card">
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
              </div>
            ))}
          </div>
          <p className="fine-print">
            Research assistance gives indicators and suggestions based on the sources searched; it supports your
            own work and does not replace your supervisor&apos;s judgment.
          </p>
          <p className="center">
            <a href={STUDENT_PLATFORM_URL} className="btn btn-primary">Go to the Student Platform</a>
          </p>
        </div>
      </section>
    </>
  );
}
