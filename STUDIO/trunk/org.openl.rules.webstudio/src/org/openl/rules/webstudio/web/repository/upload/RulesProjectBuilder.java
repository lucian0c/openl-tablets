package org.openl.rules.webstudio.web.repository.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.uw.UserWorkspace;

import java.io.InputStream;

public class RulesProjectBuilder {
    private final Log log = LogFactory.getLog(RulesProjectBuilder.class);
    private final RulesProject project;
    

    public RulesProjectBuilder(UserWorkspace workspace, String projectName) throws ProjectException {
        workspace.createProject(projectName);
        project = workspace.getProject(projectName);
        project.edit();
    }
    
    protected RulesProject getProject() {
        return project;
    }

    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        AProjectFolder folder = project;
        String resName;

        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            String path = fileName.substring(0, pos);
            resName = fileName.substring(pos + 1);

            folder = checkPath(project, path);
        } else {
            resName = fileName;
        }

        // throws exception if name is invalid
        checkName(resName);

        folder.addResource(resName, inputStream);

        return true;
    }

    public boolean addFolder(String folderName) throws ProjectException {        
        folderName = folderName.substring(0, folderName.length() - 1);

        checkPath(project, folderName);

        return true;
    }

    public void cancel() {
        // it was created it will be perish
        try {
            if (log.isDebugEnabled()) {
                log.debug("Canceling uploading of new project");
            }

            project.close();
            project.delete();
            project.erase();
        } catch (ProjectException e) {
            log.error("Failed to cancel new project", e);
        }
    }

    public void save() throws ProjectException {
        project.save();
    }

    private void checkName(String artefactName) throws ProjectException {
        if (!NameChecker.checkName(artefactName)) {
            throw new ProjectException("File or folder name '" + artefactName + "' is invalid. "
                    + NameChecker.BAD_NAME_MSG);
        }
    }

    private AProjectFolder checkPath(AProject project, String fullName) throws ProjectException {
        ArtefactPathImpl ap = new ArtefactPathImpl(fullName);
        AProjectFolder current = project;
        for (String segment : ap.getSegments()) {
            if (current.hasArtefact(segment)) {
                current = (AProjectFolder) current.getArtefact(segment);
            } else {
                // throws exception if name is invalid
                checkName(segment);

                current = current.addFolder(segment);
            }
        }

        return current;
    }
}
