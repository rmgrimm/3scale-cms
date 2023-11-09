package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.*;
import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class CmsObjectPathKeyGenerator {

    public static final String LAYOUT_FILENAME_PREFIX = "l_";
    public static final String PARTIAL_FILENAME_PREFIX = "_";

    private static final String DEFAULT_HANDLER_SUFFIX = "";
    private static final String DEFAULT_BUILTIN_PAGE_SUFFIX = ".html";
    private static final String LIQUID_SUFFIX = ".liquid";

    private static final Map<String, String> CONTENT_TYPE_TO_FILE_EXT =
        Map.of(
            "text/css", ".css",
            "text/html", ".html",
            "text/javascript", ".js",
            "text/plain", ".txt"
        );
    private static final Map<Class<? extends CmsObject>, Function<? super CmsObject, String>> PATH_KEY_FUNCTIONS =
        Map.of(
            CmsSection.class, cmsSection -> generateKeyFromCms((CmsSection) cmsSection),
            CmsFile.class, cmsFile -> generateKeyFromCms((CmsFile) cmsFile),
            CmsBuiltinPage.class, cmsBuiltinPage -> generateKeyFromCms((CmsBuiltinPage) cmsBuiltinPage),
            CmsBuiltinPartial.class, cmsBuiltinPartial -> generateKeyFromCmsSystemName(((CmsBuiltinPartial) cmsBuiltinPartial).systemName(), PARTIAL_FILENAME_PREFIX),
            CmsLayout.class, cmsLayout -> generateKeyFromCmsSystemName(((CmsLayout) cmsLayout).systemName(), LAYOUT_FILENAME_PREFIX),
            CmsPage.class, cmsPage -> generateKeyFromCms((CmsPage) cmsPage),
            CmsPartial.class, cmsPartial -> generateKeyFromCmsSystemName(((CmsPartial) cmsPartial).systemName(), PARTIAL_FILENAME_PREFIX)
        );

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsSection cmsSection) {
        return StringUtils.appendIfMissing(cmsSection.path(), "/");
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsFile cmsFile) {
        return cmsFile.path();
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsBuiltinPage cmsBuiltinPage) {
        StringBuilder pathBuilder = new StringBuilder();

        String origPath = cmsBuiltinPage.path();
        if (StringUtils.isEmpty(origPath)) {
            origPath = cmsBuiltinPage.systemName();
            pathBuilder.append('/')
                .append(cmsBuiltinPage.systemName());
        }

        if (StringUtils.endsWith(origPath, "/")) {
            pathBuilder.append("index");
        }

        pathBuilder.append(DEFAULT_BUILTIN_PAGE_SUFFIX);

        if (StringUtils.isNotBlank(cmsBuiltinPage.handler())) {
            pathBuilder.append('.').append(cmsBuiltinPage.handler());
        } else {
            pathBuilder.append(DEFAULT_HANDLER_SUFFIX);
        }

        return pathBuilder
            .append(LIQUID_SUFFIX)
            .toString();
    }

    @Nonnull
    private static String generateKeyFromCmsSystemName(@Nullable String systemName,
                                                       @Nonnull String filenamePrefix) {
        StringBuilder pathBuilder = new StringBuilder();

        String[] pathSections = StringUtils.split(StringUtils.trimToEmpty(systemName), '/');

        if (pathSections.length < 1) {
            throw new IllegalStateException("Unknown how to handle object with unparseable system name: " + systemName);
        }

        for (int i = 0; i < pathSections.length - 1; i++) {
            pathBuilder.append('/')
                .append(pathSections[i]);
        }

        pathBuilder.append("/")
            .append(filenamePrefix)
            .append(pathSections[pathSections.length - 1])
            .append(".html")
            .append(DEFAULT_HANDLER_SUFFIX)
            .append(LIQUID_SUFFIX);

        return pathBuilder.toString();
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsPage cmsPage) {
        StringBuilder pathBuilder = new StringBuilder(cmsPage.path());

        if (StringUtils.endsWith(cmsPage.path(), "/")) {
            pathBuilder.append("index");
        }

        String fileExt = CONTENT_TYPE_TO_FILE_EXT.get(StringUtils.trimToEmpty(StringUtils.lowerCase(cmsPage.contentType())));
        if (fileExt != null) {
            if (!StringUtils.endsWith(cmsPage.path(), fileExt)) {
                pathBuilder.append(fileExt);
            }
        } else {
            Log.warn("Unknown file extension for content-type \"" + cmsPage.contentType() + "\"");
        }

        if (StringUtils.isNotBlank(cmsPage.handler())) {
            pathBuilder.append('.').append(cmsPage.handler());
        } else {
            pathBuilder.append(DEFAULT_HANDLER_SUFFIX);
        }

        if (Boolean.TRUE == cmsPage.liquidEnabled()) {
            pathBuilder.append(LIQUID_SUFFIX);
        }

        return pathBuilder.toString();
    }

    @Nonnull
    public String generatePathKeyForObject(@Nonnull CmsObject cmsObject) {
        Function<? super CmsObject, String> pathKeyFunc = PATH_KEY_FUNCTIONS.get(cmsObject.getClass());

        if (pathKeyFunc == null) {
            throw new IllegalStateException("Unable to map from type to path key: " + cmsObject.getClass());
        }

        String pathKey = pathKeyFunc.apply(cmsObject);

        if (StringUtils.isBlank(pathKey)) {
            throw new IllegalStateException("Path key generator failed for " + cmsObject);
        }

        return pathKey;
    }

}
