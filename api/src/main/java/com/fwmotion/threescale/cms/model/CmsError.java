package com.fwmotion.threescale.cms.model;

public record CmsError(
    int status,
    String error
) {
}
