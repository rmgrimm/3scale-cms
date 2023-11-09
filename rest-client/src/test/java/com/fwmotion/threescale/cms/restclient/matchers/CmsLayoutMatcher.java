package com.fwmotion.threescale.cms.restclient.matchers;

import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.model.Layout;
import jakarta.annotation.Nonnull;
import org.hamcrest.Description;

public class CmsLayoutMatcher extends CmsObjectMatcher {

    private final Layout expected;

    public CmsLayoutMatcher(Layout expected) {
        super(
            expected.getId(),
            expected.getCreatedAt(),
            expected.getUpdatedAt()
        );
        this.expected = expected;
    }

    @Override
    public boolean matchesSafely(@Nonnull CmsObject actual) {
        if (!(actual instanceof CmsLayout actualLayout)) {
            return false;
        }

        return super.matchesSafely(actual)
            && actualMatchesExpected(expected.getSystemName(), actualLayout.systemName())
            && actualMatchesExpected(expected.getContentType(), actualLayout.contentType())
            && actualMatchesExpected(expected.getHandler(), actualLayout.handler())
            && actualMatchesExpected(expected.getLiquidEnabled(), actualLayout.liquidEnabled())
            && actualMatchesExpected(expected.getTitle(), actualLayout.title())
            && actualMatchesExpected(expected.getDraft(), actualLayout.draftContent())
            && actualMatchesExpected(expected.getPublished(), actualLayout.publishedContent());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("CmsLayout from " + expected);
    }
}
