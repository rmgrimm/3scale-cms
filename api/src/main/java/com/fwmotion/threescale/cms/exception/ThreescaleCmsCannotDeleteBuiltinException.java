package com.fwmotion.threescale.cms.exception;

import com.fwmotion.threescale.cms.model.CmsError;
import jakarta.annotation.Nonnull;

public class ThreescaleCmsCannotDeleteBuiltinException extends ThreescaleCmsApiException {

    /**
     * The HTTP status code sent by 3scale when this error occurs
     */
    public static final int ERROR_HTTP_CODE = 422; // Unprocessable Entity

    /**
     * The error message sent by 3scale to indicate this type of error
     */
    public static final String ERROR_MESSAGE = "Built-in resources can't be deleted";

    public ThreescaleCmsCannotDeleteBuiltinException() {
        super(ERROR_HTTP_CODE,
            new CmsError(ERROR_HTTP_CODE, ERROR_MESSAGE));
    }
    public ThreescaleCmsCannotDeleteBuiltinException(@Nonnull CmsError apiError) {
        super(ERROR_HTTP_CODE,
            apiError);
    }
}
