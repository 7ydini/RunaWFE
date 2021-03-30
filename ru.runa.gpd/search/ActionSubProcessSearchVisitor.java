package ru.runa.gpd.search;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.text.Match;

import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.ActionSubProcess;

import com.google.common.base.Objects;

public class ActionSubProcessSearchVisitor extends ProcessDefinitionsVisitor {
    
    public ActionSubProcessSearchVisitor(ActionSubProcessSearchQuery query) {
        super(query);
    }

    @Override
    protected void findInProcessDefinition(IFile definitionFile, ProcessDefinition processDefinition) {
        List<ActionSubProcess> actionsubprocesses = processDefinition.getChildren(ActionSubProcess.class);
        for (ActionSubProcess actionsubprocess : actionsubprocesses) {
            if (Objects.equal(actionsubprocess.getActionSubProcessName(), query.getSearchText())) {
                ElementMatch elementMatch = new ElementMatch(actionsubprocess, definitionFile, ElementMatch.CONTEXT_PROCESS_DEFINITION);
                query.getSearchResult().addMatch(new Match(elementMatch, 0, 0));
                elementMatch.setMatchesCount(elementMatch.getMatchesCount() + 1);
            }
        }
    }

}
