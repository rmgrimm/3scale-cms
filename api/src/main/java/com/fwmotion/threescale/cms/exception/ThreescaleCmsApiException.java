package com.fwmotion.threescale.cms.exception;

import com.fwmotion.threescale.cms.model.CmsError;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * Common base exception type for all 3scale CMS-related API exceptions
 */
public class ThreescaleCmsApiException extends ThreescaleCmsException {

    private final int httpStatus;
    private final CmsError apiError;

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable CmsError apiError,
        @Nullable String message
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.apiError = apiError;
    }

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable String message
    ) {
        this(httpStatus, null, message);
    }

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable CmsError apiError
    ) {
        this(httpStatus,
            apiError,
            Optional.ofNullable(apiError)
                .map(CmsError::error)
                .orElse(null));
    }

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable CmsError apiError,
        @Nullable String message,
        @Nonnull Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.apiError = apiError;
    }

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable String message,
        @Nonnull Throwable cause
    ) {
        this(httpStatus, null, message, cause);
    }

    public ThreescaleCmsApiException(
        int httpStatus,
        @Nullable CmsError apiError,
        @Nonnull Throwable cause
    ) {
        this(httpStatus,
            apiError,
            Optional.ofNullable(apiError)
                .map(CmsError::error)
                .orElse(null),
            cause);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    @Nonnull
    public Optional<CmsError> getApiError() {
        return Optional.ofNullable(apiError);
    }
}
