package org.openl.rules.webstudio.dependencies;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractProjectDependencyManager {

    private final Logger log = LoggerFactory.getLogger(WebStudioWorkspaceRelatedDependencyManager.class);

    private List<IDependencyLoader> dependencyLoaders;

    private List<ProjectDescriptor> projects;

    private final List<DependencyManagerListener> listeners = new ArrayList<DependencyManagerListener>();

    private final List<String> moduleNames = new ArrayList<String>();

    private Collection<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();

    private boolean singleModuleMode = false;

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects,
                                                      boolean singleModuleMode) {
        super();
        if (projects == null) {
            throw new IllegalArgumentException("projects can't be null!");
        }

        this.projects = projects;
        this.singleModuleMode = singleModuleMode;
    }

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects) {
        this(projects, false);
    }

    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        CompiledDependency loadedDependency = super.loadDependency(dependency);
        for (DependencyManagerListener listener : listeners) {
            listener.onLoadDependency(loadedDependency);
        }
        return loadedDependency;
    }

    @Override
    protected Collection<ProjectDescriptor> getProjectDescriptors() {
        return projectDescriptors;
    }

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders != null) {
            return dependencyLoaders;
        }
        dependencyLoaders = new ArrayList<IDependencyLoader>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        dependencyLoaders.add(new WebStudioDependencyLoader(m.getName(),
                                Arrays.asList(m),
                                singleModuleMode));
                        moduleNames.add(m.getName());
                    }
                }

                String dependencyName = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName());
                IDependencyLoader projectLoader = new WebStudioDependencyLoader(dependencyName,
                        project.getModules(),
                        singleModuleMode);
                projectDescriptors.add(project);
                dependencyLoaders.add(projectLoader);
            } catch (Exception e) {
                log.error("Build dependency manager loaders for project {} was failed!", project.getName(), e);
            }
        }

        return dependencyLoaders;
    }

    @Override
    public void reset(IDependency dependency) {
        for (DependencyManagerListener listener : listeners) {
            listener.onResetDependency(dependency);
        }

        if (dependencyLoaders == null) {
            return;
        }

        String dependencyName = dependency.getNode().getIdentifier();

        ProjectDescriptor projectToReset = null;

        searchProject:
        for (ProjectDescriptor project : projects) {
            if (dependencyName.equals(ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName()))) {
                projectToReset = project;
                break;
            }

            for (Module module : project.getModules()) {
                if (dependencyName.equals(module.getName())) {
                    projectToReset = project;
                    break searchProject;
                }
            }
        }

        if (projectToReset != null) {
            clearClassLoader(projectToReset.getName());
            String projectDependency = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(projectToReset.getName());

            for (IDependencyLoader dependencyLoader : dependencyLoaders) {
                WebStudioDependencyLoader loader = (WebStudioDependencyLoader) dependencyLoader;
                String loaderDependencyName = loader.getDependencyName();

                if (loaderDependencyName.equals(projectDependency)) {
                    loader.reset();
                }

                for (Module module : projectToReset.getModules()) {
                    if (loaderDependencyName.equals(module.getName())) {
                        loader.reset();
                    }
                }
            }
        }
    }

    @Override
    public void resetAll() {
        if (dependencyLoaders == null) {
            return;
        }

        clearAllClassLoader();

        for (IDependencyLoader dependencyLoader : dependencyLoaders) {
            ((WebStudioDependencyLoader) dependencyLoader).reset();
        }
    }

    public void addListener(DependencyManagerListener listener) {
        for (DependencyManagerListener l : listeners) {
            if (l == listener) {
                // Already added
                return;
            }
        }
        listeners.add(listener);
    }

    public void removeListener(DependencyManagerListener listener) {
        for (Iterator<DependencyManagerListener> iterator = listeners.iterator(); iterator.hasNext(); ) {
            DependencyManagerListener next = iterator.next();
            if (next == listener) {
                iterator.remove();
            }
        }
    }

    public List<String> getModuleNames() {
        return moduleNames;
    }
}
