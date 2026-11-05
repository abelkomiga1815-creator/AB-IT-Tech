package com.abittech.platform.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.abittech.platform.document.service.DocumentFileTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentFileTypesTest {

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "docx", "pptx", "xlsx", "csv", "txt", "md", "png", "jpg", "jpeg"})
    void commonStudyFileTypesAreAccepted(String extension) {
        assertThat(DocumentFileTypes.contentTypeFor(extension)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"exe", "bat", "sh", "js", "html", "htm", "svg", "php", "jar", "zip", "dll"})
    void executableAndActiveContentTypesAreRejected(String extension) {
        assertThat(DocumentFileTypes.contentTypeFor(extension)).isEmpty();
    }

    @Test
    void extensionIsLowerCasedAndOptional() {
        assertThat(DocumentFileTypes.extensionOf("Report.PDF")).contains("pdf");
        assertThat(DocumentFileTypes.extensionOf("README")).isEmpty();
        assertThat(DocumentFileTypes.extensionOf(null)).isEmpty();
    }

    @Test
    void sanitizeRemovesDirectoriesOfBothStyles() {
        assertThat(DocumentFileTypes.sanitizeFilename("../../etc/passwd.txt")).isEqualTo("passwd.txt");
        assertThat(DocumentFileTypes.sanitizeFilename("C:\\Users\\me\\thesis.docx")).isEqualTo("thesis.docx");
    }

    @Test
    void sanitizeRemovesControlCharactersAndFallsBackForEmptyNames() {
        assertThat(DocumentFileTypes.sanitizeFilename("na\nme\u0000.pdf")).isEqualTo("name.pdf");
        assertThat(DocumentFileTypes.sanitizeFilename("")).isEqualTo("document");
        assertThat(DocumentFileTypes.sanitizeFilename("..")).isEqualTo("document");
        assertThat(DocumentFileTypes.sanitizeFilename(null)).isEqualTo("document");
    }

    @Test
    void sanitizeLimitsLengthButKeepsTheExtension() {
        String sanitized = DocumentFileTypes.sanitizeFilename("a".repeat(300) + ".pdf");

        assertThat(sanitized).hasSizeLessThanOrEqualTo(200).endsWith(".pdf");
    }
}
