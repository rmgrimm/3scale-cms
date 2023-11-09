package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public record CmsPartial(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    String systemName,
    String contentType,
    String handler,
    Boolean liquidEnabled
) implements CmsTemplate {
}
