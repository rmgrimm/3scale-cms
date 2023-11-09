package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.fwmotion.threescale.cms.model.ThreescaleObjectType;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;

@ApplicationScoped
public class CmsSectionToTopComparator implements Comparator<CmsObject> {

    @Override
    public int compare(CmsObject left, CmsObject right) {
        boolean leftIsSection = left.threescaleObjectType() == ThreescaleObjectType.SECTION;
        boolean rightIsSection = right.threescaleObjectType() == ThreescaleObjectType.SECTION;
        if (leftIsSection) {
            if (rightIsSection) {
                return compareSectionIds((CmsSection) left, (CmsSection) right);
            }
            return -1;
        } else if (rightIsSection) {
            return 1;
        }

        return 0;
    }

    private int compareSectionIds(CmsSection left, CmsSection right) {
        if (left.id() == null) {
            return 1;
        } else if (right.id() == null) {
            return -1;
        }
        return left.id().compareTo(right.id());
    }

}
