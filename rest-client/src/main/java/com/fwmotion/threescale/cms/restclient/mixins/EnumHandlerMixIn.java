package com.fwmotion.threescale.cms.restclient.mixins;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.redhat.threescale.rest.cms.model.EnumHandler;

/**
 * @see EnumHandler
 */
@JsonSerialize(using = EnumHandlerSerializer.class)
public interface EnumHandlerMixIn {
}
