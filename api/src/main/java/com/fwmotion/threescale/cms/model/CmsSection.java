package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;

import java.time.OffsetDateTime;

public record CmsSection(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    Long parentId,
    String systemName,
    String title,
    String path,
    Boolean _public
) implements CmsObject {

    @Nonnull
    @Override
    public ThreescaleObjectType threescaleObjectType() {
        return ThreescaleObjectType.SECTION;
    }

}
