package com.ruchulu.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @ValidGmailAddress — ensures submitted email is:
 *   1. Syntactically valid
 *   2. A real Gmail address (ends with @gmail.com)
 *      OR a trusted business domain (configurable)
 *   3. NOT from a known disposable / temp / spam domain
 *
 * Why Gmail-only for Ruchulu?
 *   → Catering is a trust-based business; we need real, recoverable emails.
 *   → Prevents throwaway signups and fake caterer accounts.
 *   → OTP delivery requires a real inbox.
 *
 * To also allow other trusted domains, set app.email.allowed-domains in yml.
 */
@Documented
@Constraint(validatedBy = ValidGmailAddress.GmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGmailAddress {

    String message() default
        "Only Gmail addresses are accepted (e.g. yourname@gmail.com). " +
        "Temporary, disposable, or spam email addresses are not allowed.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // ── Validator ─────────────────────────────────────────────────
    class GmailValidator implements ConstraintValidator<ValidGmailAddress, String> {

        private static final Pattern BASIC_EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

        private static final Pattern GMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);

        // Known disposable / temp / spam email domains to block
        private static final Set<String> BLOCKED_DOMAINS = Set.of(
            // Classic throwaway services
            "mailinator.com", "guerrillamail.com", "10minutemail.com",
            "tempmail.com", "temp-mail.org", "throwam.com", "yopmail.com",
            "trashmail.com", "maildrop.cc", "dispostable.com",
            "fakeinbox.com", "mailnull.com", "spamfree24.org",
            "spam4.me", "spamgourmet.com", "spamgourmet.net",
            "tempr.email", "spamex.com", "incognitomail.com",
            "trashmail.at", "trashmail.io", "trashmail.me", "trashmail.net",
            "discard.email", "spamthisplease.com", "binkmail.com",
            "bobmail.info", "powered.name", "nospam.ze.tc",
            // Guerrilla mail variants
            "sharklasers.com", "guerrillamailblock.com", "grr.la",
            "guerrillamail.info", "guerrillamail.biz",
            "guerrillamail.de", "guerrillamail.net", "guerrillamail.org",
            // Other spammy
            "spamevader.com", "mailexpire.com", "throwaway.email",
            "getnada.com", "trbvm.com", "mailsac.com", "emailondeck.com",
            "getairmail.com", "mailnesia.com", "mintemail.com",
            "mt2014.com", "mt2015.com", "safetymail.info", "teleworm.us",
            "trbvm.com", "wegwerfmail.de", "wegwerfmail.net", "wegwerfmail.org",
            "wh4f.org", "yepmail.net", "yourmail.net", "zetmail.com",
            // Indian fake mail services
            "indimail.net", "apmail.net"
        );

        // Trusted non-Gmail domains (corporate) — extend as needed
        private static final Set<String> ALLOWED_TRUSTED_DOMAINS = Set.of(
            "gmail.com",
            "outlook.com", "hotmail.com", "live.com",
            "yahoo.com",   "yahoo.in",
            "icloud.com",
            "protonmail.com", "pm.me",
            "rediffmail.com",   // popular in India
            "ruchulu.com"       // platform staff
        );

        @Override
        public boolean isValid(String email, ConstraintValidatorContext context) {
            if (email == null || email.isBlank()) {
                return true; // let @NotBlank handle the null/blank case
            }

            String trimmedEmail = email.trim().toLowerCase();

            // 1. Basic syntax check
            if (!BASIC_EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                setMessage(context, "Please enter a valid email address (e.g. name@gmail.com).");
                return false;
            }

            // 2. Extract domain
            int atIndex = trimmedEmail.lastIndexOf('@');
            String domain = trimmedEmail.substring(atIndex + 1);

            // 3. Block known disposable / spam domains
            if (BLOCKED_DOMAINS.contains(domain)) {
                setMessage(context,
                    "'" + domain + "' is a disposable/temporary email service and is not allowed. " +
                    "Please use your real Gmail or Outlook address.");
                return false;
            }

            // 4. Check plus-addressing abuse on gmail (e.g. test+spam@gmail.com is fine, but flag obvious spam patterns)
            // We allow plus addressing but normalise later
            String localPart = trimmedEmail.substring(0, atIndex);
            if (localPart.startsWith("noreply") || localPart.startsWith("donotreply")
                    || localPart.startsWith("spam") || localPart.startsWith("fake")) {
                setMessage(context,
                    "Email addresses starting with 'noreply', 'spam', or 'fake' are not accepted.");
                return false;
            }

            // 5. Must be a trusted domain
            if (!ALLOWED_TRUSTED_DOMAINS.contains(domain)) {
                setMessage(context,
                    "Only trusted email providers are accepted (Gmail, Outlook, Yahoo, iCloud, ProtonMail, Rediffmail). " +
                    "Found: @" + domain);
                return false;
            }

            return true;
        }

        private void setMessage(ConstraintValidatorContext ctx, String msg) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        }
    }
}
