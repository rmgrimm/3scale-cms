package com.fwmotion.threescale.cms.exception;

import jakarta.annotation.Nonnull;

public class ThreescaleCmsCannotCreateBuiltinException extends ThreescaleCmsApiException {

    public static final int ERROR_HTTP_STATUS = 400; // Bad Request

    public ThreescaleCmsCannotCreateBuiltinException(@Nonnull String message) {
        super(ERROR_HTTP_STATUS, message);
    }

}
