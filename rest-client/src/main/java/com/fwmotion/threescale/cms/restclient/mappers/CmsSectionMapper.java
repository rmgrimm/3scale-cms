package com.fwmotion.threescale.cms.restclient.mappers;

import com.fwmotion.threescale.cms.model.CmsSection;
import com.redhat.threescale.rest.cms.model.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CmsSectionMapper {

    @Mapping(target = "path", source = "partialPath")
    @Mapping(target = "_public", source = "public")
    CmsSection fromRest(Section section);

    @Mapping(target = "partialPath", source = "path")
    @Mapping(target = "_public", ignore = true)
    @Mapping(target = "public", source = "_public")
    Section toRest(CmsSection section);

}
