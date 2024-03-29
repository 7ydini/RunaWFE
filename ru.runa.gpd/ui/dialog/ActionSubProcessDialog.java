package ru.runa.gpd.ui.dialog;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import ru.runa.gpd.ActionSubProcessMap;
import ru.runa.gpd.Localization;
import ru.runa.gpd.ProcessCache;
//import ru.runa.gpd.SubprocessMap;
//import ru.runa.gpd.lang.model.MultiSubprocess;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.ActionSubProcess;
import ru.runa.gpd.lang.model.ActionSubProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.par.ParContentProvider;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.DragAndDropAdapter;
import ru.runa.gpd.ui.custom.LoggingModifyTextAdapter;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.LoggingSelectionChangedAdapter;
import ru.runa.gpd.ui.custom.SwtUtils;
import ru.runa.gpd.ui.custom.TableViewerLocalDragAndDropSupport;
import ru.runa.gpd.util.MultiinstanceParameters;
import ru.runa.gpd.util.VariableMapping;
import ru.runa.gpd.util.VariableUtils;

public class ActionSubProcessDialog extends Dialog {
    private Combo actionsubprocessDefinitionCombo;
    private String actionsubprocessName;
    protected final ActionSubProcess actionsubprocess;
    protected final List<VariableMapping> variableMappings;
    private VariablesComposite variablesComposite;
    //private final boolean multiInstance;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button changeButton;
    private Button deleteButton;

    public ActionSubProcessDialog(ActionSubProcess actionsubprocess) {
        super(PlatformUI.getWorkbench().getDisplay().getActiveShell());
        this.actionsubprocess = actionsubprocess;
        this.variableMappings = MultiinstanceParameters.getCopyWithoutMultiinstanceLinks(actionsubprocess.getVariableMappings());
        this.actionsubprocessName = actionsubprocess.getActionSubProcessName();
        //this.multiinstance = actionsubProcess instanceOf MultiSubprocess;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell newShell) {
        newShell.setSize(800, 400);
        super.configureShell(newShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));
        Group subprocessNameGroup = new Group(composite, SWT.NONE);
        subprocessNameGroup.setLayout(new GridLayout());
        subprocessNameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        subprocessNameGroup.setText(Localization.getString("Subprocess.Name"));

        actionsubprocessDefinitionCombo = new Combo(subprocessNameGroup, SWT.BORDER | SWT.READ_ONLY);
        actionsubprocessDefinitionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        actionsubprocessDefinitionCombo.setItems(getProcessDefinitionNames());
        actionsubprocessDefinitionCombo.setVisibleItemCount(10);
        if (actionsubprocessName != null) {
            String value = ActionSubProcessMap.get(actionsubprocess.getQualifiedId());
            if (value != null) {
                IPath subprocessPath = new Path(value);
                if (ResourcesPlugin.getWorkspace().getRoot().exists(subprocessPath)) {
                    String itemText = subprocessPath.lastSegment() + labelDelimiter + subprocessPath.removeLastSegments(1);
                    actionsubprocessDefinitionCombo.setText(itemText);
                }
            } else {
                IFile subprocessFile = ProcessCache.getFirstProcessDefinitionFile(actionsubprocessName,
                		actionsubprocess.getProcessDefinition().getFile().getProject().getName());
                if (subprocessFile != null) {
                    IPath subprocessPath = subprocessFile.getFullPath().removeLastSegments(1);
                    String itemText = subprocessPath.lastSegment() + labelDelimiter + subprocessPath.removeLastSegments(1);
                    actionsubprocessDefinitionCombo.setText(itemText);
                }
            }
        }
        actionsubprocessDefinitionCombo.addSelectionListener(new LoggingSelectionAdapter() {

            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                onActionSubProcessChanged();
            }
        });
        actionsubprocessDefinitionCombo.addModifyListener(new LoggingModifyTextAdapter() {

            @Override
            protected void onTextChanged(ModifyEvent e) throws Exception {
                onActionSubProcessChanged();
            }
        });

        
        SashForm sf = new SashForm(composite, SWT.VERTICAL | SWT.SMOOTH);
        sf.setLayout(new GridLayout());
        sf.setLayoutData(new GridData(GridData.FILL_BOTH));

        createConfigurationComposite(sf);

        Group mappingsGroup = new Group(sf, SWT.NONE);
        mappingsGroup.setLayout(new GridLayout());
        mappingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        mappingsGroup.setText(Localization.getString("Subprocess.VariableMappings"));
        
        if (sf.getChildren().length > 1) {
            sf.setWeights(new int[] {60, 40});
        }

        variablesComposite = new VariablesComposite(mappingsGroup);
        variablesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        variablesComposite.setFocus();
        return composite;
    }

    protected void createConfigurationComposite(Composite composite) {

    }

    protected void onActionSubProcessChanged() {
        String text = actionsubprocessDefinitionCombo.getText();
        int index = text.indexOf(labelDelimiter);
        if (index > 0) {
        	actionsubprocessName = text.substring(0, index);
            String subprocessFolderName = text.substring(index + labelDelimiter.length()) + "/" + actionsubprocessName;
            ActionSubProcessMap.set(actionsubprocess.getQualifiedId(), subprocessFolderName);
            validateVariableMappings(subprocessFolderName);
        }
    }

    private void validateVariableMappings(String actionsubprocessFolderName) {
        ProcessDefinition actionsubprocess = ProcessCache.getProcessDefinition((IFile) ResourcesPlugin.getWorkspace().getRoot()
                .findMember(actionsubprocessFolderName + "/" + ParContentProvider.PROCESS_DEFINITION_FILE_NAME));
        if (actionsubprocess != null) {
            List<String> variableNames = actionsubprocess.getVariableNames(true, true);
            for (Iterator<VariableMapping> i = variableMappings.iterator(); i.hasNext();) {
                if (!variableNames.contains(i.next().getMappedName())) {
                    i.remove();
                }
            }
            variablesComposite.refresh();
        }
    }

    private class VariablesComposite extends Composite {
        private final TableViewer tableViewer;

        public VariablesComposite(Composite parent) {
            super(parent, SWT.BORDER);
            setLayout(new GridLayout(2, false));

            tableViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
            tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            Table table = tableViewer.getTable();
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
            String[] columnNames = new String[] { "VariableMapping.Usage.Read", "VariableMapping.Usage.Write", "VariableMapping.Usage.Sync",
                    "Subprocess.ProcessVariableName", "Subprocess.SubprocessVariableName" };
            int[] columnWidths = new int[] { 50, 50, 50, 250, 250 };
            int[] columnAlignments = new int[] { SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.LEFT, SWT.LEFT };
//            if (multiinstance) {
//                TableColumn tableColumn = new TableColumn(table, SWT.CENTER);
//                tableColumn.setText(Localization.getString("VariableMapping.Usage.MultiinstanceLink"));
//                tableColumn.setWidth(50);
//                tableColumn.setToolTipText(Localization.getString("VariableMapping.Usage.MultiinstanceLink.description"));
//            }
            for (int i = 0; i < columnNames.length; i++) {
                TableColumn tableColumn = new TableColumn(table, columnAlignments[i]);
                tableColumn.setText(Localization.getString(columnNames[i]));
                tableColumn.setToolTipText(Localization.getString(columnNames[i] + ".description"));
                tableColumn.setWidth(columnWidths[i]);
            }
            tableViewer.setLabelProvider(new VariableMappingTableLabelProvider());
            tableViewer.setContentProvider(new ArrayContentProvider());
            setTableInput();

            Composite buttonsComposite = new Composite(this, SWT.NONE);
            buttonsComposite.setLayout(new GridLayout());
            GridData gridData = new GridData();
            gridData.horizontalAlignment = SWT.LEFT;
            gridData.verticalAlignment = SWT.TOP;
            buttonsComposite.setLayoutData(gridData);
            SwtUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.add"), new LoggingSelectionAdapter() {

                @Override
                protected void onSelection(SelectionEvent e) throws Exception {
                    editVariableMapping(null);
                }
            });
            changeButton = SwtUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.change"),
                    new LoggingSelectionAdapter() {

                        @Override
                        protected void onSelection(SelectionEvent e) throws Exception {
                            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                            if (!selection.isEmpty()) {
                                VariableMapping oldMapping = (VariableMapping) selection.getFirstElement();
                                editVariableMapping(oldMapping);
                            }
                        }
                    });
            moveUpButton = SwtUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.up"),
                    new MoveVariableSelectionListener(true));
            moveDownButton = SwtUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.down"),
                    new MoveVariableSelectionListener(false));
            deleteButton = SwtUtils.createButtonFillHorizontal(buttonsComposite, Localization.getString("button.delete"),
                    new LoggingSelectionAdapter() {

                        @Override
                        protected void onSelection(SelectionEvent e) throws Exception {
                            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                            if (!selection.isEmpty() && Dialogs.confirm(Localization.getString("confirm.delete"))) {
                                for (VariableMapping mapping : (List<VariableMapping>) selection.toList()) {
                                    variableMappings.remove(mapping);
                                }
                                tableViewer.refresh();
                                setTableInput();
                            }
                        }
                    });
            tableViewer.addSelectionChangedListener(new LoggingSelectionChangedAdapter() {
                @Override
                protected void onSelectionChanged(SelectionChangedEvent event) throws Exception {
                    updateButtons();
                }
            });
            updateButtons();
            TableViewerLocalDragAndDropSupport.enable(tableViewer, new DragAndDropAdapter<VariableMapping>() {

                @Override
                public void onDropElement(VariableMapping beforeElement, VariableMapping element) {
                    if (variableMappings.remove(element)) {
                        variableMappings.add(variableMappings.indexOf(beforeElement), element);
                    }
                }

                @Override
                public void onDrop(VariableMapping beforeElement, List<VariableMapping> elements) {
                    super.onDrop(beforeElement, elements);
                    setTableInput();
                }

            });
        }

        private void updateButtons() {
            List<?> selected = ((IStructuredSelection) tableViewer.getSelection()).toList();
            changeButton.setEnabled(selected.size() == 1);
            moveUpButton.setEnabled(selected.size() == 1 && variableMappings.indexOf(selected.get(0)) > 0);
            moveDownButton.setEnabled(selected.size() == 1 && variableMappings.indexOf(selected.get(0)) < variableMappings.size() - 1);
            deleteButton.setEnabled(selected.size() > 0);
        }

        private void editVariableMapping(VariableMapping mapping) {
            List<String> processVariableNames = actionsubprocess.getProcessDefinition().getVariableNames(true);
            processVariableNames.addAll(ActionSubProcess.PLACEHOLDERS);
            ActionSubProcessVariableDialog dialog = new ActionSubProcessVariableDialog(processVariableNames, getProcessVariablesNames(getActionSubProcessName()),
            mapping);
            if (dialog.open() != IDialogConstants.CANCEL_ID) {
                if (mapping == null) {
                    mapping = new VariableMapping();
                    variableMappings.add(mapping);
                    setTableInput();
                }
                mapping.setName(dialog.getProcessVariable());
                mapping.setMappedName(dialog.getActionSubProcessVariable());
                String usage = dialog.getAccess();
                if (isListVariable(actionsubprocess.getProcessDefinition().getName(), mapping.getName())
                        && !isListVariable(getActionSubProcessName(), mapping.getMappedName())) {
                    usage += "," + VariableMapping.USAGE_MULTIINSTANCE_LINK;
                }
                mapping.setUsage(usage);
                tableViewer.refresh();
            }
        }

        private void setTableInput() {
            tableViewer.setInput(getVariableMappings(false));
        }

        private class MoveVariableSelectionListener extends LoggingSelectionAdapter {
            private final boolean up;

            public MoveVariableSelectionListener(boolean up) {
                this.up = up;
            }

            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                VariableMapping mapping = (VariableMapping) selection.getFirstElement();
                int index = variableMappings.indexOf(mapping);
                Collections.swap(variableMappings, index, up ? (index - 1) : (index + 1));
                setTableInput();
                tableViewer.setSelection(selection);
            }
        }

        protected void refresh() {
            tableViewer.refresh();
        }

    }

    public List<VariableMapping> getVariableMappings(boolean includeMetadata) {
        return variableMappings;
    }

    private class VariableMappingTableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            VariableMapping mapping = (VariableMapping) element;
           // if (!multiinstance) {
           //     index++;
           // }
            switch (index) {
            case 0:
                return mapping.isMultiinstanceLink() ? "+" : "";
            case 1:
                return mapping.isReadable() ? "+" : "";
            case 2:
                return mapping.isWritable() ? "+" : "";
            case 3:
                return mapping.isSyncable() ? "+" : "";
            case 4:
                return mapping.getName();
            case 5:
                return mapping.getMappedName();
            default:
                return "unknown " + index;
            }
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

    private static final String labelDelimiter = "  \u2810  ";

    private String[] getProcessDefinitionNames() {
        List<String> names = Lists.newArrayList();
        for (ProcessDefinition testProcessDefinition : ProcessCache.getAllProcessDefinitionsMap().values()) {
            if (testProcessDefinition instanceof ActionSubProcessDefinition) {
                continue;
            }
            if (!names.contains(testProcessDefinition.getName())) {
                names.add(testProcessDefinition.getName() + labelDelimiter + testProcessDefinition.getFile().getParent().getParent().getFullPath());
            }
        }
        Collections.sort(names);
        return names.toArray(new String[names.size()]);
    }

    private List<String> getProcessVariablesNames(String name) {
        ProcessDefinition definition = ProcessCache.getFirstProcessDefinition(name,
        		actionsubprocess.getProcessDefinition().getFile().getProject().getName());
        if (definition != null) {
            return definition.getVariableNames(true);
        }
        return new ArrayList<String>();
    }

    private boolean isListVariable(String name, String variableName) {
        ProcessDefinition definition = ProcessCache.getFirstProcessDefinition(name,
        		actionsubprocess.getProcessDefinition().getFile().getProject().getName());
        if (definition != null) {
            Variable variable = VariableUtils.getVariableByName(definition, variableName);
            if (variable != null) {
                return List.class.getName().equals(variable.getJavaClassName());
            }
        }
        return false;
    }

    public String getActionSubProcessName() {
        return actionsubprocessName;
    }
}
