export type ServiceIcon =
  | "code"
  | "globe"
  | "phone"
  | "shield"
  | "chart"
  | "pen"
  | "wrench";

export interface Service {
  slug: string;
  title: string;
  summary: string;
  description: string;
  features: string[];
  icon: ServiceIcon;
}

export const SERVICES: Service[] = [
  {
    slug: "software-development",
    title: "Software Development",
    summary: "Custom applications built for your workflow, from desktop tools to complex enterprise systems.",
    description:
      "We design and develop custom software tailored to your exact requirements. Whether you need a desktop utility, a backend service, or a full enterprise platform, we use modern languages and frameworks to deliver maintainable, scalable solutions.",
    features: [
      "Requirements analysis and architecture design",
      "Clean, tested code with modern tooling",
      "CI/CD pipelines and automated deployments",
      "Documentation and knowledge transfer",
      "Ongoing maintenance and support options",
    ],
    icon: "code",
  },
  {
    slug: "web-development",
    title: "Web Development",
    summary: "Fast, accessible websites and web applications that work on every device.",
    description:
      "We build responsive, performant web experiences using React, Next.js, and modern web standards. From marketing sites to data-driven web apps, we focus on accessibility, SEO, and developer experience.",
    features: [
      "Responsive, mobile-first design",
      "Server-side rendering and static generation",
      "Performance optimization and Core Web Vitals",
      "Accessibility (WCAG 2.1 AA) compliance",
      "Content management system integration",
    ],
    icon: "globe",
  },
  {
    slug: "mobile-development",
    title: "Mobile Development",
    summary: "Native and cross-platform mobile apps for iOS and Android.",
    description:
      "We create mobile applications that feel native on both platforms. Using React Native and Expo, we share logic across iOS and Android while delivering platform-appropriate interfaces and performance.",
    features: [
      "Cross-platform code sharing with React Native",
      "Native-like performance and gestures",
      "App Store and Play Store deployment support",
      "Push notifications and offline capabilities",
      "Over-the-air updates with Expo Updates",
    ],
    icon: "phone",
  },
  {
    slug: "cybersecurity",
    title: "Cybersecurity",
    summary: "Security assessments, hardening, and incident response to protect your data and systems.",
    description:
      "We help you identify and mitigate risks before they become incidents. Our services include vulnerability scanning, penetration testing, secure code reviews, and security architecture consulting.",
    features: [
      "Vulnerability assessments and penetration testing",
      "Secure code review and threat modeling",
      "Security architecture and compliance guidance",
      "Incident response planning and tabletop exercises",
      "Security awareness training for teams",
    ],
    icon: "shield",
  },
  {
    slug: "data-analysis",
    title: "Data Analysis",
    summary: "Turn raw data into actionable insights with dashboards, reports, and predictive models.",
    description:
      "We help you make sense of your data. From exploratory analysis to production dashboards and machine learning models, we build data pipelines and visualizations that drive decisions.",
    features: [
      "Data cleaning, transformation, and modeling",
      "Interactive dashboards (Metabase, Grafana, custom)",
      "Automated reporting and alerting",
      "Predictive analytics and forecasting",
      "Data governance and privacy best practices",
    ],
    icon: "chart",
  },
  {
    slug: "ui-ux-design",
    title: "UI/UX Design",
    summary: "User-centered design for products that are intuitive, accessible, and visually cohesive.",
    description:
      "We design interfaces that users understand immediately. Our process covers user research, wireframing, prototyping, usability testing, and design systems that scale across your product portfolio.",
    features: [
      "User research and journey mapping",
      "Wireframes, prototypes, and design systems",
      "Usability testing and iteration",
      "Accessibility-first design practices",
      "Handoff specifications for developers",
    ],
    icon: "pen",
  },
  {
    slug: "it-support",
    title: "IT Support",
    summary: "Reliable managed IT services, infrastructure, and help desk for organizations of any size.",
    description:
      "We keep your technology running so you can focus on your mission. Our managed services cover endpoint management, cloud infrastructure, backup and disaster recovery, and responsive help desk support.",
    features: [
      "24/7 monitoring and incident response",
      "Endpoint management and patching",
      "Cloud infrastructure (AWS, Azure, GCP)",
      "Backup, disaster recovery, and business continuity",
      "Strategic IT planning and vendor management",
    ],
    icon: "wrench",
  },
];

export function getServiceBySlug(slug: string): Service | undefined {
  return SERVICES.find((service) => service.slug === slug);
}