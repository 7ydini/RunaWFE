package ru.runa.gpd.editor.gef.part.graph;

import java.util.List;

import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;

import ru.runa.gpd.lang.model.ActionSubProcess;
import ru.runa.gpd.util.WorkspaceOperations;

public class ActionSubProcessGraphicalEditPart extends LabeledNodeGraphicalEditPart {
    @Override
    public ActionSubProcess getModel() {
        return (ActionSubProcess) super.getModel();
    }

    @Override
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            WorkspaceOperations.openActionSubProcessDefinition(getModel());
        } else {
            super.performRequest(request);
        }
    }

    @Override
    protected void fillFigureUpdatePropertyNames(List<String> list) {
        super.fillFigureUpdatePropertyNames(list);
        list.add(PROPERTY_ACTIONSUBPROCESS);
        list.add(PROPERTY_MINIMAZED_VIEW);
        list.add(PROPERTY_CHILDREN_CHANGED);
        list.add(PROPERTY_ASYNC);
    }
}
