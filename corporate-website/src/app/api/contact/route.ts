import { InquiryValues } from "@/lib/inquiry";
import { buildInquiryEmailBody, getServiceTitle } from "@/lib/email";
import { submitGuestInquiry } from "@/lib/api";
import { validateInquiry, toGuestInquiryPayload } from "@/lib/inquiry";

export async function POST(request: Request) {
  const formData = await request.formData();

  // Extract form values
  const rawValues: InquiryValues = {
    name: formData.get("name") as string,
    email: formData.get("email") as string,
    phone: formData.get("phone") as string,
    service: formData.get("service") as string,
    message: formData.get("message") as string,
  };

  // Validate
  const errors = validateInquiry(rawValues);
  if (Object.keys(errors).length > 0) {
    return new Response(
      JSON.stringify({ success: false, errors }),
      { headers: { "Content-Type": "application/json" }, status: 400 }
    );
  }

  // Build email body and send email
  const emailBody = buildInquiryEmailBody({
    name: rawValues.name,
    email: rawValues.email,
    phone: rawValues.phone,
    service: rawValues.service,
    message: rawValues.message,
  });

  const emailSent = await sendServiceRequestEmail(emailBody, rawValues);

  // Also try to submit to backend
  let backendSuccess = false;
  try {
    await submitGuestInquiry(toGuestInquiryPayload(rawValues));
    backendSuccess = true;
  } catch (backendError) {
    console.error("Backend inquiry submission failed:", backendError);
    // Continue even if backend fails; email is the critical path for notifications
  }

  if (emailSent && backendSuccess) {
    return new Response(
      JSON.stringify({ success: true, type: "both" }),
      { headers: { "Content-Type": "application/json" }, status: 200 }
    );
  } else if (emailSent) {
    return new Response(
      JSON.stringify({ success: true, type: "email" }),
      { headers: { "Content-Type": "application/json" }, status: 200 }
    );
  } else if (backendSuccess) {
    return new Response(
      JSON.stringify({ success: true, type: "backend" }),
      { headers: { "Content-Type": "application/json" }, status: 200 }
    );
  } else {
    return new Response(
      JSON.stringify({ success: false, error: "Neither email nor backend submission succeeded." }),
      { headers: { "Content-Type": "application/json" }, status: 500 }
    );
  }
}

async function sendServiceRequestEmail(emailBody: string, values: InquiryValues): Promise<boolean> {
  const config = {
    host: process.env.SMTP_HOST ?? "",
    port: Number(process.env.SMTP_PORT) ?? 587,
    user: process.env.SMTP_USER ?? "",
    password: process.env.SMTP_PASSWORD ?? "",
  };

  if (!config.host || !config.user || !config.password) {
    console.warn("SMTP configuration incomplete; skipping email send.");
    return false;
  }

  const serviceTitle = getServiceTitle(values.service);

  const mailOptions = {
    from: `"AB IT Tech Solution" <${config.user}>`,
    to: "abelkomiga1815@gmail.com",
    subject: `${serviceTitle} - Website inquiry`,
    text: emailBody,
  };

  try {
    const nodemailer = await import("nodemailer");
    const transporter = nodemailer.createTransport({
      host: config.host,
      port: config.port,
      secure: config.port === 465,
      auth: {
        user: config.user,
        pass: config.password,
      },
    });

    await transporter.sendMail(mailOptions);
    console.log("Service request email sent successfully.");
    return true;
  } catch (error) {
    console.error("Failed to send service request email:", error);
    return false;
  }
}