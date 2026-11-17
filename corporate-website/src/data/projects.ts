export interface Project {
  slug: string;
  category: string;
  title: string;
  summary: string;
  outcome: string;
  description: string;
  technologies: string[];
  image: string;
}

export const PROJECTS: Project[] = [
  {
    slug: "student-project-management-platform",
    category: "EdTech",
    title: "Student Project Management Platform",
    summary: "A comprehensive platform for students to manage their academic projects, track progress, save research sources, and prepare for defenses.",
    outcome: "Streamlined project workflow for 200+ students with integrated research assistance and defense preparation tools.",
    description:
      "Built a full-stack student project management platform with role-based access (Student, Customer, Admin), project lifecycle tracking, research assistance powered by an external AI engine, document uploads, and defense question management. The platform includes a Next.js student portal, a Spring Boot backend API, and a Python AI research service that queries OpenAlex, Crossref, and Semantic Scholar for related work analysis.",
    technologies: ["Next.js", "TypeScript", "Spring Boot", "Java", "PostgreSQL", "Python", "FastAPI", "Tailwind CSS"],
    image: "/projects/student-platform.jpg",
  },
  {
    slug: "corporate-website-redesign",
    category: "Web Development",
    title: "Corporate Website Redesign",
    summary: "A modern, responsive corporate website with service catalog, project portfolio, inquiry forms, and contact management.",
    outcome: "Professional online presence with inquiry conversion and service showcase for a technology solutions provider.",
    description:
      "Designed and developed a responsive corporate website using Next.js with server-side rendering. Features include a dynamic service catalog with individual service pages, a project portfolio with case studies, inquiry and contact forms, and a student platform landing section. The site follows WCAG accessibility guidelines and is optimized for performance and SEO.",
    technologies: ["Next.js", "TypeScript", "Tailwind CSS", "React", "Vercel"],
    image: "/projects/corporate-website.jpg",
  },
  {
    slug: "cybersecurity-assessment-tool",
    category: "Cybersecurity",
    title: "Cybersecurity Assessment Tool",
    summary: "A vulnerability scanning and security assessment application for small and medium businesses.",
    outcome: "Identified and remediated critical vulnerabilities across 50+ client systems with automated reporting.",
    description:
      "Developed a cybersecurity assessment tool that performs vulnerability scanning, port scanning, and security configuration checks. The tool generates detailed reports with remediation recommendations and compliance status against common security frameworks. Includes role-based access for security analysts and clients.",
    technologies: ["Python", "Flask", "PostgreSQL", "Nmap", "Docker", "React"],
    image: "/projects/cybersecurity.jpg",
  },
  {
    slug: "data-analytics-dashboard",
    category: "Data Analysis",
    title: "Business Data Analytics Dashboard",
    summary: "An interactive analytics dashboard for sales and operations data with real-time visualizations.",
    outcome: "Reduced reporting time by 60% with automated dashboards and real-time data visualizations for management.",
    description:
      "Built a data analytics platform that connects to multiple data sources, performs ETL transformations, and serves interactive dashboards. Features include custom chart builders, scheduled reports, alerting on data anomalies, and export capabilities. Built with a Python backend and React frontend with charting libraries.",
    technologies: ["Python", "React", "PostgreSQL", "Docker", "Plotly", "Redis"],
    image: "/projects/analytics.jpg",
  },
];
