import type { Metadata } from "next";
import Link from "next/link";
import { PageHero } from "@/components/PageHero";
import { SITE_NAME } from "@/config/site";

export const metadata: Metadata = { title: "About" };

const VALUES = [
  { title: "Quality", text: "We take pride in work that is reliable, tidy and easy to maintain." },
  { title: "Security", text: "We protect your data and your users' data by design." },
  { title: "Honesty", text: "Clear scopes, realistic timelines and no overpromising." },
  { title: "Learning", text: "We keep growing our skills and help our customers and students grow theirs." },
];

export default function AboutPage() {
  return (
    <>
      <PageHero
        title={`About ${SITE_NAME}`}
        intro="We are a technology company that helps people and organizations use technology with confidence."
      />
      <section className="section">
        <div className="container prose">
          <h2>What we do</h2>
          <p>
            {SITE_NAME} provides software development, web development, mobile app development, cybersecurity,
            data analysis, UI/UX and graphic design, and IT support and maintenance. Our customers include
            individuals, businesses, organizations, institutions, startups and students.
          </p>
          <h2>Our mission</h2>
          <p>
            To make good technology accessible: practical, secure and well supported, so that our customers can
            focus on what they do best.
          </p>
          <h2>The Student Project Platform</h2>
          <p>
            Alongside our services for all customers, we operate a dedicated platform for university and college
            students. It is a separate product with its own purpose, described on the{" "}
            <Link href="/student-platform">Student Platform page</Link>.
          </p>
        </div>
      </section>
      <section className="section section-alt">
        <div className="container">
          <h2>Our values</h2>
          <div className="grid grid-4">
            {VALUES.map((value) => (
              <div key={value.title} className="card">
                <h3>{value.title}</h3>
                <p>{value.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
