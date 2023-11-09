package com.fwmotion.threescale.cms.model;

import java.time.OffsetDateTime;

public record CmsBuiltinPartial(
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long id,
    String systemName,
    String contentType,
    String handler,
    Boolean liquidEnabled
) implements CmsBuiltinTemplate {
}
