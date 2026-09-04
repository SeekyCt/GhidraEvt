/* ###
 * IP: GHIDRA
 *
 * Copyright 2026 SeekyCt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified from ghidra/app/plugin/core/decompile/DecompilerProvider.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.event.MouseEvent;
import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.*;

import docking.ActionContext;
import docking.WindowPosition;
import docking.action.DockingAction;
import docking.action.MenuData;
import docking.action.ToolBarData;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.GhidraOptions;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.events.ProgramSelectionPluginEvent;
import ghidra.app.nav.DecoratorPanel;
import ghidra.app.nav.LocationMemento;
import ghidra.app.nav.Navigatable;
import ghidra.app.plugin.core.decompile.DecompilePlugin;
import ghidra.app.services.ClipboardService;
import ghidra.app.services.GoToService;
import ghidra.app.services.ProgramManager;
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
import ghidraevt.GhidraEvtPlugin;
import ghidraevt.action.CloneEvtAction;
import ghidraevt.action.EditDataTypeAction;
import ghidraevt.action.EvtActionContext;
import ghidraevt.action.EvtHighlightDefinedUseAction;
import ghidraevt.action.FindAction;
import ghidraevt.action.RenameSymbolAction;
import ghidraevt.action.RetypeGlobalAction;
import ghidraevt.action.SelectAllAction;
import ghidraevt.highlight.EvtHighlightController;
import ghidraevt.highlight.LocationEvtHighlightController;
import ghidraevt.location.EvtLocation;
import ghidraevt.location.EvtLocationMemento;
import ghidraevt.token.EvtToken;
import resources.Icons;
import utility.function.Callback;

public class EvtProvider extends NavigatableComponentProviderAdapter
        implements OptionsChangeListener, EvtCallbackHandler {
	private static final Icon REFRESH_ICON = Icons.REFRESH_ICON;

    private final GhidraEvtPlugin plugin;
    private ClipboardService clipboardService;
    private EvtClipboardProvider clipboardProvider;
    private EvtOptions options;

    private Program program;
    private ProgramLocation currentLocation;
    private ProgramSelection currentSelection;

    private EvtController controller;
    private DecoratorPanel decorationPanel;
    private EvtHighlightController highlightController;

    private ViewerPosition pendingViewerPosition;

    private SwingUpdateManager redisassembleUpdater;
    private EvtProgramListener programListener;
    private boolean lockDisplay;

    // Follow-up work can be items that need to happen after a pending disassemble is finished, such
    // as updating highlights after a variable rename
    private SwingUpdateManager followUpWorkUpdater;
    private Queue<Callback> followUpWork = new ConcurrentLinkedQueue<>();
    // private OverlayMessagePainter overlayPainter = new OverlayMessagePainter();

    // only used by disconnected providers
    private boolean allowOutgoingEvents = false;


    // TODO: save settings
    private DockingToggle strictMode; // TODO: type-based mode
    private DockingToggle showAddresses;
    private DockingToggle showLineNumbers;
    private DockingToggle snapToSymbol;

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
        EvtPanel evtPanel = controller.getEvtPanel();

        // FUTURE move the hl controller into the panel
        highlightController = new LocationEvtHighlightController();
        evtPanel.setHighlightController(highlightController);
        decorationPanel = new DecoratorPanel(evtPanel, isConnected);

        if (!isConnected) {
            setTransient();
        }

        setIcon(Icons.INFO_ICON);
        setTitle("Evt Disassembler");

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
        return decorationPanel;
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

    public void setClipboardService(ClipboardService service) {
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
    public void doSetProgram(Program newProgram) {
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
    public void setLocation(ProgramLocation loc, ViewerPosition viewerPosition) {
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
            initializeClone(newProvider);
        });
    }

    private void initializeClone(EvtProvider newProvider) {
        ViewerPosition myViewPosition = controller.getEvtPanel().getViewerPosition();
        newProvider.doSetProgram(program);

        newProvider.setLocation(currentLocation, myViewPosition);

        // transfer any state after the new disassembler is initialized
        EvtPanel myPanel = getEvtPanel();
        EvtPanel newPanel = newProvider.getEvtPanel();
        newProvider.doWhenNotBusy(() -> {
            newPanel.setViewerPosition(myViewPosition);
            newPanel.cloneHighlights(myPanel);
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

        SelectAllAction selectAllAction =
            new SelectAllAction(owner, controller.getEvtPanel());

        DockingAction refreshAction = new DockingAction("Refresh", owner) {
            @Override
            public void actionPerformed(ActionContext context) {
                refresh();
            }

            @Override
            public boolean isEnabledForContext(ActionContext context) {
                DisassembleData decompileData = controller.getDisassembleData();
                if (decompileData == null) {
                    return false;
                }
                return decompileData.hasDisassembleResults();
            }
        };
        refreshAction.setToolBarData(new ToolBarData(REFRESH_ICON, "A" /* first on toolbar */));
        refreshAction.setDescription("Push at any time to trigger a re-disassemble");

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
        // Symbols
        //
        String symbolGroup = "1 - Symbol Group";
        int subGroupPosition = 0;

        RenameSymbolAction renameSymbolAction = new RenameSymbolAction();
        setGroupInfo(renameSymbolAction, symbolGroup, subGroupPosition++);

        RetypeGlobalAction retypeGlobalAction = new RetypeGlobalAction();
        setGroupInfo(retypeGlobalAction, symbolGroup, subGroupPosition++);

        EditDataTypeAction editDataTypeAction = new EditDataTypeAction();
        setGroupInfo(editDataTypeAction, symbolGroup, subGroupPosition++);

        //
        // Highlight
        //
        // String highlightGroup = "4a - Highlight Group";
        // tool.setMenuGroup(new String[] { "Highlight" }, highlightGroup);
        // EvtHighlightDefinedUseAction defUseHighlightAction = new EvtHighlightDefinedUseAction();
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

        //
        // Convert
        //
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
        String searchGroup = "Comment2 - Search Group";
        subGroupPosition = 0; // reset for the next group

        FindAction findAction = new FindAction();
        setGroupInfo(findAction, searchGroup, subGroupPosition++);

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
        // ExportToCAction convertAction = new ExportToCAction();
        CloneEvtAction cloneDecompilerAction = new CloneEvtAction();

        addLocalAction(selectAllAction);
        addLocalAction(refreshAction);
        addLocalAction(renameSymbolAction);
        addLocalAction(retypeGlobalAction);
        addLocalAction(editDataTypeAction);
        // addLocalAction(defUseHighlightAction);
        // addLocalAction(forwardSliceAction);
        // addLocalAction(backwardSliceAction);
        // addLocalAction(forwardSliceToOpsAction);
        // addLocalAction(backwardSliceToOpsAction);
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
        // addLocalAction(convertAction);
        addLocalAction(findAction);
        // addLocalAction(findReferencesAction);
        // addLocalAction(propertiesAction);
        addLocalAction(cloneDecompilerAction);
        // addLocalAction(goToNextBraceAction);
        // addLocalAction(goToPreviousBraceAction);
    }

    private void toggleOutgoingEvents() {
        allowOutgoingEvents = !allowOutgoingEvents;
    }

    public boolean shouldSendEvents() {
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

	public void tokenRenamed(EvtToken tokenAtCursor, String newName) {
		plugin.handleTokenRenamed(tokenAtCursor, newName);
	}

	public void handleTokenRenamed(EvtToken tokenAtCursor, String newName) {
		controller.getEvtPanel().tokenRenamed(tokenAtCursor, newName);
	}

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
