package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public record CmsPage(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    Long sectionId,
    String title,
    String path,
    String contentType,
    String layout,
    String handler,
    Boolean liquidEnabled,
    Boolean hidden
) implements CmsTemplate {
}
