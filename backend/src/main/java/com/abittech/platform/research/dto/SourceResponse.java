package com.abittech.platform.research.dto;

import java.time.Instant;
import java.util.UUID;

import com.abittech.platform.research.entity.ResearchSource;

public record SourceResponse(
        UUID id, String title, String authors, Integer year, String venue, String doi, String url,
        String provider, String note, Instant createdAt) {

    public static SourceResponse from(ResearchSource source) {
        return new SourceResponse(source.getId(), source.getTitle(), source.getAuthors(), source.getPublicationYear(),
                source.getVenue(), source.getDoi(), source.getUrl(), source.getProvider(), source.getNote(),
                source.getCreatedAt());
    }
}
