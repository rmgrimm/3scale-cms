package com.fwmotion.threescale.cms.cli.mappers;

import com.fwmotion.threescale.cms.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsObjectWithChangedAttributesMapper {

    ///////////////
    // CmsFile
    ///////////////
    @Mapping(target = ".", source = "source")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sectionId", source = "sectionId")
    CmsFile withIdAndSectionId(CmsFile source, Long id, Long sectionId);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "sectionId", source = "sectionId")
    CmsFile withSectionId(CmsFile source, Long sectionId);

    ///////////////
    // CmsSection
    ///////////////
    @Mapping(target = ".", source = "source")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "parentId", source = "parentId")
    CmsSection withIdAndParentId(CmsSection source, Long id, Long parentId);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "parentId", source = "parentId")
    CmsSection withParentId(CmsSection source, Long parentId);

    ///////////////
    // CmsTemplate
    ///////////////
    @Mapping(target = ".", source = "source")
    @Mapping(target = "id", source = "id")
    CmsLayout withId(CmsLayout source, Long id);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sectionId", source = "sectionId")
    CmsPage withIdAndSectionId(CmsPage source, Long id, Long sectionId);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "sectionId", source = "sectionId")
    CmsPage withSectionId(CmsPage source, Long sectionId);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "layout", source = "layout")
    CmsPage withLayout(CmsPage source, String layout);

    @Mapping(target = ".", source = "source")
    @Mapping(target = "id", source = "id")
    CmsPartial withId(CmsPartial source, Long id);
}
