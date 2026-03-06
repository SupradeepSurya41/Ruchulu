package com.ruchulu.notificationservice.service;

import com.ruchulu.notificationservice.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * EmailTemplateService — builds HTML email bodies for every NotificationType.
 * Uses inline HTML with Ruchulu branding (dark brown theme, 🥄 emoji).
 * No external template engine dependency — self-contained.
 */
@Service
@Slf4j
public class EmailTemplateService {

    private static final String BRAND_COLOR  = "#5C3317";
    private static final String ACCENT_COLOR = "#8B5E3C";
    private static final String BG_COLOR     = "#FDF6F0";

    public String buildSubject(NotificationType type, Map<String, String> vars) {
        return switch (type) {
            case OTP_LOGIN            -> "Your Ruchulu Login OTP";
            case OTP_EMAIL_VERIFY     -> "Verify Your Ruchulu Email";
            case OTP_PASSWORD_RESET   -> "Reset Your Ruchulu Password";
            case ACCOUNT_CREATED      -> "Welcome to Ruchulu 🥄 — Account Created!";
            case ACCOUNT_VERIFIED     -> "Email Verified — You're All Set!";
            case PASSWORD_CHANGED     -> "Your Ruchulu Password Was Changed";
            case CATERER_PROFILE_SUBMITTED -> "Profile Submitted — Under Review";
            case CATERER_PROFILE_APPROVED  -> "🎉 Your Caterer Profile is Approved!";
            case CATERER_PROFILE_REJECTED  -> "Profile Review Update";
            case BOOKING_CREATED      -> "Booking Request Sent — Awaiting Confirmation";
            case BOOKING_CONFIRMED    -> "✅ Booking Confirmed — " + vars.getOrDefault("occasion", "Event");
            case BOOKING_REJECTED     -> "Booking Update — Request Not Accepted";
            case BOOKING_CANCELLED    -> "Booking Cancelled";
            case BOOKING_COMPLETED    -> "Event Completed — Share Your Experience!";
            case BOOKING_EXPIRED      -> "Booking Expired — Book Again?";
            case BOOKING_REMINDER_24H -> "⏰ Reminder: Your Event is Tomorrow!";
            case BOOKING_REMINDER_1H  -> "⏰ Reminder: Your Event Starts in 1 Hour!";
            case NEW_BOOKING_REQUEST  -> "📩 New Booking Request Received";
            case BOOKING_CANCELLED_BY_CUSTOMER -> "Booking Cancelled by Customer";
            case PAYMENT_RECEIVED     -> "💰 Payment Received — ₹" + vars.getOrDefault("amount", "");
            case REVIEW_RECEIVED      -> "⭐ New Review for Your Catering Service";
            default                   -> "Ruchulu Notification";
        };
    }

    public String buildBody(NotificationType type, Map<String, String> vars) {
        String content = switch (type) {
            case OTP_LOGIN, OTP_EMAIL_VERIFY, OTP_PASSWORD_RESET -> buildOtpBody(type, vars);
            case ACCOUNT_CREATED      -> buildWelcomeBody(vars);
            case CATERER_PROFILE_APPROVED -> buildApprovalBody(vars);
            case CATERER_PROFILE_REJECTED -> buildRejectionBody(vars);
            case BOOKING_CREATED      -> buildBookingCreatedBody(vars);
            case BOOKING_CONFIRMED    -> buildBookingConfirmedBody(vars);
            case BOOKING_REJECTED     -> buildBookingRejectedBody(vars);
            case BOOKING_CANCELLED    -> buildBookingCancelledBody(vars);
            case BOOKING_COMPLETED    -> buildBookingCompletedBody(vars);
            case BOOKING_REMINDER_24H, BOOKING_REMINDER_1H -> buildReminderBody(type, vars);
            case NEW_BOOKING_REQUEST  -> buildNewBookingRequestBody(vars);
            case PAYMENT_RECEIVED     -> buildPaymentBody(vars);
            case REVIEW_RECEIVED      -> buildReviewBody(vars);
            default                   -> buildGenericBody(vars);
        };
        return wrapInLayout(content);
    }

    // ── OTP EMAIL ─────────────────────────────────────────────────────────
    private String buildOtpBody(NotificationType type, Map<String, String> vars) {
        String name    = vars.getOrDefault("name", "there");
        String otp     = vars.getOrDefault("otp", "------");
        String expiry  = vars.getOrDefault("expiryMinutes", "10");
        String purpose = switch (type) {
            case OTP_LOGIN          -> "sign in to your Ruchulu account";
            case OTP_EMAIL_VERIFY   -> "verify your email address";
            case OTP_PASSWORD_RESET -> "reset your password";
            default                 -> "complete your request";
        };
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>Use the following OTP to %s:</p>
            <div style="text-align:center;margin:30px 0;">
              <div style="background:linear-gradient(135deg,%s,%s);color:#fff;
                          font-size:36px;font-weight:bold;letter-spacing:12px;
                          padding:20px 40px;border-radius:12px;display:inline-block;">
                %s
              </div>
            </div>
            <p style="text-align:center;color:#888;">This OTP expires in <strong>%s minutes</strong>.</p>
            <p style="color:#c0392b;font-size:13px;">
              ⚠️ Never share this OTP with anyone. Ruchulu will never ask for your OTP.
            </p>
            """.formatted(name, purpose, BRAND_COLOR, ACCENT_COLOR, otp, expiry);
    }

    // ── WELCOME EMAIL ─────────────────────────────────────────────────────
    private String buildWelcomeBody(Map<String, String> vars) {
        String name = vars.getOrDefault("name", "there");
        return """
            <p>Hi <strong>%s</strong>, welcome to <strong>Ruchulu 🥄</strong>!</p>
            <p>Your account has been created successfully. You can now:</p>
            <ul>
              <li>🔍 Browse and book top-rated caterers in your city</li>
              <li>📅 Manage your bookings from one place</li>
              <li>⭐ Leave reviews after your events</li>
            </ul>
            <p>Start exploring great catering in Hyderabad, Vijayawada, Visakhapatnam and more!</p>
            """.formatted(name);
    }

    // ── CATERER APPROVED ──────────────────────────────────────────────────
    private String buildApprovalBody(Map<String, String> vars) {
        String name = vars.getOrDefault("catererName", "there");
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>🎉 Great news! Your caterer profile on <strong>Ruchulu</strong> has been
            <span style="color:green;font-weight:bold;">approved</span>!</p>
            <p>Your profile is now live and customers can book your services. Make sure your
            menu and availability are up to date.</p>
            <p>Here are your next steps:</p>
            <ul>
              <li>✅ Complete your menu items</li>
              <li>📷 Upload quality photos of your food</li>
              <li>📞 Keep your phone accessible for booking confirmations</li>
            </ul>
            """.formatted(name);
    }

    // ── CATERER REJECTED ──────────────────────────────────────────────────
    private String buildRejectionBody(Map<String, String> vars) {
        String name   = vars.getOrDefault("catererName", "there");
        String reason = vars.getOrDefault("reason", "Please review our guidelines and reapply.");
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>Thank you for applying to Ruchulu. After reviewing your profile, we were unable
            to approve it at this time.</p>
            <p><strong>Reason:</strong> %s</p>
            <p>You are welcome to update your profile and reapply. If you have questions,
            please contact our support team.</p>
            """.formatted(name, reason);
    }

    // ── BOOKING CREATED ───────────────────────────────────────────────────
    private String buildBookingCreatedBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>Your booking request has been sent to <strong>%s</strong>. Here are the details:</p>
            %s
            <p>The caterer will confirm within <strong>48 hours</strong>. You will receive an email
            once confirmed.</p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("catererName", "the caterer"),
                buildBookingTable(vars));
    }

    // ── BOOKING CONFIRMED ─────────────────────────────────────────────────
    private String buildBookingConfirmedBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>✅ Your booking has been <span style="color:green;font-weight:bold;">confirmed</span>
            by <strong>%s</strong>!</p>
            %s
            <p>Please pay the advance amount of <strong>₹%s</strong> to secure your booking.</p>
            %s
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("catererName", "the caterer"),
                buildBookingTable(vars),
                vars.getOrDefault("advanceAmount", ""),
                vars.containsKey("catererNotes")
                    ? "<p><strong>Caterer notes:</strong> " + vars.get("catererNotes") + "</p>"
                    : "");
    }

    // ── BOOKING REJECTED ──────────────────────────────────────────────────
    private String buildBookingRejectedBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>Unfortunately, <strong>%s</strong> was unable to accept your booking request.</p>
            <p><strong>Reason:</strong> %s</p>
            <p>Don't worry — many other great caterers are available! Browse and book another
            caterer at <a href="https://ruchulu.netlify.app">ruchulu.netlify.app</a>.</p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("catererName", "the caterer"),
                vars.getOrDefault("reason", "Not specified"));
    }

    // ── BOOKING CANCELLED ─────────────────────────────────────────────────
    private String buildBookingCancelledBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>Your booking (ID: <code>%s</code>) for <strong>%s</strong> on <strong>%s</strong>
            has been <span style="color:#c0392b;font-weight:bold;">cancelled</span>.</p>
            <p><strong>Reason:</strong> %s</p>
            <p>If a refund is applicable, it will be processed within 5–7 business days.</p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("bookingId", ""),
                vars.getOrDefault("occasion", "your event"),
                vars.getOrDefault("eventDate", ""),
                vars.getOrDefault("reason", "Not specified"));
    }

    // ── BOOKING COMPLETED ─────────────────────────────────────────────────
    private String buildBookingCompletedBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>We hope your <strong>%s</strong> was a wonderful success! 🎉</p>
            <p>Please take a moment to rate and review <strong>%s</strong>.
            Your feedback helps other customers make great choices.</p>
            <p style="text-align:center;margin:20px 0;">
              <a href="https://ruchulu.netlify.app/bookings/%s/review"
                 style="background:%s;color:#fff;padding:12px 28px;border-radius:8px;
                        text-decoration:none;font-weight:bold;">
                ⭐ Leave a Review
              </a>
            </p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("occasion", "event"),
                vars.getOrDefault("catererName", "the caterer"),
                vars.getOrDefault("bookingId", ""),
                BRAND_COLOR);
    }

    // ── REMINDER ──────────────────────────────────────────────────────────
    private String buildReminderBody(NotificationType type, Map<String, String> vars) {
        String timeLabel = type == NotificationType.BOOKING_REMINDER_24H ? "tomorrow" : "in 1 hour";
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>⏰ This is a friendly reminder that your <strong>%s</strong> event is
            <strong>%s</strong>!</p>
            %s
            <p>Make sure everything is ready. If you have any last-minute requests,
            contact the caterer directly.</p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("occasion", "upcoming"),
                timeLabel,
                buildBookingTable(vars));
    }

    // ── NEW BOOKING REQUEST (for caterer) ─────────────────────────────────
    private String buildNewBookingRequestBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>📩 You have a new booking request! Please review and respond within
            <strong>48 hours</strong>.</p>
            %s
            <p style="text-align:center;margin:20px 0;">
              <a href="https://ruchulu.netlify.app/caterer/bookings"
                 style="background:%s;color:#fff;padding:12px 28px;border-radius:8px;
                        text-decoration:none;font-weight:bold;">
                View Booking Request
              </a>
            </p>
            """.formatted(
                vars.getOrDefault("catererName", "there"),
                buildBookingTable(vars),
                BRAND_COLOR);
    }

    // ── PAYMENT RECEIVED ──────────────────────────────────────────────────
    private String buildPaymentBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>💰 We have received your payment of <strong>₹%s</strong> for booking
            <code>%s</code>.</p>
            <p>Payment mode: <strong>%s</strong></p>
            <p>Transaction ID: <code>%s</code></p>
            <p>Thank you! Your booking is now secured.</p>
            """.formatted(
                vars.getOrDefault("customerName", "there"),
                vars.getOrDefault("amount", ""),
                vars.getOrDefault("bookingId", ""),
                vars.getOrDefault("paymentMode", ""),
                vars.getOrDefault("transactionId", "N/A"));
    }

    // ── REVIEW RECEIVED (for caterer) ─────────────────────────────────────
    private String buildReviewBody(Map<String, String> vars) {
        return """
            <p>Hi <strong>%s</strong>,</p>
            <p>⭐ You received a new <strong>%s-star</strong> review from
            <strong>%s</strong>!</p>
            <blockquote style="border-left:4px solid %s;padding-left:12px;
                               color:#555;font-style:italic;">
              "%s"
            </blockquote>
            <p>Log in to respond to the review and keep your rating strong.</p>
            """.formatted(
                vars.getOrDefault("catererName", "there"),
                vars.getOrDefault("rating", ""),
                vars.getOrDefault("reviewerName", "a customer"),
                BRAND_COLOR,
                vars.getOrDefault("comment", ""));
    }

    // ── GENERIC ───────────────────────────────────────────────────────────
    private String buildGenericBody(Map<String, String> vars) {
        return "<p>You have a new notification from Ruchulu. Please log in for details.</p>";
    }

    // ── BOOKING DETAIL TABLE ──────────────────────────────────────────────
    private String buildBookingTable(Map<String, String> vars) {
        return """
            <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
              <tr style="background:%s;color:#fff;">
                <th colspan="2" style="padding:10px;text-align:left;">Booking Details</th>
              </tr>
              %s%s%s%s%s%s
            </table>
            """.formatted(BRAND_COLOR,
                row("Occasion",   vars.get("occasion")),
                row("Date",       vars.get("eventDate")),
                row("City",       vars.get("eventCity")),
                row("Guests",     vars.get("guestCount")),
                row("Total",      vars.containsKey("totalAmount") ? "₹" + vars.get("totalAmount") : null),
                row("Booking ID", vars.get("bookingId")));
    }

    private String row(String label, String value) {
        if (value == null || value.isBlank()) return "";
        return """
            <tr style="border-bottom:1px solid #eee;">
              <td style="padding:8px 12px;color:#666;width:40%%;">%s</td>
              <td style="padding:8px 12px;font-weight:bold;">%s</td>
            </tr>
            """.formatted(label, value);
    }

    // ── LAYOUT WRAPPER ────────────────────────────────────────────────────
    private String wrapInLayout(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f4f4;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:30px 0;">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="background:#fff;border-radius:12px;overflow:hidden;
                                box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                      <td style="background:%s;padding:24px 32px;text-align:center;">
                        <h1 style="color:#fff;margin:0;font-size:26px;">Ruchulu 🥄</h1>
                        <p style="color:rgba(255,255,255,0.8);margin:4px 0 0;">
                          Bringing Great Catering to Your Doorstep
                        </p>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:32px;color:#333;font-size:15px;line-height:1.6;">
                        %s
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8f8f8;padding:20px 32px;text-align:center;
                                 color:#999;font-size:12px;border-top:1px solid #eee;">
                        <p style="margin:0;">© 2025 Ruchulu. All rights reserved.</p>
                        <p style="margin:4px 0 0;">
                          Serving Hyderabad, Vijayawada, Visakhapatnam &amp; more AP/TS cities.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(BRAND_COLOR, content);
    }
}
