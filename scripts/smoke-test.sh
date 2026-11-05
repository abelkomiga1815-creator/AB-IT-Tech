#!/usr/bin/env bash
# End-to-end smoke test against a RUNNING system (docker compose up -d --build).
#   scripts/smoke-test.sh                       # defaults to the local compose ports
#   API=https://api.example.org STUDENT=https://students.example.org SITE=https://example.org scripts/smoke-test.sh
# Needs: bash, curl, python3. Creates a few throw-away test accounts (random e-mail addresses) in the database.
# Exit code 0 = all checks passed, 1 = at least one failed.

API="${API:-http://localhost:8080}"
STUDENT="${STUDENT:-http://localhost:3001}"
SITE="${SITE:-http://localhost:3000}"
PASSWORD="Smoke-Test-Passw0rd"
RUN_ID="$(date +%s)-$RANDOM"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
PASS=0; FAIL=0

ok()   { PASS=$((PASS + 1)); printf '  PASS  %s\n' "$1"; }
bad()  { FAIL=$((FAIL + 1)); printf '  FAIL  %s  (%s)\n' "$1" "$2"; }
check() { # description expected actual
  if [ "$2" = "$3" ]; then ok "$1"; else bad "$1" "expected '$2', got '$3'"; fi
}
json() { python3 -c 'import sys,json
try:
    d=json.load(sys.stdin)
    for k in sys.argv[1].split("."):
        d=d[int(k)] if k.isdigit() else d[k]
    print(d if not isinstance(d,bool) else str(d).lower())
except Exception:
    print("")' "$1"; }
code() { curl -s -o "$TMP/body" -w '%{http_code}' "$@"; }          # prints the HTTP status, body in $TMP/body
body() { cat "$TMP/body"; }

echo "== Backend: health, catalogue =="
check "GET /api/v1/health" "200" "$(code "$API/api/v1/health")"
check "health body says UP" "UP" "$(body | json status)"
check "GET /actuator/health" "200" "$(code "$API/actuator/health")"
check "GET /api/v1/services" "200" "$(code "$API/api/v1/services")"
check "catalogue has 7 services" "7" "$(body | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["data"]))')"

echo "== Student: register, login, profile, project =="
S_EMAIL="smoke-student-$RUN_ID@example.com"
check "register student" "201" "$(code -X POST "$API/api/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Smoke Student\",\"email\":\"$S_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"STUDENT\"}")"
check "login student" "200" "$(code -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$S_EMAIL\",\"password\":\"$PASSWORD\"}")"
S_TOKEN="$(body | json data.accessToken)"
A="Authorization: Bearer $S_TOKEN"
check "GET /auth/me" "200" "$(code "$API/api/v1/auth/me" -H "$A")"
check "protected endpoint without token is 401" "401" "$(code "$API/api/v1/student/projects")"
check "wrong password is 401" "401" "$(code -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$S_EMAIL\",\"password\":\"wrong-password\"}")"
check "create student profile" "201" "$(code -X POST "$API/api/v1/student/profile" -H "$A" -H 'Content-Type: application/json' \
  -d '{"university":"Smoke University","programme":"Computer Science","yearOfStudy":3}')"
check "create project" "201" "$(code -X POST "$API/api/v1/student/projects" -H "$A" -H 'Content-Type: application/json' \
  -d '{"title":"Crop disease detection on smartphones","description":"A mobile app that detects crop disease from leaf images."}')"
PROJECT="$(body | json data.id)"
P="$API/api/v1/student/projects/$PROJECT"
check "record progress" "201" "$(code -X POST "$P/progress" -H "$A" -H 'Content-Type: application/json' -d '{"stage":"PROPOSAL","percentComplete":20,"note":"smoke"}')"
check "project shows the progress" "20" "$(code "$P" -H "$A" >/dev/null; body | json data.progressPercent)"
printf 'smoke test document\n' > "$TMP/note.txt"
check "upload document" "201" "$(code -X POST "$P/documents" -H "$A" -F "file=@$TMP/note.txt" -F type=OTHER)"
DOC="$(body | json data.id)"
check "download document" "200" "$(curl -s -o "$TMP/downloaded.txt" -w '%{http_code}' "$P/documents/$DOC/download" -H "$A")"
check "downloaded bytes match" "smoke test document" "$(cat "$TMP/downloaded.txt")"
printf 'MZ' > "$TMP/bad.exe"
check "executable upload is refused (415)" "415" "$(code -X POST "$P/documents" -H "$A" -F "file=@$TMP/bad.exe")"
check "add defense question" "201" "$(code -X POST "$P/defense-questions" -H "$A" -H 'Content-Type: application/json' -d '{"question":"Why this method?","category":"METHODOLOGY"}')"

echo "== Research assistance (needs the AI service and internet access) =="
STATUS="$(code -m 120 -X POST "$P/research/analyses" -H "$A")"
case "$STATUS" in
  201) ok "research analysis ran (201)"
       check "analysis carries the disclaimer" "true" "$(body | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"]["report"];print(str("not a plagiarism check" in d["analysis"]["similarity"]["disclaimer"]).lower())')"
       check "second analysis right away is refused (429)" "429" "$(code -X POST "$P/research/analyses" -H "$A")" ;;
  502|503) printf '  SKIP  research analysis returned %s: the AI service or its internet access is not available\n' "$STATUS" ;;
  *) bad "research analysis" "unexpected status $STATUS: $(body | head -c 200)" ;;
esac

echo "== Customer: register, profile, inquiry, service request =="
C_EMAIL="smoke-customer-$RUN_ID@example.com"
code -X POST "$API/api/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Smoke Customer\",\"email\":\"$C_EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"CUSTOMER\"}" >/dev/null
code -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$C_EMAIL\",\"password\":\"$PASSWORD\"}" >/dev/null
C_TOKEN="$(body | json data.accessToken)"; CA="Authorization: Bearer $C_TOKEN"
check "create customer profile" "201" "$(code -X POST "$API/api/v1/customer/profile" -H "$CA" -H 'Content-Type: application/json' -d '{"customerType":"BUSINESS","organizationName":"Smoke Ltd"}')"
check "customer inquiry" "201" "$(code -X POST "$API/api/v1/customer/inquiries" -H "$CA" -H 'Content-Type: application/json' -d '{"serviceSlug":"cybersecurity","message":"We would like a security review."}')"
check "service request" "201" "$(code -X POST "$API/api/v1/customer/service-requests" -H "$CA" -H 'Content-Type: application/json' -d '{"serviceSlug":"web-development","title":"Company website","description":"A modern website with a contact form."}')"
check "guest inquiry (no login)" "201" "$(code -X POST "$API/api/v1/inquiries" -H 'Content-Type: application/json' -d '{"name":"Guest","email":"guest@example.com","message":"Smoke test inquiry from a visitor."}')"

echo "== Isolation and roles =="
check "student on a customer endpoint is 403" "403" "$(code "$API/api/v1/customer/profile" -H "$A")"
check "customer on a student endpoint is 403" "403" "$(code "$API/api/v1/student/projects" -H "$CA")"
check "customer cannot open the student's project (403)" "403" "$(code "$P" -H "$CA")"
check "customer on an admin endpoint is 403" "403" "$(code "$API/api/v1/admin/inquiries" -H "$CA")"
check "ADMIN cannot be chosen at registration (400)" "400" "$(code -X POST "$API/api/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Evil\",\"email\":\"evil-$RUN_ID@example.com\",\"password\":\"$PASSWORD\",\"role\":\"ADMIN\"}")"
echo "  (a second student cannot read the first student's project: covered by the backend test suite)"

echo "== Student Platform (BFF) =="
check "login page" "200" "$(code "$STUDENT/login")"
check "dashboard without a session redirects to /login" "307" "$(code "$STUDENT/dashboard")"
JAR="$TMP/cookies.txt"
check "BFF login" "200" "$(code -c "$JAR" -X POST "$STUDENT/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$S_EMAIL\",\"password\":\"$PASSWORD\"}")"
check "session cookie is httpOnly" "1" "$(grep -c '#HttpOnly_' "$JAR")"
check "token is not in the login response" "" "$(body | json data.accessToken)"
check "proxied /auth/me with the cookie" "200" "$(code -b "$JAR" "$STUDENT/api/backend/auth/me")"
check "proxied call without cookie is 401" "401" "$(code "$STUDENT/api/backend/auth/me")"
check "proxy blocks customer endpoints (404)" "404" "$(code -b "$JAR" "$STUDENT/api/backend/customer/profile")"
check "proxy blocks traversal (404)" "404" "$(code -b "$JAR" --path-as-is "$STUDENT/api/backend/student/../admin/inquiries")"
check "customer account cannot sign in to the student app (403)" "403" "$(code -X POST "$STUDENT/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$C_EMAIL\",\"password\":\"$PASSWORD\"}")"
check "logout" "200" "$(code -b "$JAR" -c "$JAR" -X POST "$STUDENT/api/auth/logout")"

echo "== Corporate website =="
check "home page" "200" "$(code "$SITE/")"
check "services page" "200" "$(code "$SITE/services")"
check "service detail page" "200" "$(code "$SITE/services/cybersecurity")"
check "contact page" "200" "$(code "$SITE/contact")"
check "unknown page is 404" "404" "$(code "$SITE/does-not-exist")"
ORIGIN="${SITE_ORIGIN:-$SITE}"
check "CORS: the website origin may call the API" "$ORIGIN" "$(curl -s -D - -o /dev/null -X OPTIONS "$API/api/v1/inquiries" -H "Origin: $ORIGIN" \
  -H 'Access-Control-Request-Method: POST' | tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin"{print $2}')"
check "CORS: a foreign origin is refused (403)" "403" "$(code -X OPTIONS "$API/api/v1/inquiries" -H 'Origin: http://evil.example' -H 'Access-Control-Request-Method: POST')"

echo
echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
