package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public record CmsLayout(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    String systemName,
    String title,
    String contentType,
    String handler,
    Boolean liquidEnabled,
    String draftContent,
    String publishedContent
) implements CmsTemplate {
}
