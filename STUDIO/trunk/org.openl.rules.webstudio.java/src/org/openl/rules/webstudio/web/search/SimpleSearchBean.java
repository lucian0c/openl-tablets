package org.openl.rules.webstudio.web.search;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.ui.ProjectIndexer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean providing logic for Simple Search page of OpenL Studio.
 */
public class SimpleSearchBean {

    private String searchQuery;
    private String[][] searchResults;

    public SimpleSearchBean() {
        initSearchQuery();

        if (StringUtils.isNotBlank(searchQuery)) {
            search();
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String[][] getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(String[][] searchResults) {
        this.searchResults = searchResults;
    }

    private void initSearchQuery() {
        String searchQuery = FacesUtils.getRequestParameter("searchQuery");
        if (StringUtils.isNotBlank(searchQuery)) {
            setSearchQuery(searchQuery);
        }
    }

    public String search() {
        String[][] searchResults = {};

        if (StringUtils.isNotBlank(searchQuery)) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            ProjectModel projectModel = studio.getModel();
            if (projectModel != null) {
                ProjectIndexer projectIndexer = projectModel.getIndexer();
                if (projectIndexer != null) {
                    searchResults = projectModel.getIndexer().getResultsForQuery(searchQuery, 200, null);
                }
            }
        }

        setSearchResults(searchResults);

        return null;
    }

}
