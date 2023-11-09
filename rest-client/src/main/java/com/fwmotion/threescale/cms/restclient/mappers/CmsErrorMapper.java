package com.fwmotion.threescale.cms.restclient.mappers;

import com.fwmotion.threescale.cms.model.CmsError;
import com.redhat.threescale.rest.cms.model.Error;
import org.mapstruct.Mapper;

@Mapper
public interface CmsErrorMapper {

    CmsError fromRest(Error apiError);

}
