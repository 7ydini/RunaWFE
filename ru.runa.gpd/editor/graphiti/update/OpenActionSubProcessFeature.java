package ru.runa.gpd.editor.graphiti.update;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import ru.runa.gpd.lang.model.ActionSubProcess;
import ru.runa.gpd.util.WorkspaceOperations;

public class OpenActionSubProcessFeature extends AbstractCustomFeature {
    public OpenActionSubProcessFeature(IFeatureProvider fp) {
        super(fp);
    }

    @Override
    public boolean canExecute(ICustomContext context) {
        return true;
    }

    @Override
    public void execute(ICustomContext context) {
        ActionSubProcess actionsubprocess = (ActionSubProcess) getFeatureProvider().getBusinessObjectForPictogramElement(context.getInnerPictogramElement());
        WorkspaceOperations.openActionSubProcessDefinition(actionsubprocess);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
