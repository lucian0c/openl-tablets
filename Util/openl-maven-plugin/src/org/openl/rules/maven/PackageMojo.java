package org.openl.rules.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.openl.info.OpenLVersion;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.ProjectPackager;
import org.openl.util.StringUtils;
import org.openl.util.ZipArchiver;
import org.openl.util.ZipUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Packages an OpenL Tablets project in a ZIP archive.
 *
 * @author Yury Molchan
 * @since 5.19.1
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresDependencyCollection = ResolutionScope.RUNTIME)
public final class PackageMojo extends BaseOpenLMojo {

    private static final String DEPLOYMENT_YAML = "deployment.yaml";
    private static final String DEPLOYMENT_CLASSIFIER = "deployment";

    @Parameter(defaultValue = "${project.packaging}", readonly = true)
    private String packaging;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the generated artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File classesDirectory;

    /**
     * Comma separated list of packaging formats. Supported values: zip, jar.
     */
    @Parameter(defaultValue = "zip")
    private String format;

    /**
     * Folder to store dependencies inside the OpenL Tablets project.
     */
    @Parameter(defaultValue = "lib/")
    private String classpathFolder;

    /**
     * Classifier that identifies the generated artifact as a supplemental one. By default, if a classifier is not
     * provided, the system creates the artifact as the main one. Maven does not support using multiple main artifacts.
     * Upon the second attempt to create the main artifact without using a classifier, the build fails.
     */
    @Parameter
    private String classifier;

    /**
     * Allowed quantity of dependencies which can be included into the ZIP archive. Usually OpenL Tablets rules require
     * a few dependencies, such as domain models, that is, Java beans, or some utils, for example, JSON parsing.
     * Typically, the quantity of required dependencies does not exceed 3. If transitive dependencies are declared
     * incorrectly, the size of the ZIP package increases dramatically. This parameter allows to prevent such situation
     * by failing packaging.
     */
    @Parameter(defaultValue = "3", required = true)
    private int dependenciesThreshold;

    /**
     * Parameter that enables deployed zip generation. This zip includes an exploded main OpenL Tablets project and all
     * dependent OpenL Tablets projects located in separated folders inside the archive.
     */
    @Parameter(defaultValue = "false")
    private boolean deploymentPackage;

    /**
     * Deployment archive name.
     */
    @Parameter(defaultValue = "${project.build.finalName}")
    private String deploymentName;

    /**
     * Marker if MANIFEST.MF file must be generated and included into deployment archive
     */
    @Parameter(defaultValue = "true")
    private boolean addDefaultManifest;

    /**
     * Configuration properties for MANIFEST.MF. Supports all possible MANIFEST.MF attributes.
     */
    @Parameter
    private Map<String, String> manifestEntries;

    @Parameter(defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {

        File openLSourceDir = new File(sourcePath);
        if (CollectionUtils.isEmpty(openLSourceDir.list())) {
            info("No OpenL sources have been found at '", sourcePath, "' path");
            info("Skipping packaging of the empty OpenL project.");
            return;
        }
        String[] types = StringUtils.split(format, ',');
        if (CollectionUtils.isEmpty(types)) {
            throw new MojoFailureException("No formats have been defined in the plugin configuration.");
        }
        File dependencyLib = project.getArtifact().getFile();

        boolean mainArtifactExists = dependencyLib != null && dependencyLib.isFile();
        if (mainArtifactExists && StringUtils.isBlank(classifier) && Arrays.asList(types).contains(packaging)) {
            error("The main artifact have been attached already.");
            error(
                "You have to use classifier to attach supplemental artifacts to the project instead of replacing them.");
            throw new MojoFailureException("It is not possible to replace the main artifact.");
        }
        Set<Artifact> dependencies = getDependencies();
        int dependenciesSize = dependencies.size();
        if (dependenciesSize > dependenciesThreshold) {
            error("The quantity of dependencies (",
                dependenciesSize,
                ") exceeds the defined threshold in 'dependenciesThreshold=",
                dependenciesThreshold,
                "' parameter.");
            for (Artifact artifact : dependencies) {
                error("    : ", artifact);
            }
            throw new MojoFailureException("The quantity of dependencies exceeds the limit");
        }
        if (!mainArtifactExists && CollectionUtils.isNotEmpty(classesDirectory.list())) {
            // create a jar file with compiled Java sources for OpenL rules
            dependencyLib = File.createTempFile(finalName, "-lib.jar", outputDirectory);

            JarArchiver.archive(classesDirectory, dependencyLib);
        }

        for (String type : types) {
            File outputFile = getOutputFile(outputDirectory, finalName, classifier, type);

            try (ZipArchiver arch = new ZipArchiver(outputFile.toPath())) {
                if (addDefaultManifest || manifestEntries != null) {
                    Manifest manifest = createManifest();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    manifest.write(baos);
                    arch.addFile(new ByteArrayInputStream(baos.toByteArray()), JarFile.MANIFEST_NAME);
                }

                ProjectPackager.addOpenLProject(openLSourceDir, arch);

                if (dependencyLib != null && dependencyLib.isFile()) {
                    arch.addFile(dependencyLib, classpathFolder + finalName + ".jar");
                }
                for (Artifact artifact : dependencies) {
                    File file = artifact.getFile();
                    arch.addFile(file, classpathFolder + file.getName());
                }
            }

            if (mainArtifactExists || StringUtils.isNotBlank(classifier)) {
                info("Attaching the supplemental artifact '", outputFile, ",");
                projectHelper.attachArtifact(project, type, classifier, outputFile);
            } else {
                info("Registering the main artifact '", outputFile, ",");
                mainArtifactExists = true;
                project.getArtifact().setFile(outputFile);
            }
        }

        if (deploymentPackage) {
            File outputDeploymentDir = new File(outputDirectory, finalName + "-" + DEPLOYMENT_CLASSIFIER);
            if (outputDeploymentDir.isDirectory()) {
                info("Cleaning up '", outputDeploymentDir, "' directory...");
                FileUtils.delete(outputDeploymentDir);
            }
            outputDeploymentDir.mkdir();
            Set<Artifact> openLDependencies = getOpenLDependencies();
            for (Artifact openLArtifact : openLDependencies) {
                File artifactFile = openLArtifact.getFile();
                unpackZip(outputDeploymentDir, openLArtifact.getArtifactId(), artifactFile);
            }
            unpackZip(outputDeploymentDir, project.getArtifact().getArtifactId(), project.getArtifact().getFile());
            generateDeploymentFile(outputDeploymentDir);

            File outputFile = getOutputFile(outputDirectory,
                deploymentName,
                DEPLOYMENT_CLASSIFIER,
                OPENL_ARTIFACT_TYPE);
            ZipUtils.archive(outputDeploymentDir, outputFile);

            info("Attaching the deployment artifact '", outputFile, ",");
            projectHelper.attachArtifact(project, OPENL_ARTIFACT_TYPE, DEPLOYMENT_CLASSIFIER, outputFile);
        }
    }

    private Set<Artifact> getDependencies() {
        HashSet<String> allowed = getAllowedDependencies();

        Set<Artifact> dependencies = new HashSet<>();
        for (Artifact artifact : project.getArtifacts()) {
            String groupId = artifact.getGroupId();
            String type = artifact.getType();
            String scope = artifact.getScope();
            if (skipToProcess(groupId, type, scope)) {
                debug("SKIP : ", artifact);
                continue;
            }
            List<String> dependencyTrail = artifact.getDependencyTrail();
            if (dependencyTrail.size() < 2) {
                debug("SKIP : ", artifact, " (by dependency depth)");
                continue; // skip, unexpected size of dependencies
            }
            if (skipTransitiveDependency(dependencyTrail)) {
                debug("SKIP : ", artifact, " (transitive dependency from OpenL or SLF4j dependencies)");
                continue;
            }
            String tr = dependencyTrail.get(1);
            String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
            if (allowed.contains(key)) {
                debug("ADD : ", artifact);
                dependencies.add(artifact);
            }
        }

        return dependencies;
    }

    private HashSet<String> getAllowedDependencies() {
        HashSet<String> allowed = new HashSet<>();
        for (Dependency dep : project.getDependencies()) {
            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            String type = dep.getType();
            String scope = dep.getScope();
            if (skipToProcess(groupId, type, scope)) {
                debug("SKIP : ", dep);
            } else {
                allowed.add(ArtifactUtils.versionlessKey(groupId, artifactId));
            }
        }
        return allowed;
    }

    private boolean skipToProcess(String groupId, String type, String scope) {
        return !isRuntimeScope(scope) || groupId.equals("org.openl.rules") || groupId.equals("org.openl") || groupId
            .equals("org.slf4j") || OPENL_ARTIFACT_TYPE.equals(type);
    }

    private boolean isRuntimeScope(String scope) {
        return Artifact.SCOPE_RUNTIME.equals(scope) || Artifact.SCOPE_COMPILE.equals(scope);
    }

    private boolean skipTransitiveDependency(List<String> dependencyTrail) {
        for (int i = 1; i < dependencyTrail.size() - 1; i++) {
            String dependency = dependencyTrail.get(i);
            if (dependency.startsWith("org.openl.rules:") || dependency.startsWith("org.openl:") || dependency
                .startsWith("org.slf4j:")) {
                return true;
            }
        }
        return false;
    }

    @Override
    String getHeader() {
        return "OPENL PACKAGING";
    }

    /**
     * Returns the Jar file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param resultFinalName the name of the ear file
     * @param classifier an optional classifier
     * @return the file to generate
     */
    private File getOutputFile(File basedir, String resultFinalName, String classifier, String format) {
        Objects.requireNonNull(basedir, "basedir is not allowed to be null.");
        Objects.requireNonNull(resultFinalName, "finalName is not allowed to be null.");

        StringBuilder fileName = new StringBuilder(resultFinalName);

        if (StringUtils.isNotBlank(classifier)) {
            fileName.append('-').append(classifier);
        }

        fileName.append('.').append(format);

        return new File(basedir, fileName.toString());
    }

    private Set<Artifact> getOpenLDependencies() {
        Set<Artifact> openLDependencies = new HashSet<>();
        for (Artifact artifact : project.getArtifacts()) {
            if (OPENL_ARTIFACT_TYPE.equals(artifact.getType()) && isRuntimeScope(artifact.getScope())) {
                openLDependencies.add(artifact);
                debug("ADD : ", artifact);
            }
        }
        return openLDependencies;
    }

    private void unpackZip(File baseDir, String name, File zip) throws IOException {
        File outDir = new File(baseDir, name);
        ZipUtils.extractAll(zip, outDir);
    }

    private void generateDeploymentFile(File baseDir) throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", deploymentName);
        try (FileWriter writer = new FileWriter(new File(baseDir, DEPLOYMENT_YAML))) {
            DumperOptions options = new DumperOptions();
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            new Yaml(options).dump(properties, writer);
        }
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        //initialize with default values
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Build-Date", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        attributes.putValue("Built-By", session.getSystemProperties().getProperty("user.name"));
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, String.format("%s:%s", project.getGroupId(), project.getArtifactId()));
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION,  project.getVersion());
        if (project.getOrganization() != null) {
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, project.getOrganization().getName());
        }
        attributes.putValue("Created-By", "OpenL Maven Plugin v" + OpenLVersion.getVersion());

        if (manifestEntries != null) {
            for (Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                String key = entry.getKey();
                //if value is empty, create an entry with empty string to prevent nulls in file
                String value = StringUtils.trimToEmpty(entry.getValue());
                attributes.putValue(key, value);
            }
        }
        return manifest;
    }
}
