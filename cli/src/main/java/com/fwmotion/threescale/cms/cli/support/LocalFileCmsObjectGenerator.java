package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.*;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocalFileCmsObjectGenerator {

    private static final Map<String, String> FILE_EXT_TO_CONTENT_TYPE =
        Map.of(
            ".css", "text/css",
            ".gif", "image/gif",
            ".htm", "text/html",
            ".html", "text/html",
            ".ico", "image/x-icon",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".js", "text/javascript",
            ".png", "image/png",
            ".txt", "text/plain"
        );

    private static final Set<String> TEMPLATE_CONTENT_TYPES = Set.of(
        "text/css",
        "text/html",
        "text/javascript",
        "text/plain"
    );

    private static final Set<String> REMOVE_FILEEXT_CONTENT_TYPES = Collections.singleton(
        "text/html"
    );

    private static final Pattern LAYOUT_PREFIX_PATTERN = Pattern.compile("(?<path>.*/)" + CmsObjectPathKeyGenerator.LAYOUT_FILENAME_PREFIX + "(?<layoutname>[^/]+)");
    private static final Predicate<String> LAYOUT_PREFIX_PREDICATE = LAYOUT_PREFIX_PATTERN.asMatchPredicate();

    private static final Pattern PARTIAL_PREFIX_PATTERN = Pattern.compile("(?<path>.*/)" + CmsObjectPathKeyGenerator.PARTIAL_FILENAME_PREFIX + "(?<partialname>[^/]+)");
    private static final Predicate<String> PARTIAL_PREFIX_PREDICATE = PARTIAL_PREFIX_PATTERN.asMatchPredicate();

    private static final Pattern FILE_SUFFIX_PATTERN = Pattern.compile(
        "(?<filename>.*?)" +
            "(?<fileext>" +
            FILE_EXT_TO_CONTENT_TYPE.keySet().stream().map(ext -> "\\" + ext).collect(Collectors.joining("|"))
            + ")?" +
            "(?:\\.(?<handler>markdown|textile))?" +
            "(?<liquid>\\.liquid)?");

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Nonnull
    private static OffsetDateTime calculateUpdatedAt(@Nonnull File file) {
        return Instant.ofEpochMilli(file.lastModified()).atOffset(ZoneOffset.UTC);
    }

    @Nonnull
    public CmsObject generateObjectFromFile(@Nonnull String relativePath,
                                            @Nonnull File file) {
        if (file.isDirectory()) {
            return generateSectionFromFile(relativePath, file);
        }

        if (LAYOUT_PREFIX_PREDICATE.test(relativePath)
            || StringUtils.startsWith(relativePath, "/layouts/")) {
            return generateLayoutFromFile(relativePath, file);
        }

        if (PARTIAL_PREFIX_PREDICATE.test(relativePath)) {
            return generatePartialFromFile(relativePath, file);
        }

        Matcher matcher = FILE_SUFFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            String fileExt = StringUtils.trimToEmpty(matcher.group("fileext"));
            String contentType = FILE_EXT_TO_CONTENT_TYPE.getOrDefault(fileExt, DEFAULT_CONTENT_TYPE);

            if (TEMPLATE_CONTENT_TYPES.contains(contentType)) {
                return generatePageFromFile(relativePath, file);
            }
        }

        return generateCmsFileFromFile(relativePath, file);
    }

    private FileSuffixInfo getFileSuffixInfo(@Nonnull String path) {
        Matcher matcher = FILE_SUFFIX_PATTERN.matcher(path);
        if (matcher.matches()) {
            String fileExt = StringUtils.trimToEmpty(matcher.group("fileext"));
            String contentType = FILE_EXT_TO_CONTENT_TYPE.getOrDefault(fileExt, DEFAULT_CONTENT_TYPE);

            String handler = StringUtils.trimToEmpty(matcher.group("handler"));

            Boolean liquidEnabled = null;
            if (StringUtils.isNotBlank(matcher.group("liquid"))) {
                liquidEnabled = true;
            }

            String filename = matcher.group("filename");
            if (StringUtils.isNotBlank(fileExt)
                && !REMOVE_FILEEXT_CONTENT_TYPES.contains(contentType)) {
                filename += fileExt;
            }

            return new FileSuffixInfo(
                filename,
                contentType,
                handler,
                liquidEnabled
            );
        }

        return new FileSuffixInfo(
            path,
            null,
            null,
            null
        );
    }

    @Nonnull
    private CmsSection generateSectionFromFile(@Nonnull String relativePath,
                                               @Nonnull File file) {
        String basename = file.toPath().getFileName().toString();
        if ("/".equals(relativePath)) {
            basename = "root";
        }

        return new CmsSection(
            null,
            calculateUpdatedAt(file),
            null,
            null,
            basename,
            basename,
            relativePath.replaceAll("/+$", ""),
            true
        );
    }

    @Nonnull
    private CmsLayout generateLayoutFromFile(@Nonnull String relativePath,
                                             @Nonnull File file) {
        String transformedPath;

        Matcher matcher = LAYOUT_PREFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            transformedPath = matcher.group("path") + matcher.group("layoutname");
        } else {
            transformedPath = relativePath;
        }

        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(transformedPath);

        String systemName = fileSuffixInfo.path()
            .replaceFirst("^/+", "")
            .replaceFirst("^/layouts/", "/");

        String layoutTitle = systemName.replaceAll("_", " ");
        if (!StringUtils.endsWithIgnoreCase(layoutTitle, " layout")) {
            layoutTitle += " layout";
        }

        return new CmsLayout(
            null,
            calculateUpdatedAt(file),
            null,
            systemName,
            layoutTitle,
            fileSuffixInfo.contentType(),
            fileSuffixInfo.handler(),
            fileSuffixInfo.liquidEnabled(),
            null,
            null
        );
    }

    @Nonnull
    private CmsPartial generatePartialFromFile(@Nonnull String relativePath,
                                               @Nonnull File file) {
        String transformedPath;

        Matcher matcher = PARTIAL_PREFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            transformedPath = matcher.group("path") + matcher.group("partialname");
        } else {
            transformedPath = relativePath;
        }

        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(transformedPath);

        return new CmsPartial(
            null,
            calculateUpdatedAt(file),
            null,
            fileSuffixInfo.path().replaceFirst("^/+", ""),
            fileSuffixInfo.contentType(),
            fileSuffixInfo.handler(),
            fileSuffixInfo.liquidEnabled()
        );
    }

    @Nonnull
    private CmsPage generatePageFromFile(@Nonnull String relativePath,
                                         @Nonnull File file) {
        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(relativePath);

        String path = fileSuffixInfo.path()
            .replaceFirst("/index$", "/");

        String title;
        if ("/".equals(path)) {
            title = "Home";
        } else {
            title = Path.of(path).getFileName().toString();
        }

        return new CmsPage(
            null,
            calculateUpdatedAt(file),
            null,
            null,
            title,
            path,
            fileSuffixInfo.contentType(),
            null,
            fileSuffixInfo.handler(),
            fileSuffixInfo.liquidEnabled(),
            null
        );
    }

    @Nonnull
    private CmsFile generateCmsFileFromFile(@Nonnull String relativePath,
                                            @Nonnull File file) {
        return new CmsFile(
            null,
            calculateUpdatedAt(file),
            null,
            null,
            relativePath,
            null,
            null
        );
    }

    private record FileSuffixInfo(
        String path,
        String contentType,
        String handler,
        Boolean liquidEnabled
    ) {
    }

}
