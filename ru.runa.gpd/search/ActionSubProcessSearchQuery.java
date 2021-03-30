package ru.runa.gpd.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class ActionSubProcessSearchQuery extends BaseSearchQuery {
    
    public ActionSubProcessSearchQuery(String processDefinitionName) {
        super(processDefinitionName, "any");
    }

    @Override
    public IStatus run(final IProgressMonitor monitor) {
        getSearchResult().removeAll();
        return new ActionSubProcessSearchVisitor(this).search(monitor);
    }

}
