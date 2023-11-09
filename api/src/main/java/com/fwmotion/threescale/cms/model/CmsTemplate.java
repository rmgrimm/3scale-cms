package com.fwmotion.threescale.cms.model;

import jakarta.annotation.Nonnull;

public interface CmsTemplate extends CmsObject {

    @Nonnull
    @Override
    default ThreescaleObjectType threescaleObjectType() {
        return ThreescaleObjectType.TEMPLATE;
    }

}
