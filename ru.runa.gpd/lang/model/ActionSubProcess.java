package ru.runa.gpd.lang.model;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import ru.runa.gpd.Activator;
import ru.runa.gpd.Localization;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.extension.VariableFormatArtifact;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.bpmn.IBoundaryEventContainer;
import ru.runa.gpd.settings.LanguageElementPreferenceNode;
import ru.runa.gpd.settings.PrefConstants;
import ru.runa.gpd.util.VariableMapping;
import ru.runa.gpd.util.VariableUtils;
import ru.runa.wfe.lang.AsyncCompletionMode;

public class ActionSubProcess extends Node implements Synchronizable, IBoundaryEventContainer {
    protected String actionSubProcessName = "";
    protected List<VariableMapping> variableMappings = Lists.newArrayList();
    private boolean embedded;
    private boolean async;
    private boolean transactional;
    protected boolean validateAtStart;
    private AsyncCompletionMode asyncCompletionMode = AsyncCompletionMode.ON_MAIN_PROCESS_END;
    public static List<String> PLACEHOLDERS = Lists.newArrayList(VariableUtils.CURRENT_PROCESS_ID, VariableUtils.CURRENT_PROCESS_DEFINITION_NAME,
            VariableUtils.CURRENT_NODE_ID, VariableUtils.CURRENT_NODE_NAME);

    @Override
    public void validate(List<ValidationError> errors, IFile definitionFile) {
        super.validate(errors, definitionFile);
        if (actionSubProcessName == null || actionSubProcessName.length() == 0) {
            errors.add(ValidationError.createLocalizedError(this, "actionsubprocess.empty"));
            return;
        }
        if (embedded) {
            if (getLeavingTransitions().size() != 1) {
                errors.add(ValidationError.createLocalizedError(this, "actionsubprocess.embedded.required1leavingtransition"));
            }
        }
        ProcessDefinition actionSubProcessDefinition = ProcessCache.getFirstProcessDefinition(actionSubProcessName, null);
        if (actionSubProcessDefinition == null) {
            errors.add(ValidationError.createLocalizedWarning(this, "actionsubprocess.notFound"));
            return;
        }
        for (VariableMapping mapping : variableMappings) {
            if (mapping.isText() || mapping.isMultiinstanceLinkByRelation()) {
                continue;
            }
            if (VariableUtils.isVariableNameWrapped(mapping.getName()) && PLACEHOLDERS.contains(mapping.getName())) {
                continue;
            }
            Variable processVariable = VariableUtils.getVariableByName(getProcessDefinition(), mapping.getName());
            if (processVariable == null) {
                errors.add(ValidationError.createLocalizedError(this, "actionsubprocess.processVariableDoesNotExist", mapping.getName()));
                continue;
            }
            if (mapping.isSyncable()) {
                checkSyncModeUsage(errors, mapping, actionSubProcessDefinition);
            }
            Variable actionSubProcessVariable = VariableUtils.getVariableByName(actionSubProcessDefinition, mapping.getMappedName());
            if (actionSubProcessVariable == null) {
                errors.add(ValidationError.createLocalizedWarning(this, "actionsubprocess.subProcessVariableDoesNotExist", mapping.getMappedName()));
                continue;
            }
            if (!isCompatibleVariables(mapping, processVariable, actionSubProcessVariable)) {
                VariableFormatArtifact artifact1 = VariableFormatRegistry.getInstance().getArtifactNotNull(processVariable.getFormatClassName());
                VariableFormatArtifact artifact2 = VariableFormatRegistry.getInstance().getArtifactNotNull(actionSubProcessVariable.getFormatClassName());
                errors.add(ValidationError.createLocalizedWarning(this, "actionsubprocess.variableMappingIncompatibleTypes", processVariable.getName(),
                        artifact1.getLabel(), actionSubProcessVariable.getName(), artifact2.getLabel()));
            }
        }
        if (isAsync()) {
            String propertyName = LanguageElementPreferenceNode.getId(this.getTypeDefinition(), getProcessDefinition().getLanguage()) + '.'
                    + PrefConstants.P_LANGUAGE_ACTION_SUB_PROCESS_ASYNC_INPUT_DATA;
            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            boolean inputDataAllowedInAsyncActionSubProcess = store.contains(propertyName) ? store.getBoolean(propertyName) : false;
            for (VariableMapping mapping : variableMappings) {
                if (isAsync() && mapping.isWritable() && !inputDataAllowedInAsyncActionSubProcess) {
                    errors.add(ValidationError.createLocalizedWarning(this, "actionsubprocess.asyncVariablesInput"));
                    break;
                }
            }
        }
    }

    protected void checkSyncModeUsage(List<ValidationError> errors, VariableMapping mapping, ProcessDefinition actionSubProcessDefinition) {
        if (actionSubProcessDefinition.getSwimlaneByName(mapping.getMappedName()) != null) {
            errors.add(ValidationError.createLocalizedWarning(this, "actionsubprocess.actionSubProcessSwimlaneSyncNotSupported", mapping.getMappedName()));
        }
    }

    protected boolean isCompatibleVariables(VariableMapping mapping, Variable variable1, Variable variable2) {
        if (variable1.getUserType() != null && Objects.equal(variable1.getUserType(), variable2.getUserType())) {
            return true;
        }
        if (VariableFormatRegistry.isAssignableFrom(variable1.getJavaClassName(), variable2.getJavaClassName())) {
            return true;
        }
        return VariableFormatRegistry.isAssignableFrom(variable2.getJavaClassName(), variable1.getJavaClassName());
    }

    public List<VariableMapping> getVariableMappings() {
        return variableMappings;
    }

    public void setVariableMappings(List<VariableMapping> variablesList) {
        this.variableMappings.clear();
        this.variableMappings.addAll(variablesList);
        setDirty();
    }

    public void setActionSubProcessName(String actionSubProcessName) {
        String old = this.actionSubProcessName;
        this.actionSubProcessName = actionSubProcessName;
        firePropertyChange(PROPERTY_ACTIONSUBPROCESS, old, this.actionSubProcessName);
    }

    public String getActionSubProcessName() {
        return actionSubProcessName;
    }

    public ActionSubProcessDefinition getEmbeddedActionSubProcess() {
        return getProcessDefinition().getMainProcessDefinition().getEmbeddedActionSubProcessByName(getActionSubProcessName());
    }

    @Override
    protected boolean allowLeavingTransition(List<Transition> transitions) {
        return super.allowLeavingTransition(transitions) && transitions.size() == 0;
    }

    @Override
    public void populateCustomPropertyDescriptors(List<IPropertyDescriptor> descriptors) {
        super.populateCustomPropertyDescriptors(descriptors);
        descriptors.add(new PropertyDescriptor(PROPERTY_ACTIONSUBPROCESS, Localization.getString("ActionSubProcess.Name")));
        if (isEmbedded()) {
            descriptors.add(new ComboBoxPropertyDescriptor(PROPERTY_TRANSACTIONAL, Localization.getString("ActionSubProcess.Transactional"),
                    YesNoComboBoxTransformer.LABELS));
        }
        descriptors.add(new ComboBoxPropertyDescriptor(PROPERTY_VALIDATE_AT_START, Localization.getString("ActionSubProcess.ValidateAtStart"),
                YesNoComboBoxTransformer.LABELS));
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (PROPERTY_ACTIONSUBPROCESS.equals(id)) {
            return actionSubProcessName;
        }
        if (PROPERTY_TRANSACTIONAL.equals(id)) {
            if (transactional) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(1);
            }
        }
        else if (PROPERTY_VALIDATE_AT_START.equals(id)) {
            if (validateAtStart) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(1);
            }
        }
        return super.getPropertyValue(id);
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        boolean old = this.embedded;
        this.embedded = embedded;
        firePropertyChange(PROPERTY_ACTIONSUBPROCESS, old, this.embedded);
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    @Override
    public void setAsync(boolean async) {
        if (this.async != async) {
            this.async = async;
            firePropertyChange(PROPERTY_ASYNC, !async, async);
        }
    }

    @Override
    public AsyncCompletionMode getAsyncCompletionMode() {
        return asyncCompletionMode;
    }

    @Override
    public void setAsyncCompletionMode(AsyncCompletionMode asyncCompletionMode) {
        AsyncCompletionMode old = this.asyncCompletionMode;
        this.asyncCompletionMode = asyncCompletionMode;
        firePropertyChange(PROPERTY_ASYNC_COMPLETION_MODE, old, asyncCompletionMode);
    }

    @Override
    protected void fillCopyCustomFields(GraphElement copy) {
        super.fillCopyCustomFields(copy);
        // we are not copy embedded actionProcess
        ((ActionSubProcess) copy).setActionSubProcessName(embedded ? "" : getActionSubProcessName());
        for (VariableMapping mapping : getVariableMappings()) {
            ((ActionSubProcess) copy).getVariableMappings().add(mapping.getCopy());
        }
        ((ActionSubProcess) copy).setEmbedded(isEmbedded());
    }

    @Override
    public List<Variable> getUsedVariables(IFolder processFolder) {
        List<Variable> result = super.getUsedVariables(processFolder);
        for (VariableMapping mapping : getVariableMappings()) {
            if (mapping.isText()) {
                continue;
            }
            Variable variable = VariableUtils.getVariableByName(getProcessDefinition(), mapping.getName());
            if (variable != null) {
                result.add(variable);
            }
        }
        return result;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        boolean old = this.transactional;
        this.transactional = transactional;
        firePropertyChange(PROPERTY_TRANSACTIONAL, old, this.transactional);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        if (PROPERTY_ACTIONSUBPROCESS.equals(id)) {
            setActionSubProcessName((String) value);
        } else if (PROPERTY_TRANSACTIONAL.equals(id)) {
            setTransactional(YesNoComboBoxTransformer.setPropertyValue(value));
        } else if (PROPERTY_VALIDATE_AT_START.equals(id)) {
            setValidateAtStart(YesNoComboBoxTransformer.setPropertyValue(value));
        } else {
            super.setPropertyValue(id, value);
        }
    }

    public String getQualifiedId() {
        return getProcessDefinition().getFile().getParent().getFullPath() + "." + getId();
    }

    public boolean isValidateAtStart() {
        return validateAtStart;
    }

    public void setValidateAtStart(boolean validateAtStart) {
        boolean old = this.validateAtStart;
        this.validateAtStart = validateAtStart;
        firePropertyChange(PROPERTY_VALIDATE_AT_START, old, this.validateAtStart);
    }

}
