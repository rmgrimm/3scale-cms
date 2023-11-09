package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public record CmsBuiltinPage(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    String systemName,
    String title,
    String path,
    String contentType,
    String layout,
    String handler,
    Boolean liquidEnabled,
    Boolean hidden
) implements CmsBuiltinTemplate {
}
