package me.theoria.barlinex.domain.barcode;

import android.net.Uri;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class QrContentBuilder {

    private static final DateTimeFormatter EVENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private QrContentBuilder() {
        // Utility class.
    }

    public static String buildWifi(
            String ssid,
            String password,
            String security,
            boolean hidden
    ) {
        if (isBlank(ssid)) {
            return "";
        }

        String type = security == null ? "WPA" : security;

        if ("None".equalsIgnoreCase(type)) {
            type = "nopass";
        } else if ("WPA/WPA2".equalsIgnoreCase(type)) {
            type = "WPA";
        }

        StringBuilder result = new StringBuilder("WIFI:");

        result.append("T:")
                .append(escapeWifi(type))
                .append(";S:")
                .append(escapeWifi(ssid))
                .append(";");

        if (!"nopass".equalsIgnoreCase(type)) {
            result.append("P:")
                    .append(escapeWifi(password))
                    .append(";");
        }

        if (hidden) {
            result.append("H:true;");
        }

        return result.append(";").toString();
    }

    public static String buildContact(
            String firstName,
            String lastName,
            String phone,
            String email,
            String organization
    ) {
        if (isBlank(firstName)
                && isBlank(lastName)
                && isBlank(phone)
                && isBlank(email)
                && isBlank(organization)) {
            return "";
        }

        String fullName = join(firstName, lastName);

        StringBuilder result = new StringBuilder();
        result.append("BEGIN:VCARD\r\n");
        result.append("VERSION:3.0\r\n");

        if (!isBlank(fullName)) {
            result.append("N:")
                    .append(escapeVCard(lastName))
                    .append(";")
                    .append(escapeVCard(firstName))
                    .append(";;;\r\n");

            result.append("FN:")
                    .append(escapeVCard(fullName))
                    .append("\r\n");
        }

        if (!isBlank(organization)) {
            result.append("ORG:")
                    .append(escapeVCard(organization))
                    .append("\r\n");
        }

        if (!isBlank(phone)) {
            result.append("TEL:")
                    .append(escapeVCard(phone))
                    .append("\r\n");
        }

        if (!isBlank(email)) {
            result.append("EMAIL:")
                    .append(escapeVCard(email))
                    .append("\r\n");
        }

        result.append("END:VCARD");
        return result.toString();
    }

    public static String buildEmail(
            String address,
            String subject,
            String body
    ) {
        if (isBlank(address)) {
            return "";
        }

        Uri.Builder builder = Uri.parse("mailto:")
                .buildUpon()
                .appendEncodedPath(address.trim());

        boolean hasSubject = !isBlank(subject);
        boolean hasBody = !isBlank(body);

        if (hasSubject) {
            builder.appendQueryParameter("subject", subject);
        }

        if (hasBody) {
            builder.appendQueryParameter("body", body);
        }

        return builder.build().toString();
    }

    public static String buildSms(
            String phone,
            String message
    ) {
        if (isBlank(phone)) {
            return "";
        }

        Uri.Builder builder = Uri.parse("sms:")
                .buildUpon()
                .appendEncodedPath(phone.trim());

        if (!isBlank(message)) {
            builder.appendQueryParameter("body", message);
        }

        return builder.build().toString();
    }

    public static String buildEvent(
            String title,
            String location,
            String start,
            String end,
            String description
    ) {
        if (isBlank(title) || isBlank(start) || isBlank(end)) {
            return "";
        }

        try {
            LocalDateTime startDate = LocalDateTime.parse(
                    start.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            LocalDateTime endDate = LocalDateTime.parse(
                    end.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            if (!endDate.isAfter(startDate)) {
                return "";
            }

            StringBuilder result = new StringBuilder();

            result.append("BEGIN:VCALENDAR\r\n");
            result.append("VERSION:2.0\r\n");
            result.append("PRODID:-//BarLineX//QR Event//EN\r\n");
            result.append("BEGIN:VEVENT\r\n");

            result.append("SUMMARY:")
                    .append(escapeCalendar(title))
                    .append("\r\n");

            result.append("DTSTART:")
                    .append(startDate.format(EVENT_DATE_FORMAT))
                    .append("\r\n");

            result.append("DTEND:")
                    .append(endDate.format(EVENT_DATE_FORMAT))
                    .append("\r\n");

            if (!isBlank(location)) {
                result.append("LOCATION:")
                        .append(escapeCalendar(location))
                        .append("\r\n");
            }

            if (!isBlank(description)) {
                result.append("DESCRIPTION:")
                        .append(escapeCalendar(description))
                        .append("\r\n");
            }

            result.append("END:VEVENT\r\n");
            result.append("END:VCALENDAR");

            return result.toString();

        } catch (Exception exception) {
            return "";
        }
    }

    public static String buildLocation(
            String latitude,
            String longitude
    ) {
        if (isBlank(latitude) || isBlank(longitude)) {
            return "";
        }

        try {
            double lat = Double.parseDouble(latitude.trim());
            double lon = Double.parseDouble(longitude.trim());

            if (!Double.isFinite(lat)
                    || !Double.isFinite(lon)
                    || lat < -90 || lat > 90
                    || lon < -180 || lon > 180) {
                return "";
            }

            return "geo:" + lat + "," + lon;

        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private static String escapeWifi(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:")
                .replace("\"", "\\\"");
    }

    private static String escapeVCard(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace(";", "\\;")
                .replace(",", "\\,");
    }

    private static String escapeCalendar(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private static String join(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();

        if (left.isEmpty()) {
            return right;
        }

        if (right.isEmpty()) {
            return left;
        }

        return left + " " + right;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
