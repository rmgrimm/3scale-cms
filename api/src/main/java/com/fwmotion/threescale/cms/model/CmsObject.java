package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.OffsetDateTime;

public interface CmsObject {

    default boolean builtin() {
        return false;
    }

    @Nonnull
    ThreescaleObjectType threescaleObjectType();

    @Nullable
    Long id();

    OffsetDateTime createdAt();

    OffsetDateTime updatedAt();

}
