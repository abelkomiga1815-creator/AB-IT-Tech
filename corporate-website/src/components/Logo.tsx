import Link from "next/link";
import { LOGO_IS_PLACEHOLDER, LOGO_SRC, SITE_NAME } from "@/config/site";

/**
 * Site logo. Currently renders the clearly marked placeholder; once the official logo file is in
 * /public, change LOGO_SRC and LOGO_IS_PLACEHOLDER in src/config/site.ts.
 */
export function Logo() {
  return (
    <Link href="/" className="logo" aria-label={`${SITE_NAME} - home`}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={LOGO_SRC} alt={LOGO_IS_PLACEHOLDER ? `${SITE_NAME} (logo placeholder)` : SITE_NAME} width={160} height={40} />
    </Link>
  );
}
