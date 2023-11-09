package com.fwmotion.threescale.cms.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.model.*;
import com.fwmotion.threescale.cms.restclient.mixins.EnumHandlerMixIn;
import com.fwmotion.threescale.cms.restclient.testsupport.FilesApiTestSupport;
import com.fwmotion.threescale.cms.restclient.testsupport.SectionsApiTestSupport;
import com.fwmotion.threescale.cms.restclient.testsupport.TemplatesApiTestSupport;
import com.redhat.threescale.rest.cms.ApiClient;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.*;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.fwmotion.threescale.cms.restclient.matchers.HeaderMatcher.header;
import static com.fwmotion.threescale.cms.restclient.matchers.InputStreamContentsMatcher.inputStreamContents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.only;

@ExtendWith(MockitoExtension.class)
class ThreescaleCmsClientRestImplUnitTest {

    @InjectMocks
    ThreescaleCmsClientRestImpl threescaleCmsClient;
    @Mock
    FilesApi filesApi;
    @Mock
    SectionsApi sectionsApi;
    @Mock
    TemplatesApi templatesApi;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    SectionsApiTestSupport sectionsApiTestSupport;
    FilesApiTestSupport filesApiTestSupport;
    TemplatesApiTestSupport templatesApiTestSupport;

    ApiClient apiClient;

    @Mock
    CloseableHttpClient httpClientMock;

    @Mock
    CloseableHttpResponse httpResponseMock;

    @Captor
    ArgumentCaptor<HttpClientResponseHandler<?>> responseHandlerMatcher;

    @BeforeEach
    void setUp() {
        objectMapper.addMixIn(EnumHandler.class, EnumHandlerMixIn.class);

        sectionsApiTestSupport = new SectionsApiTestSupport(sectionsApi);
        filesApiTestSupport = new FilesApiTestSupport(filesApi);
        templatesApiTestSupport = new TemplatesApiTestSupport(templatesApi);

        apiClient = new ApiClient(httpClientMock);
    }

    @Test
    void testStreamAllCmsObjects() throws Exception {
        sectionsApiTestSupport.givenListSectionRootAndCss();
        filesApiTestSupport.givenListFilesOnlyFavicon();
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsObject> result = threescaleCmsClient.streamAllCmsObjects()
            .collect(Collectors.toList());

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        filesApiTestSupport.thenOnlyListFilesCalled();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(
            SectionsApiTestSupport.ROOT_BUILTIN_SECTION_MATCHER,
            SectionsApiTestSupport.CSS_SECTION_MATCHER,
            FilesApiTestSupport.FAVICON_FILE_MATCHER,
            TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void listAllCmsObjects() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();
        filesApiTestSupport.givenListFilesOnlyFavicon();
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsObject> result = threescaleCmsClient.listAllCmsObjects();

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        filesApiTestSupport.thenOnlyListFilesCalled();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(
            SectionsApiTestSupport.ROOT_BUILTIN_SECTION_MATCHER,
            FilesApiTestSupport.FAVICON_FILE_MATCHER,
            TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void streamSections() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();

        List<CmsSection> result = threescaleCmsClient.streamSections()
            .collect(Collectors.toList());

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(SectionsApiTestSupport.ROOT_BUILTIN_SECTION_MATCHER));
    }

    @Test
    void listSections() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();

        List<CmsSection> result = threescaleCmsClient.listSections();

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(SectionsApiTestSupport.ROOT_BUILTIN_SECTION_MATCHER));
    }

    @Test
    void streamFiles() throws Exception {
        filesApiTestSupport.givenListFilesOnlyFavicon();

        List<CmsFile> result = threescaleCmsClient.streamFiles()
            .collect(Collectors.toList());

        filesApiTestSupport.thenOnlyListFilesCalled();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(FilesApiTestSupport.FAVICON_FILE_MATCHER));
    }

    @Test
    void listFiles() throws Exception {
        filesApiTestSupport.givenListFilesOnlyFavicon();

        List<CmsFile> result = threescaleCmsClient.listFiles();

        filesApiTestSupport.thenOnlyListFilesCalled();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(FilesApiTestSupport.FAVICON_FILE_MATCHER));
    }

    @Test
    void getFileContent_ByIdNoAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new WrappedProviderAccount()
                .account(new ProviderAccount()
                    .baseUrl("https://3scale.example.com")
                    .siteAccessCode("")));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>) invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(16);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And no access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, is("*/*")))
            ).and(
                not(hasItemInArray(header("Cookie", Matchers.startsWith("access_code="))))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByIdWithAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including an access code)
        given(filesApi.readProviderSettings())
            .willReturn(new WrappedProviderAccount()
                .account(new ProviderAccount()
                    .baseUrl("https://3scale.example.com")
                    .siteAccessCode("this is my access code")));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>) invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(16);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And the correct access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, equalTo("*/*")))
            ).and(
                hasItemInArray(header("Cookie", equalTo("access_code=this is my access code")))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByCmsFileNoAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new WrappedProviderAccount()
                .account(new ProviderAccount()
                    .baseUrl("https://3scale.example.com")
                    .siteAccessCode("")));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>) invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        CmsFile cmsFile = new CmsFile(
            null,
            null,
            16L,
            null,
            null,
            null,
            null
        );

        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(cmsFile);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And no access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, is("*/*")))
            ).and(
                not(hasItemInArray(header("Cookie", Matchers.startsWith("access_code="))))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByCmsFileWithAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including an access code)
        given(filesApi.readProviderSettings())
            .willReturn(new WrappedProviderAccount()
                .account(new ProviderAccount()
                    .baseUrl("https://3scale.example.com")
                    .siteAccessCode("this is my access code")));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>) invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        CmsFile cmsFile = new CmsFile(
            null,
            null,
            16L,
            null,
            null,
            null,
            null
        );

        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(cmsFile);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And the correct access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, equalTo("*/*")))
            ).and(
                hasItemInArray(header("Cookie", equalTo("access_code=this is my access code")))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void streamTemplates_WithNoContent() throws Exception {
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsTemplate> result = threescaleCmsClient.streamTemplates(false)
            .collect(Collectors.toList());

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void listTemplates_WithNoContent() throws Exception {
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsTemplate> result = threescaleCmsClient.listTemplates(false);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void getTemplateDraft_ByIdNoDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithoutDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByIdWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplateDraft_ByObjectNoDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithoutDraft(119);

        CmsLayout layout = new CmsLayout(
            null,
            null,
            119L,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByObjectWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout(
            null,
            null,
            119L,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplatePublished_ById() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplatePublished_ByObject() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout(
            null,
            null,
            119L,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void save_NewSectionNoTitle() throws Exception {
        // Given a new CmsSection object to create with no title set
        CmsSection newSection = new CmsSection(
            null,
            null,
            null, 30L,
            "new",
            null, "/new",
            null
        );

        // And the generated API will respond with an object with an ID
        given(sectionsApi.createSection(
            eq(newSection._public()),
            eq(newSection.systemName()),
            eq(newSection.parentId()),
            eq(newSection.path()),
            eq(newSection.systemName())))
            .willReturn(new Section()
                .parentId(newSection.parentId())
                .id(31L)
                .systemName(newSection.systemName())
                .partialPath(newSection.path())
                .title(newSection.systemName())
                ._public(newSection._public())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        CmsSection savedSection = threescaleCmsClient.save(newSection);

        // Then only the Sections API should have been called to create a section
        then(sectionsApi).should(only()).createSection(
            eq(newSection._public()),
            eq(newSection.systemName()),
            eq(newSection.parentId()),
            eq(newSection.path()),
            eq(newSection.systemName()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the new section object should have an ID
        assertThat(savedSection.id(), is(31L));
    }

    @Test
    void save_NewSectionWithTitle() throws Exception {
        // Given a new CmsSection object to create with a title
        CmsSection newSection = new CmsSection(
            null,
            null,
            null, 30L,
            "new",
            "new_section", "/new",
            true
        );

        // And the generated API will respond with an object with an ID
        given(sectionsApi.createSection(
            eq(newSection._public()),
            eq(newSection.title()),
            eq(newSection.parentId()),
            eq(newSection.path()),
            eq(newSection.systemName())))
            .willReturn(new Section()
                .parentId(newSection.parentId())
                .id(32L)
                .systemName(newSection.systemName())
                .partialPath(newSection.path())
                .title(newSection.title())
                ._public(newSection._public())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        CmsSection savedSection = threescaleCmsClient.save(newSection);

        // Then only the sections API should have been called to create a section
        then(sectionsApi).should(only()).createSection(
            eq(newSection._public()),
            eq(newSection.title()),
            eq(newSection.parentId()),
            eq(newSection.path()),
            eq(newSection.systemName()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the new section should have an ID
        assertThat(savedSection.id(), is(32L));
    }

    @Test
    void save_UpdatedSection() throws Exception {
        // Given a CmsSection object with an ID already
        CmsSection updatedSection = new CmsSection(
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            31L, 30L,
            "new",
            "new_section", "/new",
            true
        );

        // And the generated API will respond with an object with an ID
        given(sectionsApi.updateSection(
            eq(updatedSection.id()),
            eq(updatedSection._public()),
            eq(updatedSection.title()),
            eq(updatedSection.parentId())))
            .willReturn(new Section()
                .parentId(updatedSection.parentId())
                .id(updatedSection.id())
                .systemName(updatedSection.systemName())
                .partialPath(updatedSection.path())
                .title(updatedSection.title())
                ._public(updatedSection._public())
                .createdAt(updatedSection.createdAt())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        threescaleCmsClient.save(updatedSection);

        // Then only the sections API should have been called to create a section
        then(sectionsApi).should(only()).updateSection(
            eq(updatedSection.id()),
            eq(updatedSection._public()),
            eq(updatedSection.title()),
            eq(updatedSection.parentId()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void save_NewFile() throws Exception {
        // Given a CmsFile object with no ID yet
        CmsFile newFile = new CmsFile(
            null,
            null,
            null,
            30L,
            "/file.jpg",
            true,
            null
        );

        // And a File
        File newFileContent = new File("/tmp/file.jpg");

        // And the generated API will respond with an object with an ID
        given(filesApi.createFile(
            eq(newFile.sectionId()),
            eq(newFile.path()),
            same(newFileContent),
            eq(newFile.downloadable()),
            nullable(String.class)))
            .willReturn(new ModelFile()
                // TODO
                .id(17L));

        // When the interface code is called
        CmsFile savedFile = threescaleCmsClient.save(newFile, newFileContent);

        // Then only the file content should have been saved
        then(filesApi).should(only()).createFile(
            eq(newFile.sectionId()),
            eq(newFile.path()),
            same(newFileContent),
            eq(newFile.downloadable()),
            nullable(String.class));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the file should have had its ID updated
        assertThat(savedFile.id(), is(17L));
    }

    @Test
    void save_UpdatedFile() throws Exception {
        // Given a CmsFile object with an ID already
        CmsFile updateFile = new CmsFile(
            null,
            null,
            16L,
            30L,
            "/file.jpg",
            true,
            null
        );

        // And a File
        File newFileContent = new File("/tmp/file.jpg");

        // And the generated API will respond with an object with an ID
        given(filesApi.updateFile(
            eq(updateFile.id()),
            eq(updateFile.sectionId()),
            eq(updateFile.path()),
            eq(updateFile.downloadable()),
            same(newFileContent),
            nullable(String.class)))
            .willReturn(new ModelFile()
                // TODO
                .id(17L));

        // When the interface code is called
        threescaleCmsClient.save(updateFile, newFileContent);

        // Then only the file content should have been saved
        then(filesApi).should(only()).updateFile(
            eq(updateFile.id()),
            eq(updateFile.sectionId()),
            eq(updateFile.path()),
            eq(updateFile.downloadable()),
            same(newFileContent),
            nullable(String.class));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void testGetFileContent() {
    }

    @Test
    void testSave() {
    }

    @Test
    void testSave1() {
    }

    @Test
    void publish() {
    }

    @Test
    void delete_File() throws Exception {
        // Given a CmsFile object with an ID already
        CmsFile newFile = new CmsFile(
            null,
            null,
            16L,
            30L,
            "/file.jpg",
            true,
            null
        );

        // When the interface code is called
        threescaleCmsClient.delete(newFile);

        // Then only the file should have been deleted
        then(filesApi).should(only()).deleteFile(
            eq(newFile.id()));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_FileById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.FILE, 16);

        // Then only the file should have been deleted
        then(filesApi).should(only()).deleteFile(
            eq(16L));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_Section() throws Exception {
        // Given a CmsSection object with an ID already
        CmsSection cmsSection = new CmsSection(
            null,
            null,
            31L, 30L,
            "new",
            "new_section", "/new",
            true
        );

        // When the interface code is called
        threescaleCmsClient.delete(cmsSection);

        // Then only the section should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).should(only()).deleteSection(
            eq(cmsSection.id()));
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_SectionById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.SECTION, 31);

        // Then only the section should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).should(only()).deleteSection(
            eq(31L));
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_Template() throws Exception {
        // Given a CmsLayout (Template) object with an ID already
        CmsLayout layout = new CmsLayout(
            null,
            null,
            119L,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // When the interface code is called
        threescaleCmsClient.delete(layout);

        // Then only the template should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).deleteTemplate(
            eq(layout.id()));
    }

    @Test
    void delete_TemplateById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.TEMPLATE, 119);

        // Then only the template should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).deleteTemplate(
            eq(119L));
    }
}
