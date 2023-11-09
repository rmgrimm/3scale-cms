package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;

import java.time.OffsetDateTime;

public record CmsFile(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    Long sectionId,
    String path,
    Boolean downloadable,
    String contentType
) implements CmsObject {

    @Nonnull
    @Override
    public ThreescaleObjectType threescaleObjectType() {
        return ThreescaleObjectType.FILE;
    }

}
