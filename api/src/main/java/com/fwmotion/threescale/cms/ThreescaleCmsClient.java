package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.exception.ThreescaleCmsCannotDeleteBuiltinException;
import com.fwmotion.threescale.cms.exception.ThreescaleCmsException;
import com.fwmotion.threescale.cms.model.*;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public interface ThreescaleCmsClient {

    @Nonnull
    default Stream<CmsObject> streamAllCmsObjects() throws ThreescaleCmsException {
        return Stream.of(
            streamSections(),
            streamFiles(),
            streamTemplates(false)
        ).flatMap(s -> s);
    }

    @Nonnull
    default List<CmsObject> listAllCmsObjects() throws ThreescaleCmsException {
        return streamAllCmsObjects().toList();
    }

    @Nonnull
    Stream<CmsSection> streamSections();

    @Nonnull
    default List<CmsSection> listSections() throws ThreescaleCmsException {
        return streamSections().toList();
    }

    @Nonnull
    Stream<CmsFile> streamFiles();

    @Nonnull
    default List<CmsFile> listFiles() throws ThreescaleCmsException {
        return streamFiles().toList();
    }

    @Nonnull
    Optional<InputStream> getFileContent(long fileId) throws ThreescaleCmsException;

    @Nonnull
    default Optional<InputStream> getFileContent(@Nonnull CmsFile file) throws ThreescaleCmsException {
        return Optional.of(file)
            .map(CmsFile::id)
            .flatMap(this::getFileContent);
    }

    @Nonnull
    Stream<CmsTemplate> streamTemplates(boolean includeContent);

    @Nonnull
    default List<CmsTemplate> listTemplates(boolean includeContent) throws ThreescaleCmsException {
        return streamTemplates(includeContent).toList();
    }

    @Nonnull
    Optional<InputStream> getTemplateDraft(long templateId) throws ThreescaleCmsException;

    @Nonnull
    default Optional<InputStream> getTemplateDraft(@Nonnull CmsTemplate template) throws ThreescaleCmsException {
        return Optional.of(template)
            .map(CmsTemplate::id)
            .flatMap(this::getTemplateDraft);
    }

    @Nonnull
    Optional<InputStream> getTemplatePublished(long templateId) throws ThreescaleCmsException;

    @Nonnull
    default Optional<InputStream> getTemplatePublished(@Nonnull CmsTemplate template) throws ThreescaleCmsException {
        return Optional.of(template)
            .map(CmsTemplate::id)
            .flatMap(this::getTemplatePublished);
    }

    CmsSection save(@Nonnull CmsSection section) throws ThreescaleCmsException;

    CmsFile save(@Nonnull CmsFile file, @Nonnull File fileContent) throws ThreescaleCmsException;

    CmsTemplate save(@Nonnull CmsTemplate template, @Nonnull File draft) throws ThreescaleCmsException;

    void publish(long templateId) throws ThreescaleCmsException;

    default void publish(@Nonnull CmsTemplate template) throws ThreescaleCmsException {
        publish(Objects.requireNonNull(template.id()));
    }

    void delete(@Nonnull ThreescaleObjectType type, long id) throws ThreescaleCmsException;

    default void delete(@Nonnull CmsObject object) throws ThreescaleCmsException {
        if (object.builtin()) {
            throw new ThreescaleCmsCannotDeleteBuiltinException();
        }

        Long id = object.id();
        if (id != null) {
            delete(object.threescaleObjectType(), id);
        }
    }
}
