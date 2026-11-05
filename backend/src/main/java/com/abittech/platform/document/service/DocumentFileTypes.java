package com.abittech.platform.document.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.StringUtils;

/**
 * Allowed upload types. The content type served on download comes from this table (by extension),
 * never from what the client claimed, and executables/scripts/archives are not accepted.
 */
public final class DocumentFileTypes {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("csv", "text/csv"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/plain"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"));

    private static final int MAX_NAME_LENGTH = 200;

    private DocumentFileTypes() {
    }

    /** @return the lower-case extension of an allowed type, or empty when the file type is not allowed */
    public static Optional<String> allowedExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            return Optional.empty();
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.containsKey(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    public static Optional<String> contentTypeFor(String extension) {
        return Optional.ofNullable(CONTENT_TYPES.get(extension));
    }

    /**
     * Extracts the lower-case extension from a filename if it is an allowed type.
     * @return the allowed extension in lower case, or empty if not allowed or no extension
     */
    public static Optional<String> extensionOf(String filename) {
        return allowedExtension(filename);
    }

    /**
     * Sanitizes a filename for safe storage: strips directory components, control characters,
     * limits length, and preserves the extension. Falls back to {@code document.<ext>} or
     * {@code document} when nothing usable remains.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String extension = StringUtils.getFilenameExtension(filename);
        String normalizedExt = (extension != null) ? extension.toLowerCase(Locale.ROOT) : "";
        boolean allowed = CONTENT_TYPES.containsKey(normalizedExt);
        String extForFallback = allowed ? normalizedExt : "";

        String name = StringUtils.getFilename(filename.replace('\\', '/'));
        name = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", "").trim();

        if (name.isEmpty() || name.equals(".") || name.equals("..") || (allowed && name.equals("." + normalizedExt))) {
            return "document" + (extForFallback.isEmpty() ? "" : "." + extForFallback);
        }

        if (name.length() > MAX_NAME_LENGTH) {
            if (allowed) {
                String base = name.substring(0, name.length() - normalizedExt.length() - 1);
                name = base.substring(0, MAX_NAME_LENGTH - normalizedExt.length() - 1) + "." + normalizedExt;
            } else {
                name = name.substring(0, MAX_NAME_LENGTH);
            }
        }
        return name;
    }

    /**
     * Display name safe to store and return: no directory parts, no control characters, bounded length.
     * Falls back to {@code document.<ext>} when nothing usable is left.
     */
    public static String sanitizeDisplayName(String original, String extension) {
        String name = StringUtils.getFilename(original == null ? "" : original.replace('\\', '/'));
        name = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.equals("." + extension)) {
            return "document." + extension;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            String base = name.substring(0, name.length() - extension.length() - 1);
            name = base.substring(0, MAX_NAME_LENGTH - extension.length() - 1) + "." + extension;
        }
        return name;
    }
}
