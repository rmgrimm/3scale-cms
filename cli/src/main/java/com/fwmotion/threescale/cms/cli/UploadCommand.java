package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.cli.mappers.CmsObjectWithChangedAttributesMapper;
import com.fwmotion.threescale.cms.cli.support.*;
import com.fwmotion.threescale.cms.model.*;
import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.factory.Mappers;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
    header = "Upload 3scale CMS Content",
    name = "upload",
    description = "Upload all content, specified file, or all specified " +
        "folder contents to CMS"
)
public class UploadCommand extends CommandBase implements Callable<Integer> {

    private static final CmsObjectWithChangedAttributesMapper WITH_CHANGED_ATTRIBUTES_MAPPER
        = Mappers.getMapper(CmsObjectWithChangedAttributesMapper.class);

    @Inject
    CmsObjectPathKeyGenerator pathKeyGenerator;

    @Inject
    LocalRemoteObjectTreeComparator localRemoteObjectTreeComparator;

    @Inject
    PathRecursionSupport pathRecursionSupport;

    @Inject
    CmsSectionToTopComparator sectionToTopComparator;

    @CommandLine.ArgGroup
    MutuallyExclusiveGroup exclusiveOptions;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Option(
        names = {"--keep-as-draft"},
        description = "Keep the uploaded content in draft and do not publish"
    )
    private boolean keepAsDraft;

    @CommandLine.Option(
        names = {"--layout"},
        arity = "1",
        paramLabel = "layout_file_name",
        description = "Specify layout for new (not updated) pages"
    )
    private String layoutFilename;

    @CommandLine.Option(
        names = {"-n", "--dry-run"},
        description = "Dry run: do not upload any files; instead, just list " +
            "operations that would be performed"
    )
    private boolean noop;

    @Override
    public Integer call() throws Exception {
        LocalRemoteTreeComparisonDetails treeDetails =
            localRemoteObjectTreeComparator.compareLocalAndRemoteCmsObjectTrees(
                topLevelCommand.getCmsObjects().stream(),
                topLevelCommand.getRootDirectory(),
                !isDeleteMissing());

        Map<String, CmsObject> remoteObjectsByPath = treeDetails.getRemoteObjectsByCmsPath();
        Map<String, Pair<CmsObject, File>> localObjectsByPath = treeDetails.getLocalObjectsByCmsPath();

        Set<String> remotePathsToDelete;
        Set<String> localPathsToUpload;

        if (isUploadAll()) {
            if (isIncludeUnchanged()) {
                localPathsToUpload = new HashSet<>(treeDetails.getLocalObjectsByCmsPath().keySet());
            } else {
                localPathsToUpload = new HashSet<>(treeDetails.getLocalPathsMissingInRemote());
                localPathsToUpload.addAll(treeDetails.getLocalObjectsNewerThanRemote());
            }

            if (isDeleteMissing()) {
                remotePathsToDelete = treeDetails.getRemotePathsMissingInLocal();
            } else {
                remotePathsToDelete = Collections.emptySet();
            }
        } else {
            Map<String, CmsObject> simplifiedLocalObjectsByPath = localObjectsByPath.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getKey()));

            localPathsToUpload = new HashSet<>(
                pathRecursionSupport.calculateSpecifiedPaths(
                    exclusiveOptions.individualFilesUploadGroup.uploadPaths,
                    recursionStyle(),
                    simplifiedLocalObjectsByPath));

            remotePathsToDelete = Collections.emptySet();
        }

        String newPageLayoutSystemName;
        if (layoutFilename == null) {
            newPageLayoutSystemName = treeDetails.getDefaultLayout()
                .map(CmsLayout::systemName)
                .orElse(null);
        } else if (StringUtils.isBlank(layoutFilename)) {
            newPageLayoutSystemName = null;
        } else {
            CmsObject layout = Optional.ofNullable(localObjectsByPath.get(layoutFilename))
                .map(Pair::getLeft)
                .orElseGet(() -> remoteObjectsByPath.get(layoutFilename));

            if (layout instanceof CmsLayout cmsLayout) {
                newPageLayoutSystemName = cmsLayout.systemName();
            } else {
                throw new IllegalArgumentException("Specified layout for new pages is not a layout!");
            }
        }

        if (remotePathsToDelete.isEmpty() && localPathsToUpload.isEmpty()) {
            Log.info("Nothing to do.");
            return 0;
        }

        List<CmsObject> deleteObjects = remotePathsToDelete.stream()
            .map(remoteObjectsByPath::get)
            .sorted(sectionToTopComparator
                .reversed())
            .collect(Collectors.toList());

        Comparator<Pair<CmsObject, File>> uploadSortComparator;
        if (StringUtils.isNotBlank(newPageLayoutSystemName)) {
            uploadSortComparator = Comparator.comparing(Pair::getLeft,
                sectionToTopComparator
                    .thenComparing((o1, o2) -> {
                        if (o1 instanceof CmsLayout layout1 && StringUtils.equals(newPageLayoutSystemName, layout1.systemName())) {
                            return -1;
                        }

                        if (o2 instanceof CmsLayout layout2 && StringUtils.equals(newPageLayoutSystemName, layout2.systemName())) {
                            return 1;
                        }

                        return 0;
                    }));
        } else {
            uploadSortComparator = Comparator.comparing(Pair::getLeft, sectionToTopComparator);
        }

        List<Pair<CmsObject, File>> localObjectsToUpload = localPathsToUpload.stream()
            .map(pathKey -> {
                Pair<CmsObject, File> localObjectPair = localObjectsByPath.get(pathKey);
                CmsObject remoteObject = remoteObjectsByPath.get(pathKey);
                CmsObject localObject = localObjectPair.getLeft();
                File localFile = localObjectPair.getRight();

                if (remoteObject == null) {
                    localObject = withRequiredCreationProperties(localObjectPair.getLeft(), newPageLayoutSystemName);
                } else {
                    localObject = withCopiedObjectId(localObject, remoteObject);
                }

                localObjectPair = Pair.of(localObject, localFile);
                localObjectsByPath.put(pathKey, localObjectPair);

                return localObjectPair;
            })
            .sorted(uploadSortComparator)
            .toList();

        if (noop) {
            for (CmsObject object : deleteObjects) {
                Log.info("Would delete " + object.threescaleObjectType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
            }

            for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                CmsObject object = pair.getLeft();
                Log.info("Would upload " + object.threescaleObjectType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
            }

            if (!keepAsDraft) {
                for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                    CmsObject object = pair.getLeft();

                    if (object.threescaleObjectType() == ThreescaleObjectType.TEMPLATE) {
                        Log.info("Would publish " + object.threescaleObjectType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
                    }
                }
            }

        } else {
            ThreescaleCmsClient client = topLevelCommand.getClient();

            DeleteCommand.deleteObjects(client, pathKeyGenerator, deleteObjects);

            List<CmsTemplate> localTemplatesToPublish = localObjectsToUpload.stream()
                .map(pair -> performUpload(client, pair.getLeft(), pair.getRight(), localObjectsByPath, remoteObjectsByPath))
                .filter(cmsObject -> cmsObject instanceof CmsTemplate)
                .map(o -> (CmsTemplate) o)
                .toList();

            if (!keepAsDraft) {
                for (CmsTemplate cmsTemplate : localTemplatesToPublish) {
                    Log.info("Publishing " + cmsTemplate.threescaleObjectType() + " " + pathKeyGenerator.generatePathKeyForObject(cmsTemplate) + "...");
                    client.publish(cmsTemplate);
                }
            }
        }

        return 0;
    }

    private CmsObject performUpload(@Nonnull ThreescaleCmsClient client,
                                    @Nonnull CmsObject object,
                                    @Nonnull File file,
                                    @Nonnull Map<String, Pair<CmsObject, File>> localFilesByPathKey,
                                    @Nonnull Map<String, CmsObject> remoteObjectsByPathKey) {
        String pathKey = pathKeyGenerator.generatePathKeyForObject(object);
        Log.info("Uploading " + object.threescaleObjectType() + " " + pathKey + "...");

        if (object instanceof CmsSection section) {
            if (section.parentId() == null && !"/".equals(pathKey)) {
                section = WITH_CHANGED_ATTRIBUTES_MAPPER.withParentId(
                    section,
                    findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey)
                );
            }
            return client.save(section);
        } else if (object instanceof CmsFile cmsFile) {
            if (cmsFile.sectionId() == null) {
                cmsFile = WITH_CHANGED_ATTRIBUTES_MAPPER.withSectionId(
                    cmsFile,
                    findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey)
                );
            }
            return client.save(cmsFile, file);
        } else if (object instanceof CmsTemplate template) {
            if (template instanceof CmsPage page && page.sectionId() == null) {
                template = WITH_CHANGED_ATTRIBUTES_MAPPER.withSectionId(
                    page,
                    findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey)
                );
            }
            return client.save(template, file);
        } else {
            throw new UnsupportedOperationException("Unknown object type " + object.getClass());
        }
    }

    private Long findParentId(@Nonnull String pathKey,
                              @Nonnull Map<String, Pair<CmsObject, File>> localFilesByPathKey,
                              @Nonnull Map<String, CmsObject> remoteObjectsByPathKey) {
        String parentPathKey;
        if (pathKey.endsWith("/")) {
            parentPathKey = pathKey.substring(0, pathKey.lastIndexOf('/', pathKey.length() - 2) + 1);
        } else {
            parentPathKey = pathKey.substring(0, pathKey.lastIndexOf('/') + 1);
        }

        do {
            CmsObject potentialParent = Optional.ofNullable(localFilesByPathKey.get(parentPathKey))
                .map(Pair::getLeft)
                .orElse(null);
            if (potentialParent != null && potentialParent.id() != null) {
                return potentialParent.id();
            }

            potentialParent = remoteObjectsByPathKey.get(parentPathKey);
            if (potentialParent != null) {
                return potentialParent.id();
            }

            parentPathKey = parentPathKey.substring(0, pathKey.lastIndexOf('/', parentPathKey.length() - 2) + 1);
        } while (!parentPathKey.isEmpty());

        throw new IllegalStateException("Couldn't find any parent section ID... not even root");
    }

    private CmsObject withRequiredCreationProperties(@Nonnull CmsObject localObject,
                                                     @Nullable String newPageLayoutSystemName) {
        if (newPageLayoutSystemName != null
            && localObject instanceof CmsPage localPage
            && "text/html".equals(localPage.contentType())
        ) {
            return WITH_CHANGED_ATTRIBUTES_MAPPER.withLayout(
                localPage,
                newPageLayoutSystemName
            );
        }

        return localObject;
    }

    private CmsObject withCopiedObjectId(@Nonnull CmsObject target,
                                         @Nonnull CmsObject source) {
        if (target.threescaleObjectType() != source.threescaleObjectType()) {
            throw new IllegalArgumentException("Cannot change types with update. " +
                "Source type: " + source.threescaleObjectType() + "; " +
                "Target type: " + target.threescaleObjectType());
        }

        // Only set ID... Leave the rest for 3scale to dictate what's updatable
        // and not on each type of object.

        if (target instanceof CmsSection targetSection) {
            CmsSection sourceSection = (CmsSection) source;

            return WITH_CHANGED_ATTRIBUTES_MAPPER.withIdAndParentId(
                targetSection,
                sourceSection.id(),
                sourceSection.parentId()
            );
        } else if (target instanceof CmsFile targetFile) {
            CmsFile sourceFile = (CmsFile) source;

            return WITH_CHANGED_ATTRIBUTES_MAPPER.withIdAndSectionId(
                targetFile,
                sourceFile.id(),
                sourceFile.sectionId()
            );
        } else if (target instanceof CmsLayout targetLayout) {
            return WITH_CHANGED_ATTRIBUTES_MAPPER.withId(targetLayout, source.id());
        } else if (target instanceof CmsPage targetPage) {
            if (source instanceof CmsPage sourcePage) {
                return WITH_CHANGED_ATTRIBUTES_MAPPER.withIdAndSectionId(
                    targetPage,
                    sourcePage.id(),
                    sourcePage.sectionId()
                );
            } else {
                return WITH_CHANGED_ATTRIBUTES_MAPPER.withIdAndSectionId(
                    targetPage,
                    source.id(),
                    null
                );
            }
        } else if (target instanceof CmsPartial targetPartial) {
            return WITH_CHANGED_ATTRIBUTES_MAPPER.withId(
                targetPartial,
                source.id()
            );
        } else {
            // Unknown... built-in pages/partials shouldn't come from local
            // files anyway
            throw new UnsupportedOperationException("Unknown how to set ID for " + target.threescaleObjectType());
        }
    }

    private boolean isUploadAll() {
        return exclusiveOptions == null
            || exclusiveOptions.individualFilesUploadGroup == null
            || exclusiveOptions.individualFilesUploadGroup.uploadPaths == null
            || exclusiveOptions.individualFilesUploadGroup.uploadPaths.isEmpty();
    }

    private boolean isIncludeUnchanged() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesUploadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.includeUnchanged)
            .orElse(false);
    }

    private boolean isDeleteMissing() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesUploadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.deleteMissing)
            .orElse(false);
    }

    private PathRecursionSupport.RecursionOption recursionStyle() {
        boolean shouldRecurse = Optional.ofNullable(exclusiveOptions)
            .map(options -> options.individualFilesUploadGroup)
            .map(individualFilesDownloadGroup -> individualFilesDownloadGroup.recurseSubdirectories)
            .orElse(false);

        if (shouldRecurse) {
            return PathRecursionSupport.RecursionOption.PATH_PREFIX;
        } else {
            return PathRecursionSupport.RecursionOption.NONE;
        }
    }

    private static class MutuallyExclusiveGroup {

        @CommandLine.ArgGroup(exclusive = false)
        AllFilesUploadGroup allFilesUploadGroup;

        @CommandLine.ArgGroup(exclusive = false)
        IndividualFilesUploadGroup individualFilesUploadGroup;

    }

    private static class AllFilesUploadGroup {

        @CommandLine.Option(
            names = {"-u", "--include-unchanged"},
            description = {
                "Include unchanged files in upload; if not set, only " +
                    "changed and new files will be uploaded",
                "(note: only usable when uploading all local contents)"
            },
            defaultValue = "false"
        )
        boolean includeUnchanged;

        @CommandLine.Option(
            names = {"-d", "--delete-missing"},
            description = {
                "Delete remote objects that are missing from local filesystem",
                "(note: only usable when uploading all local contents)"
            },
            defaultValue = "false"
        )
        boolean deleteMissing;

    }

    private static class IndividualFilesUploadGroup {

        @CommandLine.Option(
            names = {"-r", "--recurse"},
            description = {
                "Flag to recurse subdirectories when uploading"
            },
            defaultValue = "false"
        )
        boolean recurseSubdirectories;

        @CommandLine.Parameters(
            paramLabel = "PATH",
            arity = "1..*",
            description = {
                "Paths to upload from local files into 3scale CMS",
                "(note: will upload regardless of last-modified timestamps)"
            }
        )
        List<String> uploadPaths;

    }
}
