package ru.runa.gpd.lang.action;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.FileEditorInput;

import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.ActionSubProcess;
import ru.runa.gpd.lang.model.ActionSubProcessDefinition;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class EmbeddedActionSubProcessActionsDelegate extends BaseModelDropDownActionDelegate {
    private String selectedName;
    private ProcessDefinition definition;
    private ActionSubProcess actionsubprocess;
    private IFile definitionFile;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        actionsubprocess = getSelection();
        if (actionsubprocess != null) {
            if (actionsubprocess.getClass() == ActionSubProcess.class) {
                definition = actionsubprocess.getProcessDefinition();
                selectedName = actionsubprocess.getActionSubProcessName();
                action.setChecked(actionsubprocess.isEmbedded());
                definitionFile = ((FileEditorInput) getActiveEditor().getEditorInput()).getFile();
                action.setEnabled(true);
            } else {
                action.setEnabled(false);
            }
        }
    }

    /**
     * Fills the menu with applicable launch shortcuts
     * 
     * @param menu
     *            The menu to fill
     */
    @Override
    protected void fillMenu(Menu menu) {
        if (actionsubprocess.getClass() != ActionSubProcess.class) {
            return;
        }
        List<String> allNames = Lists.newArrayList();
        ProcessDefinition mainProcessDefinition = definition.getMainProcessDefinition();
        for (ActionSubProcessDefinition actionsubprocessDefinition : mainProcessDefinition.getEmbeddedActionSubProcesses().values()) {
            allNames.add(actionsubprocessDefinition.getName());
        }
        List<String> usedNames = Lists.newArrayList();
        usedNames.add(definition.getName());
        for (ActionSubProcess actionsubprocess : mainProcessDefinition.getChildren(ActionSubProcess.class)) {
            if (actionsubprocess.isEmbedded() && !Objects.equal(actionsubprocess, this.actionsubprocess)) {
                usedNames.add(actionsubprocess.getActionSubProcessName());
            }
        }
        for (ActionSubProcessDefinition actionsubprocessDefinition : mainProcessDefinition.getEmbeddedActionSubProcesses().values()) {
            for (ActionSubProcess actionsubprocess : actionsubprocessDefinition.getChildren(ActionSubProcess.class)) {
                if (actionsubprocess.isEmbedded() && !Objects.equal(actionsubprocess, this.actionsubprocess)) {
                    usedNames.add(actionsubprocess.getActionSubProcessName());
                }
            }
        }
        Collections.sort(allNames);
        for (String actionsubprocessName : allNames) {
            Action action = new SetEmbeddedSubprocessAction();
            action.setText(actionsubprocessName);
            if (actionsubprocess.isEmbedded() && Objects.equal(selectedName, actionsubprocessName)) {
                action.setChecked(true);
            }
            action.setEnabled(!usedNames.contains(actionsubprocessName));
            ActionContributionItem item = new ActionContributionItem(action);
            item.fill(menu, -1);
        }
        new MenuItem(menu, SWT.SEPARATOR);
        Action action;
        ActionContributionItem item;
        action = new CreateEmbeddedSubprocessAction();
        item = new ActionContributionItem(action);
        item.fill(menu, -1);
    }

    private void createEmbeddedSubprocess() {
        IStructuredSelection selection = new StructuredSelection(definitionFile.getParent());
        ProcessDefinition created = WorkspaceOperations.createNewProcessDefinition(selection, ProcessDefinitionAccessType.EmbeddedSubprocess);
        if (created != null) {
            setEmbeddedActionSubProcess(created.getName());
        }
    }

    private void setEmbeddedActionSubProcess(String name) {
    	actionsubprocess.setAsync(false);
    	actionsubprocess.setEmbedded(true);
    	actionsubprocess.setActionSubProcessName(name);
    }

    private class SetEmbeddedSubprocessAction extends Action {
        @Override
        public void run() {
            setEmbeddedActionSubProcess(getText());
            WorkspaceOperations.openActionSubProcessDefinition(actionsubprocess);
        }
    }

    private class CreateEmbeddedSubprocessAction extends Action {
        public CreateEmbeddedSubprocessAction() {
            setText(Localization.getString("ExplorerTreeView.menu.label.newEmbeddedSubprocess"));
        }

        @Override
        public void run() {
            createEmbeddedSubprocess();
        }
    }

}