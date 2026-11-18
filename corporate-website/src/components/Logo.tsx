import Link from "next/link";
import { LOGO_IS_PLACEHOLDER, LOGO_SRC, SITE_NAME } from "@/config/site";

/**
 * Site logo. Renders the official AB IT Tech Solutions logo from /public.
 * The logo background is transparent so the website header background shows through.
 */
export function Logo() {
  return (
    <Link href="/" className="logo" aria-label={`${SITE_NAME} - home`}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={LOGO_SRC}
        alt={LOGO_IS_PLACEHOLDER ? `${SITE_NAME} (logo placeholder)` : SITE_NAME}
        width={160}
        height={40}
        className="logo-img"
      />
    </Link>
  );
}
