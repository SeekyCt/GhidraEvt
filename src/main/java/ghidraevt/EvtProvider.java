package ghidraevt;

import java.awt.event.MouseEvent;
import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.*;

import docking.ActionContext;
import docking.WindowPosition;
import docking.action.DockingAction;
import docking.action.MenuData;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.GhidraOptions;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.events.ProgramSelectionPluginEvent;
import ghidra.app.nav.LocationMemento;
import ghidra.app.nav.Navigatable;
import ghidra.app.plugin.core.decompile.DecompilePlugin;
import ghidra.app.services.ClipboardService;
import ghidra.app.services.GoToService;
import ghidra.app.services.ProgramManager;
import ghidra.app.services.QueryData;
import ghidra.app.util.ListingHighlightProvider;
import ghidra.framework.options.OptionsChangeListener;
import ghidra.framework.options.SaveState;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.NavigatableComponentProviderAdapter;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.Swing;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import ghidra.util.task.SwingUpdateManager;

import resources.Icons;
import utility.function.Callback;

// DecompilerHighlightService?

public class EvtProvider extends NavigatableComponentProviderAdapter
        implements OptionsChangeListener, EvtCallbackHandler {
    private final GhidraEvtPlugin plugin;
    private ClipboardService clipboardService;
    private EvtClipboardProvider clipboardProvider;
    private EvtOptions options;

    private Program program;
    private ProgramLocation currentLocation;
    private ProgramSelection currentSelection;

    private EvtController controller;
    private EvtPanel evtPanel;
    // private DecoratorPanel decorationPanel;
    // private ClangHighlightController highlightController;

    private ViewerPosition pendingViewerPosition;

    private SwingUpdateManager redisassembleUpdater;
    private EvtProgramListener programListener;
    private boolean lockDisplay;

    // Follow-up work can be items that need to happen after a pending disassemble is finished, such
    // as updating highlights after a variable rename
    private SwingUpdateManager followUpWorkUpdater;
    private Queue<Callback> followUpWork = new ConcurrentLinkedQueue<>();
    // private OverlayMessagePainter overlayPainter = new OverlayMessagePainter();
    private DockingAction refreshAction;

    // only used by disconnected providers
    private boolean allowOutgoingEvents = false;


    // TODO: save settings
    private DockingToggle strictMode; // TODO: type-based mode
    private DockingToggle showAddresses;
    private DockingToggle showLineNumbers;
    private DockingToggle snapToSymbol;
    private DockingToggle game;

    // private EvtHighlightFactory hlFactory = new EvtHighlightFactory();

    public EvtProvider(GhidraEvtPlugin plugin, boolean isConnected) {
        super(plugin.getTool(), "Evt Disassembler", plugin.getName(), EvtActionContext.class);

        this.plugin = plugin;
        this.clipboardProvider = new EvtClipboardProvider(plugin, this);
        setConnected(isConnected);

        DecompileOptions decompileOptions = new DecompileOptions();
        options = new EvtOptions(decompileOptions);
        initializeOptions();


        controller =
            new EvtController(getTool(), this, options, clipboardProvider);
        evtPanel = controller.getEvtPanel();

        // FUTURE move the hl controller into the panel
        // highlightController = new LocationClangHighlightController();
        // evtPanel.setHighlightController(highlightController);
        // decorationPanel = new DecoratorPanel(evtPanel, isConnected) {
        //     @Override
        //     public void paint(Graphics g) {
        //         super.paint(g);
        //         overlayPainter.paintOverlay(g, evtPanel.getViewContentBounds());
        //     }
        // };

        if (!isConnected) {
            setTransient();
        }

        setIcon(Icons.INFO_ICON);

        setWindowMenuGroup("Evt Disassembler");
        setDefaultWindowPosition(WindowPosition.RIGHT);
        addToTool();
        createActions(isConnected);

        redisassembleUpdater = new SwingUpdateManager(500, 5000, () -> doRefresh(false));
        followUpWorkUpdater = new SwingUpdateManager(() -> doFollowUpWork());

        programListener = new EvtProgramListener(redisassembleUpdater);
        setDefaultFocusComponent(controller.getEvtPanel());
    }

//==================================================================================================
// Component Provider methods
//==================================================================================================

    @Override
    public boolean isSnapshot() {
        // we are a snapshot when we are 'disconnected'
        return !isConnected();
    }

    @Override
    public void closeComponent() {
        super.closeComponent();
        controller.clear();
        plugin.closeProvider(this);
    }

    @Override
    public String getWindowGroup() {
        if (isConnected()) {
            return "";
        }
        return "disconnected";
    }

    @Override
    public void componentShown() {
        if (program != null && currentLocation != null) {
            ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
            ToolOptions opt = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
            options.grabFromToolAndProgram(fieldOptions, opt, program);
            controller.setOptions(options);

            refreshToggleButtons();

            controller.display(program, currentLocation, null);
        }
    }

    @Override
    public ActionContext getActionContext(MouseEvent event) {
        if (program == null) {
            return null;
        }
        EvtScript script = controller.getScript();
        if (script == null) {
            return null;
        }
        if (!controller.hasDisassembleResults()) {
            return null;
        }

		Address entryPoint = script.getStartAddress();
        int lineNumber =
            event != null ? getEvtPanel().getLineNumber(event.getY()) : 0;
        return new EvtActionContext(this, entryPoint, lineNumber);
    }

    @Override
    public JComponent getComponent() {
        return evtPanel;
    }


//==================================================================================================
// Navigatable interface methods
//==================================================================================================

    @Override
    public Program getProgram() {
        return program;
    }

    @Override
    public ProgramLocation getLocation() {
        if (currentLocation instanceof EvtLocation) {
            return currentLocation;
        }
        return controller.getEvtPanel().getCurrentLocation();
    }

    @Override
    public boolean goTo(Program gotoProgram, ProgramLocation location) {
        if (!isConnected()) {
            if (program == null) {
                // Special Case: this 'disconnected' provider is waiting to be initialized
                // with the first goTo() callback
                doSetProgram(gotoProgram);
            }
            else if (gotoProgram != program) {
                // this disconnected provider only works with its given program
                tool.setStatusInfo("Program location not applicable for this provider!");
                return false;
            }
        }

        ProgramManager programManagerService = tool.getService(ProgramManager.class);
        if (programManagerService != null) {
            programManagerService.setCurrentProgram(gotoProgram);
        }

        setLocation(location, null);
        pendingViewerPosition = null;
        plugin.locationChanged(this, location);
        return true;
    }

    @Override
    public LocationMemento getMemento() {
        ViewerPosition vp = controller.getEvtPanel().getViewerPosition();
        return new EvtLocationMemento(program, currentLocation, vp);
    }

    @Override
    public void setMemento(LocationMemento memento) {
        EvtLocationMemento evtMemento = (EvtLocationMemento) memento;
        pendingViewerPosition = evtMemento.getViewerPosition();
    }

//==================================================================================================
// DomainObjectListener methods
//==================================================================================================

    private void doRefresh(boolean optionsChanged) {
        if (!isVisible()) {
            return;
        }
        ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        ToolOptions opt = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);

        // Current values of toggle buttons
        // boolean decompilerEliminatesUnreachable = decompilerOptions.isEliminateUnreachable();
        // boolean decompilerRespectsReadOnlyFlags = decompilerOptions.isRespectReadOnly();

        options.grabFromToolAndProgram(fieldOptions, opt, program);

        // If the tool options were not changed
        if (!optionsChanged) {
            // Keep these analysis options the same
            // decompilerOptions.setEliminateUnreachable(decompilerEliminatesUnreachable);
            // decompilerOptions.setRespectReadOnly(decompilerRespectsReadOnlyFlags);
        }
        else {
            // Otherwise, keep the new analysis options and update the state of the toggle buttons
            refreshToggleButtons();
        }

        controller.setOptions(options);

        if (currentLocation != null) {
            controller.refreshDisplay(program, currentLocation, null);
        }
    }

    private void refreshToggleButtons() {
        // displayUnreachableCodeToggle.setSelected(!decompilerOptions.isEliminateUnreachable());
        // respectReadOnlyFlags.setSelected(!decompilerOptions.isRespectReadOnly());
    }

    private void doFollowUpWork() {
        if (isBusy()) {
            // try again later
            followUpWorkUpdater.updateLater();
            return;
        }

        Callback work = followUpWork.poll();
        while (work != null) {
            work.call();
            work = followUpWork.poll();
        }
    }

//==================================================================================================
// OptionsListener methods
//==================================================================================================

    @Override
    public void optionsChanged(ToolOptions options, String optionName, Object oldValue,
            Object newValue) {
        if (!isVisible()) {
            return;
        }

        if (options.getName().equals(DecompilePlugin.OPTIONS_TITLE) ||
            options.getName().equals(GhidraOptions.CATEGORY_BROWSER_FIELDS)) {
            doRefresh(true);
        }
    }

//==================================================================================================
// methods called from the plugin
//==================================================================================================

    void setClipboardService(ClipboardService service) {
        clipboardService = service;
        if (clipboardService != null) {
            clipboardService.registerClipboardContentProvider(clipboardProvider);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        redisassembleUpdater.dispose();
        followUpWorkUpdater.dispose();

        if (clipboardService != null) {
            clipboardService.deRegisterClipboardContentProvider(clipboardProvider);
        }

        controller.dispose();
        program = null;
        currentLocation = null;
        currentSelection = null;
    }

    /**
     * Sets the current program and adds/removes itself as a domainObjectListener
     *
     * @param newProgram the new program or null to clear out the current program.
     */
    void doSetProgram(Program newProgram) {
        controller.clear();
        if (program != null) {
            program.removeListener(programListener);
        }

        program = newProgram;
        currentLocation = null;
        currentSelection = null;
        if (program != null) {
            program.addListener(programListener);
            ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
            ToolOptions opt = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
            options.grabFromToolAndProgram(fieldOptions, opt, program);
        }

        clipboardProvider.setProgram(program);
    }

    @Override
    public void setSelection(ProgramSelection selection) {
        currentSelection = selection;
        if (isVisible()) {
            contextChanged();
            controller.setSelection(selection);
        }

        clipboardProvider.setSelection(selection);
        notifySelectionChanged(selection);
    }

    private void notifySelectionChanged(ProgramSelection selection) {
        if (!isConnected()) {
            return;
        }

        if (selection == null) {
            return;
        }

        plugin.firePluginEvent(
            new ProgramSelectionPluginEvent(plugin.getName(), selection, getProgram()));
    }

    @Override
    public void setHighlight(ProgramSelection highlight) {
        // do nothing for now
    }

    @Override
    public boolean supportsHighlight() {
        return false;
    }

    /**
     * sets the current location for this provider. If the provider is not visible, it does not pass
     * it on to the controller. When the component is later shown, the current location will then be
     * passed to the controller.
     *
     * @param loc the location to compile and set the cursor.
     * @param viewerPosition if non-null the position at which to scroll the view.
     */
    void setLocation(ProgramLocation loc, ViewerPosition viewerPosition) {
        Address currentAddress = currentLocation != null ? currentLocation.getAddress() : null;
        currentLocation = loc;
        clipboardProvider.setLocation(currentLocation);
        Address newAddress = currentLocation != null ? currentLocation.getAddress() : null;
        if (viewerPosition == null) {
            viewerPosition = pendingViewerPosition;
        }
        if (isVisible() && newAddress != null && !newAddress.equals(currentAddress)) {
            controller.display(program, loc, viewerPosition);
        }
        contextChanged();
        pendingViewerPosition = null;

    }

    /**
     * Re-disassemble the currently displayed location
     */
    void refresh() {
        controller.refreshDisplay(program, currentLocation, null);
    }

    /**
     * Update the options from EvtOptions
     */
    void updateOptionsAndRefresh() {
        controller.setOptions(options);

        refresh();
    }

    @Override
    public ProgramSelection getSelection() {
        return currentSelection;
    }

    @Override
    public ProgramSelection getHighlight() {
        return null;
    }

    // @Override
    // public String getTextSelection() {
    //     FieldSelection selection = fieldPanel.getSelection();
    //     if (selection.isEmpty()) {
    //         return null;
    //     }

    //     return FieldSelectionHelper.getFieldSelectionText(selection, fieldPanel);
    // }

    @Override
    public String getTextSelection() {
        EvtPanel evtPanel = controller.getEvtPanel();
        return evtPanel.getSelectedText();
    }

    boolean isBusy() {
        return redisassembleUpdater.isBusy();
    }

    /**
     * Set the cursor location of the disassemblr.
     *
     * @param lineNumber the 1-based line number
     * @param offset the character offset into line; the offset is from the start of the line
     */
    void setCursorLocation(int lineNumber, int offset) {

        EvtPanel evtPanel = controller.getEvtPanel();
        int row = lineNumber - 1; // 1-based number
        BigInteger index = BigInteger.valueOf(row);
        FieldLocation location = new FieldLocation(index, 0, 0, offset);
        evtPanel.setCursorPosition(location);
    }

    public EvtController getController() {
        return controller;
    }

//==================================================================================================
// methods called from the controller
//==================================================================================================

    @Override
    public void disassembleDataChanged(DisassembleData disassembleData) {
        updateTitle();
        contextChanged();
        controller.setSelection(currentSelection);
    }

    @Override
    public void locationChanged(ProgramLocation programLocation) {
        if (programLocation.equals(currentLocation)) {
            return;
        }
        currentLocation = programLocation;
        contextChanged();
        plugin.locationChanged(this, programLocation);
    }

    @Override
    public void selectionChanged(ProgramSelection programSelection) {
        currentSelection = programSelection;
        contextChanged();
        plugin.selectionChanged(this, programSelection);
    }

    @Override
    public void annotationClicked(AnnotatedTextFieldElement annotation, boolean newWindow) {

        Navigatable navigatable = this;
        if (newWindow) {
            EvtProvider newProvider = plugin.createNewDisconnectedProvider();
            navigatable = newProvider;
        }

        annotation.handleMouseClicked(navigatable, tool);
    }

    @Override
    public void goToAddress(Address address, boolean newWindow) {

        GoToService service = tool.getService(GoToService.class);
        if (service == null) {
            return;
        }

        Navigatable navigatable = this;
        if (newWindow) {
            EvtProvider newProvider = plugin.createNewDisconnectedProvider();
            navigatable = newProvider;
        }

        service.goTo(navigatable, new ProgramLocation(program, address), program);
    }

    @Override
    public void doWhenNotBusy(Callback c) {
        followUpWork.offer(c);
        followUpWorkUpdater.update();
    }

    // @Override
    public EvtPanel getEvtPanel() {
        return controller.getEvtPanel();
    }

//==================================================================================================
// methods called from other members
//==================================================================================================

    // snapshot callback
    public void cloneWindow() {
        EvtProvider newProvider = plugin.createNewDisconnectedProvider();

        // invoke later to give the window manage a chance to create the new window
        // (its done in an invoke later)
        Swing.runLater(() -> {

            ViewerPosition myViewPosition = controller.getEvtPanel().getViewerPosition();
            newProvider.doSetProgram(program);

            // initialize the new provider's cache and then set the location
            newProvider.setLocation(currentLocation, myViewPosition);

            // transfer any state after the new disassembler is initialized
            EvtPanel myPanel = getEvtPanel();
            EvtPanel newPanel = newProvider.getEvtPanel();
            newProvider.doWhenNotBusy(() -> {
                newPanel.setViewerPosition(myViewPosition);
                // newPanel.cloneHighlights(myPanel);
            });
        });
    }

    @Override
    public void contextChanged() {
        tool.contextChanged(this);
    }

//==================================================================================================
// private methods
//==================================================================================================
    /**
     * Updates the windows title and subtitle to reflect the currently disassembled script
     */
    private void updateTitle() {
        EvtScript script = controller.getDisassembleData().getScript();
        String programName = (program != null) ? program.getDomainFile().getName() : "";
        String title = "Evt Disassembler";
        String functionName = "No script";
        String tabText = "Evt Disassembler";
        String subTitle = "";
        if (script != null && program != null) {
            Symbol symbol = program.getSymbolTable().getPrimarySymbol(script.getStartAddress());
            if (symbol != null) {
                functionName = symbol.getName();
            }
            else {
                functionName = script.getStartAddress().toString();
            }
            title = "Disassemble: " + functionName;
            subTitle = " (" + programName + ")";
        }
        
        if (!isConnected()) {
            title = "[" + title + "]";
            tabText = "[" + functionName + "]";
        }
        setTitle(title);
        setSubTitle(subTitle);
        setTabText(tabText);
    }

    private void initializeOptions() {
        ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        ToolOptions opt = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
        options.registerOptions(fieldOptions, opt, program);

        opt.addOptionsChangeListener(this);

        ToolOptions codeBrowserOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        codeBrowserOptions.addOptionsChangeListener(this);
    }

    private void createActions(boolean isConnected) {
        String owner = plugin.getName();

        // SelectAllAction selectAllAction =
        //     new SelectAllAction(owner, controller.getEvtPanel());

        // DockingAction refreshAction = new DockingAction("Refresh", owner) {
        //     @Override
        //     public void actionPerformed(ActionContext context) {
        //         refresh();
        //     }

        //     @Override
        //     public boolean isEnabledForContext(ActionContext context) {
        //         EvtData decompileData = controller.getEvtData();
        //         if (decompileData == null) {
        //             return false;
        //         }
        //         return decompileData.hasDecompileResults();
        //     }
        // };
        // refreshAction.setToolBarData(new ToolBarData(REFRESH_ICON, "A" /* first on toolbar */));
        // refreshAction.setDescription("Push at any time to trigger a re-decompile");
        // refreshAction
        //         .setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ToolBarRedecompile")); // just use the default

        // displayUnreachableCodeToggle = new ToggleDockingAction("Toggle Unreachable Code", owner) {
        //     @Override
        //     public void actionPerformed(ActionContext context) {
        //         boolean isSelected = this.isSelected();

        //         // Set the option based on the button state
        //         decompilerOptions.setEliminateUnreachable(!isSelected);

        //         updateOptionsAndRefresh();
        //     }

        //     @Override
        //     public void setSelected(boolean isSelected) {
        //         super.setSelected(isSelected);

        //         // Update the icon to have a slash or not
        //         if (!isSelected) {
        //             displayUnreachableCodeToggle
        //                     .setToolBarData(new ToolBarData(TOGGLE_UNREACHABLE_CODE_ICON, "A"));
        //         }
        //         else {
        //             displayUnreachableCodeToggle.setToolBarData(
        //                 new ToolBarData(TOGGLE_UNREACHABLE_CODE_DISABLED_ICON, "A"));
        //         }
        //     }

        //     @Override
        //     public boolean isEnabledForContext(ActionContext context) {
        //         DecompileData decompileData = controller.getEvtData();
        //         if (decompileData == null) {
        //             return false;
        //         }
        //         return decompileData.hasDecompileResults();
        //     }
        // };
        // displayUnreachableCodeToggle.setDescription("Toggle off to eliminate unreachable code");
        // displayUnreachableCodeToggle.setHelpLocation(
        //     new HelpLocation(HelpTopics.DECOMPILER, "ToolBarEliminateUnreachableCode"));

        // respectReadOnlyFlags = new ToggleDockingAction("Toggle Respecting Read-only Flags", owner) {
        //     @Override
        //     public void actionPerformed(ActionContext context) {
        //         boolean isSelected = this.isSelected();

        //         // Set the option based on the button state
        //         decompilerOptions.setRespectReadOnly(!isSelected);

        //         updateOptionsAndRefresh();
        //     }

        //     @Override
        //     public void setSelected(boolean isSelected) {
        //         super.setSelected(isSelected);

        //         // Update the icon to have a slash or not
        //         if (!isSelected) {
        //             respectReadOnlyFlags
        //                     .setToolBarData(new ToolBarData(TOGGLE_READ_ONLY_ICON, "A"));
        //         }
        //         else {
        //             respectReadOnlyFlags
        //                     .setToolBarData(new ToolBarData(TOGGLE_READ_ONLY_DISABLED_ICON, "A"));
        //         }
        //     }

        //     @Override
        //     public boolean isEnabledForContext(ActionContext context) {
        //         DecompileData decompileData = controller.getEvtData();
        //         if (decompileData == null) {
        //             return false;
        //         }
        //         return decompileData.hasDecompileResults();
        //     }
        // };
        // respectReadOnlyFlags.setDescription("Toggle off to respect readonly flags set on memory");
        // respectReadOnlyFlags
        //         .setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ToolBarRespectReadOnly"));

        // Set the selected state and icon for the above two toggle icons
        refreshToggleButtons();

        //
        // Below are actions along with their groups and subgroup information.  The comments
        // for each section indicates the logical group for the actions that follow.
        // The actual group String used is for ordering the groups.  The int position is
        // used to specify a position *within* each group for each action.
        //
        // Group naming note:  We can control the ordering of our groups.  We cannot, however,
        // control the grouping of the dynamically inserted actions, such as the 'comment' actions.
        // In order to organize our groups around the comment actions, we have
        // to make our group names based upon the comment group name.
        // Below you will see group names that will trigger group sorting by number for those
        // groups before the comments group and then group sorting using the known comment group
        // name for those groups after the comments.
        //

        //
        // Function
        //
        // String functionGroup = "1 - Function Group";
        // int subGroupPosition = 0;

        // SpecifyCPrototypeAction specifyCProtoAction = new SpecifyCPrototypeAction();
        // setGroupInfo(specifyCProtoAction, functionGroup, subGroupPosition++);

        // OverridePrototypeAction overrideSigAction = new OverridePrototypeAction();
        // setGroupInfo(overrideSigAction, functionGroup, subGroupPosition++);

        // EditPrototypeOverrideAction editOverrideSigAction = new EditPrototypeOverrideAction();
        // setGroupInfo(editOverrideSigAction, functionGroup, subGroupPosition++);

        // DeletePrototypeOverrideAction deleteSigAction = new DeletePrototypeOverrideAction();
        // setGroupInfo(deleteSigAction, functionGroup, subGroupPosition++);

        // RenameFunctionAction renameFunctionAction = new RenameFunctionAction();
        // setGroupInfo(renameFunctionAction, functionGroup, subGroupPosition++);

        // // not function actions, but they fit nicely in this group
        // RenameLabelAction renameLabelAction = new RenameLabelAction();
        // setGroupInfo(renameLabelAction, functionGroup, subGroupPosition++);

        // RemoveLabelAction removeLabelAction = new RemoveLabelAction();
        // setGroupInfo(removeLabelAction, functionGroup, subGroupPosition++);

        //
        // Variables
        //
        // String variableGroup = "2 - Variable Group";
        // subGroupPosition = 0; // reset for the next group

        // RenameLocalAction renameLocalAction = new RenameLocalAction();
        // setGroupInfo(renameLocalAction, variableGroup, subGroupPosition++);

        // RenameGlobalAction renameGlobalAction = new RenameGlobalAction();
        // setGroupInfo(renameGlobalAction, variableGroup, subGroupPosition++);

        // RenameFieldAction renameFieldAction = new RenameFieldAction();
        // setGroupInfo(renameFieldAction, variableGroup, subGroupPosition++);

        // RenameBitFieldAction renameBitFieldAction = new RenameBitFieldAction();
        // setGroupInfo(renameBitFieldAction, variableGroup, subGroupPosition++);

        // ForceUnionAction forceUnionAction = new ForceUnionAction();
        // setGroupInfo(forceUnionAction, variableGroup, subGroupPosition++);

        // RetypeLocalAction retypeLocalAction = new RetypeLocalAction();
        // setGroupInfo(retypeLocalAction, variableGroup, subGroupPosition++);

        // CreatePointerRelative createRelativeAction = new CreatePointerRelative();
        // setGroupInfo(createRelativeAction, variableGroup, subGroupPosition++);

        // RetypeGlobalAction retypeGlobalAction = new RetypeGlobalAction();
        // setGroupInfo(retypeGlobalAction, variableGroup, subGroupPosition++);

        // RetypeReturnAction retypeReturnAction = new RetypeReturnAction();
        // setGroupInfo(retypeReturnAction, variableGroup, subGroupPosition++);

        // RetypeFieldAction retypeFieldAction = new RetypeFieldAction();
        // setGroupInfo(retypeFieldAction, variableGroup, subGroupPosition++);

        // IsolateVariableAction isolateVarAction = new IsolateVariableAction();
        // setGroupInfo(isolateVarAction, variableGroup, subGroupPosition++);

        // DecompilerStructureVariableAction decompilerCreateStructureAction =
        //     new DecompilerStructureVariableAction(owner, tool, controller);
        // setGroupInfo(decompilerCreateStructureAction, variableGroup, subGroupPosition++);

        // EditDataTypeAction editDataTypeAction = new EditDataTypeAction();
        // setGroupInfo(editDataTypeAction, variableGroup, subGroupPosition++);

        // // shows the quick editor dialog
        // EditFieldAction editFieldAction = new EditFieldAction();
        // setGroupInfo(editFieldAction, variableGroup, subGroupPosition++);

        //
        // Listing action for Creating Structure on a Variable
        //
        // ListingStructureVariableAction listingCreateStructureAction =
        //     new ListingStructureVariableAction(owner, tool, controller);

        //
        // Commit
        //
        // String commitGroup = "3 - Commit Group";
        // subGroupPosition = 0; // reset for the next group

        // CommitParamsAction lockProtoAction = new CommitParamsAction();
        // setGroupInfo(lockProtoAction, commitGroup, subGroupPosition++);

        // CommitLocalsAction lockLocalAction = new CommitLocalsAction();
        // setGroupInfo(lockLocalAction, commitGroup, subGroupPosition++);

        // subGroupPosition = 0; // reset for the next group

        //
        // Highlight
        //
        // String highlightGroup = "4a - Highlight Group";
        // tool.setMenuGroup(new String[] { "Highlight" }, highlightGroup);
        // HighlightDefinedUseAction defUseHighlightAction = new HighlightDefinedUseAction();
        // setGroupInfo(defUseHighlightAction, highlightGroup, subGroupPosition++);

        // ForwardSliceAction forwardSliceAction = new ForwardSliceAction();
        // setGroupInfo(forwardSliceAction, highlightGroup, subGroupPosition++);

        // BackwardsSliceAction backwardSliceAction = new BackwardsSliceAction();
        // setGroupInfo(backwardSliceAction, highlightGroup, subGroupPosition++);

        // ForwardSliceToPCodeOpsAction forwardSliceToOpsAction = new ForwardSliceToPCodeOpsAction();
        // setGroupInfo(forwardSliceToOpsAction, highlightGroup, subGroupPosition++);

        // BackwardsSliceToPCodeOpsAction backwardSliceToOpsAction =
        //     new BackwardsSliceToPCodeOpsAction();
        // setGroupInfo(backwardSliceToOpsAction, highlightGroup, subGroupPosition++);

        // tool.setMenuGroup(new String[] { "Secondary Highlight" }, highlightGroup);
        // SetSecondaryHighlightAction setSecondaryHighlightAction = new SetSecondaryHighlightAction();
        // setGroupInfo(setSecondaryHighlightAction, highlightGroup, subGroupPosition++);

        // SetSecondaryHighlightColorChooserAction setSecondaryHighlightColorChooserAction =
        //     new SetSecondaryHighlightColorChooserAction();
        // setGroupInfo(setSecondaryHighlightColorChooserAction, highlightGroup, subGroupPosition++);

        // RemoveSecondaryHighlightAction removeSecondaryHighlightAction =
        //     new RemoveSecondaryHighlightAction();
        // setGroupInfo(removeSecondaryHighlightAction, highlightGroup, subGroupPosition++);

        // RemoveAllSecondaryHighlightsAction removeAllSecondadryHighlightsAction =
        //     new RemoveAllSecondaryHighlightsAction();
        // setGroupInfo(removeAllSecondadryHighlightsAction, highlightGroup, subGroupPosition++);

        // PreviousHighlightedTokenAction previousHighlightedTokenAction =
        //     new PreviousHighlightedTokenAction();
        // setGroupInfo(previousHighlightedTokenAction, highlightGroup, subGroupPosition++);

        // NextHighlightedTokenAction nextHighlightedTokenAction = new NextHighlightedTokenAction();
        // setGroupInfo(nextHighlightedTokenAction, highlightGroup, subGroupPosition++);

        // String convertGroup = "7 - Convert Group";
        // subGroupPosition = 0;
        // RemoveEquateAction removeEquateAction = new RemoveEquateAction();
        // setGroupInfo(removeEquateAction, convertGroup, subGroupPosition++);

        // SetEquateAction setEquateAction = new SetEquateAction(plugin);
        // setGroupInfo(setEquateAction, convertGroup, subGroupPosition++);

        // ConvertBinaryAction convertBinaryAction = new ConvertBinaryAction(plugin);
        // setGroupInfo(convertBinaryAction, convertGroup, subGroupPosition++);

        // ConvertDecAction convertDecAction = new ConvertDecAction(plugin);
        // setGroupInfo(convertDecAction, convertGroup, subGroupPosition++);

        // ConvertFloatAction convertFloatAction = new ConvertFloatAction(plugin);
        // setGroupInfo(convertFloatAction, convertGroup, subGroupPosition++);

        // ConvertDoubleAction convertDoubleAction = new ConvertDoubleAction(plugin);
        // setGroupInfo(convertDoubleAction, convertGroup, subGroupPosition++);

        // ConvertHexAction convertHexAction = new ConvertHexAction(plugin);
        // setGroupInfo(convertHexAction, convertGroup, subGroupPosition++);

        // ConvertOctAction convertOctAction = new ConvertOctAction(plugin);
        // setGroupInfo(convertOctAction, convertGroup, subGroupPosition++);

        // ConvertCharAction convertCharAction = new ConvertCharAction(plugin);
        // setGroupInfo(convertCharAction, convertGroup, subGroupPosition++);

        //
        // Comments
        //
        // NOTE: this is just a placeholder to represent where the comment actions should appear
        //       in relation to our local actions.
        //

        //
        // Search
        //
        // String searchGroup = "Comment2 - Search Group";
        // subGroupPosition = 0; // reset for the next group

        // FindAction findAction = new FindAction();
        // setGroupInfo(findAction, searchGroup, subGroupPosition++);

        //
        // References
        //

        // note: set the menu group so that the 'References' group is with the 'Find' action
        // String referencesParentGroup = searchGroup;

        // FindReferencesToDataTypeAction findReferencesAction =
        //     new FindReferencesToDataTypeAction(owner, tool, controller);
        // setGroupInfo(findReferencesAction, searchGroup, subGroupPosition++);
        // findReferencesAction.getPopupMenuData().setParentMenuGroup(referencesParentGroup);

        // FindReferencesToHighSymbolAction findReferencesToSymbolAction =
        //     new FindReferencesToHighSymbolAction();
        // setGroupInfo(findReferencesToSymbolAction, searchGroup, subGroupPosition++);
        // findReferencesToSymbolAction.getPopupMenuData().setParentMenuGroup(referencesParentGroup);
        // addLocalAction(findReferencesToSymbolAction);

        // FindReferencesToAddressAction findReferencesToAddressAction =
        //     new FindReferencesToAddressAction(tool, owner);
        // setGroupInfo(findReferencesToAddressAction, searchGroup, subGroupPosition++);
        // findReferencesToAddressAction.getPopupMenuData().setParentMenuGroup(referencesParentGroup);
        // addLocalAction(findReferencesToAddressAction);

        //
        // Options
        //
        // String optionsGroup = "comment6 - Options Group";
        // subGroupPosition = 0; // reset for the next group

        // EditPropertiesAction propertiesAction = new EditPropertiesAction(owner, tool);
        // setGroupInfo(propertiesAction, optionsGroup, subGroupPosition++);

        //
        // These actions are not in the popup menu
        //
        // DebugDecompilerAction debugFunctionAction = new DebugDecompilerAction(controller);
        // ExportToCAction convertAction = new ExportToCAction();
        // CloneDecompilerAction cloneDecompilerAction = new CloneDecompilerAction();
        // GoToNextBraceAction goToNextBraceAction = new GoToNextBraceAction();
        // GoToPreviousBraceAction goToPreviousBraceAction = new GoToPreviousBraceAction();
        // DisplayTypeCastsAction displayTypeCastsAction = new DisplayTypeCastsAction(plugin);

        // addLocalAction(refreshAction);
        // addLocalAction(displayUnreachableCodeToggle);
        // addLocalAction(respectReadOnlyFlags);
        // addLocalAction(selectAllAction);
        // addLocalAction(defUseHighlightAction);
        // addLocalAction(forwardSliceAction);
        // addLocalAction(backwardSliceAction);
        // addLocalAction(forwardSliceToOpsAction);
        // addLocalAction(backwardSliceToOpsAction);
        // addLocalAction(lockProtoAction);
        // addLocalAction(lockLocalAction);
        // addLocalAction(renameLocalAction);
        // addLocalAction(renameGlobalAction);
        // addLocalAction(renameFieldAction);
        // addLocalAction(renameBitFieldAction);
        // addLocalAction(forceUnionAction);
        // addLocalAction(setSecondaryHighlightAction);
        // addLocalAction(setSecondaryHighlightColorChooserAction);
        // addLocalAction(removeSecondaryHighlightAction);
        // addLocalAction(removeAllSecondadryHighlightsAction);
        // addLocalAction(nextHighlightedTokenAction);
        // addLocalAction(previousHighlightedTokenAction);
        // addLocalAction(convertBinaryAction);
        // addLocalAction(convertDecAction);
        // addLocalAction(convertFloatAction);
        // addLocalAction(convertDoubleAction);
        // addLocalAction(convertHexAction);
        // addLocalAction(convertOctAction);
        // addLocalAction(convertCharAction);
        // addLocalAction(setEquateAction);
        // addLocalAction(removeEquateAction);
        // addLocalAction(retypeLocalAction);
        // addLocalAction(createRelativeAction);
        // addLocalAction(retypeGlobalAction);
        // addLocalAction(retypeReturnAction);
        // addLocalAction(retypeFieldAction);
        // addLocalAction(isolateVarAction);
        // addLocalAction(decompilerCreateStructureAction);
        // tool.addAction(listingCreateStructureAction);
        // addLocalAction(editDataTypeAction);
        // addLocalAction(editFieldAction);
        // addLocalAction(specifyCProtoAction);
        // addLocalAction(overrideSigAction);
        // addLocalAction(editOverrideSigAction);
        // addLocalAction(deleteSigAction);
        // addLocalAction(renameFunctionAction);
        // addLocalAction(renameLabelAction);
        // addLocalAction(removeLabelAction);
        // addLocalAction(debugFunctionAction);
        // addLocalAction(displayTypeCastsAction);
        // addLocalAction(convertAction);
        // addLocalAction(findAction);
        // addLocalAction(findReferencesAction);
        // addLocalAction(propertiesAction);
        // addLocalAction(cloneDecompilerAction);
        // addLocalAction(goToNextBraceAction);
        // addLocalAction(goToPreviousBraceAction);
    }

    private void toggleOutgoingEvents() {
        allowOutgoingEvents = !allowOutgoingEvents;
    }

    boolean shouldSendEvents() {
        if (isConnected()) {
            return true;
        }
        return allowOutgoingEvents;
    }

    private void toggleDisplayLock() {
        lockDisplay = !lockDisplay;
        if (!lockDisplay) {
            refresh();
        }
    }

    /**
     * Sets the group and subgroup information for the given action.
     */
    private void setGroupInfo(DockingAction action, String group, int subGroupPosition) {
        MenuData popupMenuData = action.getPopupMenuData();
        popupMenuData.setMenuGroup(group);

        // Some groups have numbers reach double-digits.  These will not compare correctly unless
        // padded.  Ensure all string numbers are at least 2 digits.
        String numberString = Integer.toString(subGroupPosition);
        if (numberString.length() == 1) {
            numberString = '0' + numberString;
        }
        popupMenuData.setMenuSubGroup(numberString);
    }

    @Override
    public void exportLocation() {
        if (program != null && currentLocation != null) {
            plugin.exportLocation(program, currentLocation);
        }
    }

    @Override
    public void writeDataState(SaveState saveState) {
        super.writeDataState(saveState);
        if (currentLocation != null) {
            currentLocation.saveState(saveState);
        }
        ViewerPosition vp = controller.getEvtPanel().getViewerPosition();
        saveState.putInt("INDEX", vp.getIndexAsInt());
        saveState.putInt("Y_OFFSET", vp.getYOffset());

    }

    @Override
    public void readDataState(SaveState saveState) {
        super.readDataState(saveState);
        int index = saveState.getInt("INDEX", 0);
        int yOffset = saveState.getInt("Y_OFFSET", 0);
        ViewerPosition vp = new ViewerPosition(index, 0, yOffset);
        if (program != null && isVisible()) {
            currentLocation = ProgramLocation.getLocation(program, saveState);
            if (currentLocation != null) {
                controller.display(program, currentLocation, vp);
            }
        }
    }

    @Override
    public void removeHighlightProvider(ListingHighlightProvider highlightProvider, Program p) {
        // currently unsupported
    }

    @Override
    public void setHighlightProvider(ListingHighlightProvider highlightProvider, Program p) {
        // currently unsupported
    }

    // private static class EvtHighlightFactory implements FieldHighlightFactory {
    //     @Override
    //     public Highlight[] createHighlights(Field field, String text, int cursorTextOffset) {
    //         return new Highlight[0];
    //     }
    // }

    // // Customize GUI
    // private void buildPanel() {
    //     panel = new JPanel(new BorderLayout());
    //     panel.setName("Evt Master Panel");

    //     layout = new EvtLayoutModel(panel, getTool(), hlFactory);
    //     fieldPanel = new FieldPanel(layout, "Evt Field Panel");

    //     IndexedScrollPane scrollPane = new IndexedScrollPane(fieldPanel);
    //     scrollPane.setName("Evt Scroll Pane");

    //     panel.add(scrollPane);
    //     setIcon(Icons.INFO_ICON);
    //     setDefaultWindowPosition(WindowPosition.RIGHT);
    //     setVisible(true);
    // }

    // public EvtToken getTokenAtCursor() {
    //     FieldLocation cursorPosition = fieldPanel.getCursorLocation();
    //     Field field = fieldPanel.getCurrentField();
    //     if (field == null) {
    //         return null;
    //     }
    //     return ((EvtTextField) field).getToken(cursorPosition);
    // }

    // private void createActions() {
    //     Runnable disasmCallback = new Runnable() {
    //         @Override
    //         public void run() {
    //             updateDisasm();
    //         }
    //     };

    //     strictMode = new DockingToggle(
    //         "Strict Mode",
    //         getOwner(),
    //         false,
    //         disasmCallback);
    //     strictMode.setEnabled(true);
    //     strictMode.markHelpUnnecessary();

    //     showAddresses = new DockingToggle(
    //         "Show Addresses",
    //         getOwner(),
    //         false,
    //         disasmCallback);
    //     showAddresses.setEnabled(true);
    //     showAddresses.markHelpUnnecessary();

    //     showLineNumbers = new DockingToggle(
    //         "Show Line Numbers",
    //         getOwner(),
    //         false,
    //         disasmCallback);
    //     showLineNumbers.setEnabled(true);
    //     showLineNumbers.markHelpUnnecessary();

    //     snapToSymbol = new DockingToggle(
    //         "Snap to Symbol",
    //         getOwner(),
    //         true,
    //         disasmCallback);
    //     snapToSymbol.setEnabled(true);
    //     snapToSymbol.markHelpUnnecessary();

    //     game = new DockingToggle(
    //         "Game",
    //         getOwner(),
    //         true,
    //         disasmCallback);
    //     game.setEnabled(true);
    //     game.markHelpUnnecessary();

    //     dockingTool.addLocalAction(this, showLineNumbers);
    //     dockingTool.addLocalAction(this, showAddresses);
    //     dockingTool.addLocalAction(this, strictMode);
    //     dockingTool.addLocalAction(this, snapToSymbol);
    //     dockingTool.addLocalAction(this, game);
    // }
}
