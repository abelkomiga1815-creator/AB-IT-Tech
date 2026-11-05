package com.abittech.platform.document.dto;

import org.springframework.core.io.Resource;

/**
 * A file ready to be streamed to its owner.
 */
public record DocumentDownload(Resource resource, String fileName, String contentType, long sizeBytes) {
}
