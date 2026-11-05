#!/usr/bin/env python3
"""Cross-project consistency checks. Needs only Python 3 and no running services.

Verifies that the five projects agree with each other (service catalogue, enums, error codes, the
AI contract, environment variable names, migrations) and that nothing risky was left in the tree.
Run from the repository root:  python3 scripts/verify-consistency.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESULTS: list[tuple[bool, str]] = []


def check(ok: bool, description: str, detail: str = "") -> None:
    RESULTS.append((ok, description + (f"  [{detail}]" if detail and not ok else "")))


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def java_enum_values(relative: str) -> list[str]:
    body = re.search(r"enum\s+\w+\s*\{(.*?)(?:;|\n\})", read(relative), re.S).group(1)
    return re.findall(r"\b([A-Z][A-Z_]+)\b(?=\s*(?:,|$|\())", re.sub(r"//.*", "", body), re.M)


def ts_values(source: str, constant: str) -> list[str]:
    block = re.search(rf"export const {constant}[^=]*=\s*\[(.*?)\n\];", source, re.S).group(1)
    return re.findall(r'value:\s*"([A-Z_]+)"', block)


BACKEND = "backend/src/main/java/com/abittech/platform"

# ---- 1. service catalogue: backend seed == website data ----
migration = read("backend/src/main/resources/db/migration/V5__create_services.sql")
backend_services = re.findall(r"\('[0-9a-f-]{36}',\s*'([a-z0-9-]+)',\s*'([^']+)'", migration)
site = read("corporate-website/src/data/services.ts")
site_services = list(zip(re.findall(r'slug:\s*"([a-z0-9-]+)"', site), re.findall(r'title:\s*"([^"]+)"', site)))
check(len(backend_services) == 7, "backend seeds exactly 7 services", str(len(backend_services)))
check(backend_services == site_services, "service slugs and names are identical in the backend seed and the website")
check(not re.search(r"cloud|consultancy", migration + site, re.I), "no Cloud Solutions / ICT Consultancy service anywhere")

# ---- 2. enums: backend == Student Platform ----
constants = read("student-platform/src/lib/constants.ts")
pairs = [
    ("ProjectStage", "PROJECT_STAGES", f"{BACKEND}/project/entity/ProjectStage.java"),
    ("ProjectStatus", "PROJECT_STATUSES", f"{BACKEND}/project/entity/ProjectStatus.java"),
    ("DocumentType", "DOCUMENT_TYPES", f"{BACKEND}/document/entity/DocumentType.java"),
    ("DefenseCategory", "DEFENSE_CATEGORIES", f"{BACKEND}/defense/entity/DefenseCategory.java"),
]
for name, constant, java_file in pairs:
    check(java_enum_values(java_file) == ts_values(constants, constant), f"{name}: backend enum equals the Student Platform list")

role_values = java_enum_values(f"{BACKEND}/user/entity/Role.java")
check(role_values == ["STUDENT", "CUSTOMER", "ADMIN"], "roles are STUDENT, CUSTOMER, ADMIN", str(role_values))

# ---- 3. SQL check constraints agree with the Java enums ----
all_sql = "\n".join(p.read_text() for p in sorted((ROOT / "backend/src/main/resources/db/migration").glob("V*.sql")))
for java_file, column_hint in [(f"{BACKEND}/project/entity/ProjectStage.java", "TOPIC_SELECTION"),
                               (f"{BACKEND}/project/entity/ProjectStatus.java", "PLANNING"),
                               (f"{BACKEND}/document/entity/DocumentType.java", "THESIS_DRAFT"),
                               (f"{BACKEND}/defense/entity/DefenseCategory.java", "METHODOLOGY"),
                               (f"{BACKEND}/servicerequest/entity/ServiceRequestStatus.java", "IN_REVIEW"),
                               (f"{BACKEND}/inquiry/entity/InquiryStatus.java", "IN_PROGRESS")]:
    missing = [v for v in java_enum_values(java_file) if f"'{v}'" not in all_sql]
    check(not missing, f"every value of {Path(java_file).stem} appears in a SQL check constraint", str(missing))

# ---- 4. error codes used by the frontends exist in the backend ----
backend_codes = set(re.findall(r"^\s+([A-Z_]+)\(HttpStatus", read(f"{BACKEND}/common/exception/ErrorCode.java"), re.M))
for app in ("student-platform", "corporate-website"):
    used = set()
    for path in (ROOT / app / "src").rglob("*.ts*"):
        used |= set(re.findall(r'"([A-Z]{2,}(?:_[A-Z]+)+)"', path.read_text()))
    known_local = {"NOT_A_STUDENT_ACCOUNT", "SERVICE_UNAVAILABLE", "UNKNOWN_ERROR", "NETWORK_ERROR", "HEALTH_CHECK_FAILED",
                   "BAD_REQUEST", "RESOURCE_NOT_FOUND"}
    # only codes that look like backend codes: must be known to the backend unless generated locally
    unknown = {c for c in used if c.endswith(("_NOT_FOUND", "_ALREADY_EXISTS", "_TOO_FREQUENT")) and c not in backend_codes | known_local}
    check(not unknown, f"{app}: every *_NOT_FOUND / *_ALREADY_EXISTS code it checks exists in the backend", str(unknown))
check({"STUDENT_PROFILE_NOT_FOUND", "RATE_LIMITED", "ANALYSIS_TOO_FREQUENT", "AI_SERVICE_UNAVAILABLE"} <= backend_codes,
      "backend defines the codes the platform relies on")

# ---- 5. AI contract: backend client <-> AI service <-> Student Platform types ----
ai_client = read(f"{BACKEND}/ai/AiClient.java")
ai_routes = read("ai-service/app/api/routes.py")
check('"/api/v1/research/report"' in ai_client.replace("REPORT_PATH = ", "") or "/api/v1/research/report" in ai_client,
      "backend AiClient calls /api/v1/research/report")
check('@router.post("/report")' in ai_routes and 'prefix="/api/v1/research"' in ai_routes, "AI service exposes POST /api/v1/research/report")
check('"X-API-Key"' in ai_client and "x_api_key" in read("ai-service/app/api/deps.py"), "API key header is X-API-Key on both sides")
builder = read("ai-service/app/reports/builder.py")
for key in ("retrieved", "analysis", "papersFound", "relatedWork", "gapAnalysis", "similarity", "notices"):
    check(f'"{key}"' in builder, f"AI report contains '{key}'")
research_service = read(f"{BACKEND}/research/service/ResearchService.java")
for path in ('path("analysis").path("similarity").path("level")', 'path("retrieved").path("papersFound")'):
    check(path in research_service, f"backend reads {path}")
ai_levels = set(re.findall(r'= "([a-z_]+)"', read("ai-service/app/similarity/indicator.py")))
check({"low", "moderate", "high", "insufficient_data"} <= ai_levels, "AI similarity levels are low/moderate/high/insufficient_data")
check("'low', 'moderate', 'high', 'insufficient_data'" in read("backend/src/main/resources/db/migration/V11__create_research.sql"),
      "database check constraint accepts exactly the AI similarity levels")
types_ts = read("student-platform/src/lib/types.ts")
check('"low" | "moderate" | "high" | "insufficient_data"' in types_ts, "Student Platform types use the same similarity levels")
for status in ("ok", "no_results", "rate_limited", "unavailable"):
    check(f'"{status}"' in types_ts and f'"{status}"' in read("ai-service/app/search/models.py").replace("= ", ""),
          f"source status '{status}' known to the AI service and the Student Platform")

# ---- 6. environment variables line up ----
compose = read("docker-compose.yml")
root_env = read(".env.example")
for var in ("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD", "JWT_SECRET", "AI_SERVICE_API_KEY", "AI_SERVICE_URL",
            "CORS_ALLOWED_ORIGINS", "DOCUMENTS_DIR"):
    check(var in compose and var in read("backend/src/main/resources/application.yml") + read("backend/.env.example"),
          f"{var} is used by compose and by the backend configuration")
check("AI_SERVICE_API_KEY" in read("ai-service/app/core/config.py"), "AI service reads AI_SERVICE_API_KEY")
for var in re.findall(r"\$\{([A-Z_]+)(?::[-?][^}]*)?\}", compose):
    check(var in root_env or var in {"POSTGRES_USER"}, f"compose variable {var} is documented in .env.example")
check("BACKEND_URL" in compose and "BACKEND_URL" in read("student-platform/.env.example"), "BACKEND_URL wiring matches")
check("NEXT_PUBLIC_API_BASE_URL" in compose and "NEXT_PUBLIC_API_BASE_URL" in read("corporate-website/Dockerfile"),
      "website API base URL is passed as a build argument")

# ---- 7. migrations are continuous ----
versions = sorted(int(m.group(1)) for p in (ROOT / "backend/src/main/resources/db/migration").glob("V*__*.sql")
                  if (m := re.match(r"V(\d+)__", p.name)))
check(versions == list(range(1, len(versions) + 1)), "Flyway versions are continuous with no duplicates", str(versions))
check(not re.search(r"\b(DROP\s+(DATABASE|SCHEMA|TABLE)|TRUNCATE)\b", re.sub(r"--.*", "", all_sql), re.I),
      "no destructive SQL in any migration")

# ---- 8. security hygiene across the tree ----
skip = {"node_modules", ".next", "target", "__pycache__", ".git"}
files = [p for p in ROOT.rglob("*") if p.is_file() and not (set(p.parts) & skip)]
check(not [p for p in files if p.name == ".env" or p.name.startswith(".env.") and p.name != ".env.example"], "no .env files in the tree")
check(not [p for p in files if p.suffix in (".class", ".jar")], "no compiled artifacts in the tree")
frontend_sources = [p for p in files if p.suffix in (".ts", ".tsx") and ("student-platform" in p.parts or "corporate-website" in p.parts)]
check(not [p for p in frontend_sources if "dangerouslySetInnerHTML" in p.read_text()], "no dangerouslySetInnerHTML in the frontends")
check(not [p for p in frontend_sources if re.search(r"localStorage|sessionStorage", p.read_text()) and not p.name.endswith(".test.ts")],
      "frontends never use localStorage/sessionStorage (the token lives in an httpOnly cookie)")
java_main = [p for p in files if p.suffix == ".java" and "src/main" in p.as_posix()]
check(not [p for p in java_main if "@Autowired" in p.read_text()], "no field injection in backend main code")
secret_pattern = re.compile(r"""(?i)(password|secret|api[_-]?key|token)\s*[:=]\s*["']?(?!\$\{|your_|replace|test-only|""|\s|<)[A-Za-z0-9+/=_-]{16,}""")
suspicious = []
for p in files:
    if p.suffix in (".yml", ".yaml", ".properties", ".env", ".example", ".sh", ".py", ".ts", ".tsx", ".java", ".xml", ".md"):
        for m in secret_pattern.finditer(p.read_text(errors="ignore")):
            line = m.group(0)
            if "test" in p.as_posix().lower() or "tests/" in p.as_posix():
                continue
            suspicious.append(f"{p.relative_to(ROOT)}: {line[:50]}")
check(not suspicious, "no hard-coded secrets outside test configuration", "; ".join(suspicious[:3]))
check(all((ROOT / d / "README.md").exists() for d in ("backend", "ai-service", "corporate-website", "student-platform")), "every project has a README")
check(all((ROOT / f).exists() for f in ("docs/DEPLOYMENT.md", "docs/SECURITY-REVIEW.md", "docs/TESTING.md", "docs/ARCHITECTURE.md")),
      "system documentation exists")

failed = [description for ok, description in RESULTS if not ok]
for ok, description in RESULTS:
    print(("PASS  " if ok else "FAIL  ") + description)
print(f"\n{len(RESULTS) - len(failed)} of {len(RESULTS)} checks passed")
sys.exit(1 if failed else 0)
