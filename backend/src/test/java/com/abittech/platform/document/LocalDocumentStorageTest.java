package com.abittech.platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.abittech.platform.config.StorageProperties;
import com.abittech.platform.document.service.LocalDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDocumentStorageTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalDocumentStorage(new StorageProperties(tempDir.resolve("docs").toString(), 1000));
    }

    @Test
    void createsTheDirectoryAndStoresLoadsAndDeletesFiles() throws Exception {
        storage.store("abc.pdf", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.load("abc.pdf")).isNotNull();
        assertThat(storage.load("abc.pdf").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("hello");

        storage.delete("abc.pdf");
        assertThat(storage.load("abc.pdf")).isNull();
    }

    @Test
    void missingFilesLoadAsNullAndDeletingThemIsHarmless() {
        assertThat(storage.load("missing.pdf")).isNull();
        storage.delete("missing.pdf");
    }

    @Test
    void keysCannotEscapeTheStorageDirectory() {
        assertThatThrownBy(() -> storage.store("../escape.pdf", new ByteArrayInputStream(new byte[] {1})))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("..")).isInstanceOf(IllegalArgumentException.class);
    }
}
