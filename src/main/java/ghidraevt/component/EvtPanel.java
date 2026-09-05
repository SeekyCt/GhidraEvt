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
 * Modified from ghidra/app/decompiler/component/DecompilerPanel.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.*;
import java.awt.event.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;

import docking.DockingUtils;
import docking.util.AnimationUtils;
import docking.util.SwingAnimationCallback;
import docking.widgets.EventTrigger;
import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.listener.*;
import docking.widgets.fieldpanel.support.*;
import docking.widgets.indexedscrollpane.IndexedScrollPane;
import generic.theme.GColor;
import ghidra.app.decompiler.component.margin.VerticalLayoutPixelIndexMap;
import ghidra.app.util.viewer.util.ScrollpaneAlignedHorizontalLayout;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.*;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import ghidra.util.task.SwingUpdateManager;
import ghidraevt.action.EvtSearchLocation;
import ghidraevt.action.EvtSearchResults;
import ghidraevt.highlight.EvtHighlightController;
import ghidraevt.highlight.EvtHighlightListener;
import ghidraevt.highlight.EvtHighlighter;
import ghidraevt.highlight.EvtTokenHighlightColors;
import ghidraevt.highlight.EvtTokenHighlightMatcher;
import ghidraevt.highlight.EvtTokenHighlighter;
import ghidraevt.highlight.EvtTokenHighlights;
import ghidraevt.hover.EvtHoverProvider;
import ghidraevt.hover.EvtHoverService;
import ghidraevt.location.DefaultEvtLocation;
import ghidraevt.location.EvtLocation;
import ghidraevt.location.EvtLocationInfo;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtDocument;
import ghidraevt.token.EvtLine;
import ghidraevt.token.EvtToken;

/**
 * Class to handle the display of a decompiled function
 */
public class EvtPanel extends JPanel implements FieldMouseListener, FieldLocationListener,
        FieldSelectionListener, EvtHighlightListener, LayoutListener {
    // Default color for specially highlighted tokens
    private final static Color SPECIAL_COLOR_DEF =
        new GColor("color.bg.decompiler.highlights.special");

    private final EvtController controller;
    private final EvtOptions options;
    private LineNumberEvtMarginProvider lineNumbersMargin;

    private final EvtFieldPanel fieldPanel;
    private EvtLayoutModel layoutController;
    private final IndexedScrollPane scroller;

    private final List<EvtMarginProvider> marginProviders = new ArrayList<>();
    private final VerticalLayoutPixelIndexMap pixmap = new VerticalLayoutPixelIndexMap();

    private FieldHighlightFactory hlFactory;
	private EvtHighlightController highlightController;
	private Map<String, EvtHighlighter> highlightersById = new HashMap<>();
	private PendingHighlightUpdate pendingHighlightUpdate;
	private SwingUpdateManager highlighCursorUpdater = new SwingUpdateManager(() -> {
		if (pendingHighlightUpdate != null) {
			pendingHighlightUpdate.doUpdate();
			pendingHighlightUpdate = null;
		}
	});

	private ActiveMiddleMouse activeMiddleMouse;
    private int middleMouseHighlightButton;
    private Color middleMouseHighlightColor;
    private Color currentVariableHighlightColor;

    private Color activeSearchHighlightColor;
    private Color searchHighlightColor;

    private EvtSearchResults currentSearchResults;

    private DisassembleData decompileData = new EmptyDisassembleData("No Script");
    private final EvtClipboardProvider clipboard;

    private Color originalBackgroundColor;
    private boolean navigationEnabled = true;

    private EvtHoverProvider decompilerHoverProvider;

    EvtPanel(EvtController controller, EvtOptions options, EvtClipboardProvider clipboard) {
        this.controller = controller;
        this.options = options;
        this.clipboard = clipboard;
        FontMetrics metrics = getFontMetrics(options);
        if (clipboard != null) {
            clipboard.setFontMetrics(metrics);
        }
        hlFactory = new SearchHighlightFactory();

        layoutController = new EvtLayoutModel(options, this, metrics, hlFactory);
        fieldPanel = new EvtFieldPanel(layoutController);

        scroller = new IndexedScrollPane(fieldPanel);
        fieldPanel.addFieldSelectionListener(this);
        fieldPanel.addFieldMouseListener(this);
        fieldPanel.addFieldLocationListener(this);
        fieldPanel.addLayoutListener(this);

        fieldPanel.setName("Evt Disassembler View");
        fieldPanel.getAccessibleContext().setAccessibleName("Evt Disassembler View");

        fieldPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (EvtMarginProvider provider : marginProviders) {
                    provider.getComponent().invalidate();
                }
                validate();
            }
        });

        setBackground(options.getBackgroundColor());

        decompilerHoverProvider = new EvtHoverProvider();

        activeSearchHighlightColor = options.getActiveSearchHighlightColor();
        searchHighlightColor = options.getSearchHighlightColor();
        currentVariableHighlightColor = options.getCurrentVariableHighlightColor();
        middleMouseHighlightColor = options.getMiddleMouseHighlightColor();
        middleMouseHighlightButton = options.getMiddleMouseHighlightButton();

        setLayout(new BorderLayout());
        add(scroller);

        setPreferredSize(new Dimension(600, 400));
        setDisassembleData(new EmptyDisassembleData("No Script"));

        if (options.isDisplayLineNumbers()) {
            addMarginProvider(lineNumbersMargin = new LineNumberEvtMarginProvider());
        }
    }

    public EvtController getController() {
        return controller;
    }

    public EvtDocument getLines() {
        return layoutController.getDocument();
    }

    public List<Field> getFields() {
        return Arrays.asList(layoutController.getFields());
    }

    public FieldPanel getFieldPanel() {
        return fieldPanel;
    }

//==================================================================================================
// Highlight Methods
//==================================================================================================

    public EvtTokenHighlightColors getSecondaryHighlightColors() {
        return highlightController.getSecondaryHighlightColors();
    }

    public boolean hasSecondaryHighlights(EvtScript script) {
        return highlightController.hasSecondaryHighlights(script);
    }

    public boolean hasSecondaryHighlight(EvtToken token) {
        return highlightController.hasSecondaryHighlight(token);
    }

    public Color getSecondaryHighlight(EvtToken token) {
        return highlightController.getSecondaryHighlight(token);
    }

    public EvtTokenHighlights getHighlights(EvtHighlighter highligter) {
        return highlightController.getHighlighterHighlights(highligter);
    }

    public EvtTokenHighlights getMiddleMouseHighlights() {
        if (activeMiddleMouse != null) {
            return activeMiddleMouse.getHighlights();
        }
        return null;
    }

    private Set<EvtHighlighter> getSecondaryHighlihgtersByFunction(EvtScript script) {
        return highlightController.getSecondaryHighlighters(script);
    }

    /**
     * Removes all secondary highlights for the current function
     *
     * @param function the function containing the secondary highlights
     */
    public void removeSecondaryHighlights(EvtScript function) {
        highlightController.removeSecondaryHighlights(function);
    }

    public void removeSecondaryHighlight(EvtToken token) {
        highlightController.removeSecondaryHighlights(token);
    }

    public void addSecondaryHighlight(EvtToken token) {
        EvtColorProvider cp = highlightController.getGeneratedColorProvider();
        addSecondaryHighlight(token.getText(), cp);
    }

    public void addSecondaryHighlight(EvtToken token, Color color) {
        EvtColorProvider cp = new DefaultEvtColorProvider("User Secondary Highlight", color);
        addSecondaryHighlight(token.getText(), cp);
    }

    private void addSecondaryHighlight(String tokenText, EvtColorProvider colorProvider) {
        EvtNameTokenMatcher matcher = new EvtNameTokenMatcher(tokenText, colorProvider);
        EvtHighlighter highlighter = createHighlighter(matcher);
        applySecondaryHighlights(highlighter);
    }

    private void applySecondaryHighlights(EvtHighlighter highlighter) {
        EvtScript function = decompileData.getScript();
        highlightController.addSecondaryHighlighter(function, highlighter);
        highlighter.applyHighlights();
    }

    private void toggleMiddleMouseHighlight(FieldLocation location, Field field) {
        EvtToken token = ((EvtTextField) field).getToken(location);

        ActiveMiddleMouse previousMiddleMouse = activeMiddleMouse;
        activeMiddleMouse = null;

        if (previousMiddleMouse != null) {
            // middle mousing always clears the last middle-mouse highlight
            previousMiddleMouse.clear();

            if (previousMiddleMouse.matches(token)) {
                // middle mousing on the same token clears, but does not create a new highlight
                return;
            }
        }

        // exclude tokens that users do not want to highlight
        // if (shouldIgnoreOpToken(token)) {
            // return;
        // }
        // if (shouldIgnoreSyntaxTokenHighlight(token)) {
        //     return;
        // }

        ActiveMiddleMouse newMiddleMouse = new ActiveMiddleMouse(token.getText());
        newMiddleMouse.apply();
        activeMiddleMouse = newMiddleMouse;
    }

    public void addHighlighterHighlights(EvtTokenHighlighter highlighter,
            Supplier<? extends Collection<EvtToken>> tokens, EvtColorProvider colorProvider) {
        highlightController.addHighlighterHighlights(highlighter, tokens, colorProvider);
    }

    public void removeHighlighterHighlights(EvtHighlighter highlighter) {
        highlightController.removeHighlighterHighlights(highlighter);
    }

    private EvtHighlighter createHighlighter(EvtTokenHighlightMatcher tm) {
        EvtScript function = decompileData.getScript();
        return createHighlighter(function, tm);
    }

    public EvtHighlighter createHighlighter(EvtScript f, EvtTokenHighlightMatcher tm) {
        UUID uuId = UUID.randomUUID();
        String id = uuId.toString();
        return createHighlighter(id, f, tm);
    }

    public EvtHighlighter createHighlighter(String id, EvtScript f,
            EvtTokenHighlightMatcher tm) {
        EvtHighlighter currentHighlighter = highlightersById.get(id);
        if (currentHighlighter != null) {
            currentHighlighter.dispose();
        }

        EvtTokenHighlighter newHighlighter = new EvtTokenHighlighter(id, this, f, tm);
        highlightersById.put(id, newHighlighter);
        highlightController.addHighlighter(newHighlighter);
        return newHighlighter;
    }

    public EvtHighlighter getHighlighter(String id) {
        return highlightersById.get(id);
    }

    public void removeHighlighter(String id) {
        EvtHighlighter highlighter = highlightersById.remove(id);
        highlightController.removeHighlighter(highlighter);
    }

    public void clearPrimaryHighlights() {
        highlightController.clearPrimaryHighlights();
    }

    public void addHighlights(/*Set<Varnode> varnodes,*/ EvtColorProvider colorProvider) {
        EvtDocument root = layoutController.getDocument();
        highlightController.addPrimaryHighlights(root, colorProvider);
    }

    public void setHighlightController(EvtHighlightController highlightController) {
        if (this.highlightController != null) {
            this.highlightController.removeListener(this);
        }

        this.highlightController = EvtHighlightController.dummyIfNull(highlightController);
        highlightController.setHighlightColor(currentVariableHighlightColor);
        highlightController.addListener(this);
    }

    public EvtHighlightController getHighlightController() {
        return highlightController;
    }

    @Override
    public void tokenHighlightsChanged() {
        repaint();
    }

    /**
     * This function is used to alert the panel that a token was renamed. If the token being renamed
     * had a middle-mouse or secondary highlight, we must re-apply the highlights to the new token.
     *
     * <p>
     * This is not needed for highlighter service highlights, since they get called again to
     * re-apply highlights. It is up to that highlighter to determine if highlighting still applies
     * to the new token name. Alternatively, for secondary highlights, we know the user chose the
     * highlight based upon name. Thus, when the name changes, we need to take action to update the
     * secondary highlight.
     *
     * @param token the token being renamed
     * @param newName the new name of the token
     */
    public void tokenRenamed(EvtToken token, String newName) {
        repairMiddleMouseSelectionForRename(token, newName);
        repairSecondarySelectionForRename(token, newName);
    }

    private void repairSecondarySelectionForRename(EvtToken token, String newName) {
        Color hlColor = highlightController.getSecondaryHighlight(token);
        if (hlColor == null) {
            return; // not highlighted
        }

        highlightController.removeSecondaryHighlights(token);

        // Add the new highlighter when we have rebuilt the token
        controller.doWhenNotBusy(() -> {
            addSecondaryHighlight(newName, t -> hlColor);
        });
    }

    private void repairMiddleMouseSelectionForRename(EvtToken token, String newName) {
        if (activeMiddleMouse == null || !activeMiddleMouse.matches(token)) {
            return;
        }

        activeMiddleMouse.clear();
        activeMiddleMouse = new ActiveMiddleMouse(newName);

        // Apply the new middle-mouse highlighter when we have rebuilt the token
        controller.doWhenNotBusy(() -> {
            activeMiddleMouse.apply();
        });
    }

    private void cloneServiceHiglighters(EvtPanel sourcePanel) {

        Set<EvtHighlighter> serviceHighlighters =
            sourcePanel.highlightController.getServiceHighlighters();

        for (EvtHighlighter otherHighlighter : serviceHighlighters) {

            if (!(otherHighlighter instanceof EvtTokenHighlighter clangHighlighter)) {
                continue;
            }

            EvtHighlighter newHighlighter = clangHighlighter.clone(this);
            highlightersById.put(newHighlighter.getId(), newHighlighter);

            EvtTokenHighlights otherHighlighterTokens =
                sourcePanel.highlightController.getHighlighterHighlights(otherHighlighter);
            if (otherHighlighterTokens == null || otherHighlighterTokens.isEmpty()) {
                // The highlighter has been created but no highlights have been applied.  It is up
                // to the client to apply the highlights. The new highlighter will respond to the
                // client request if the later apply the highlights.
                continue;
            }

            newHighlighter.applyHighlights();
        }
    }

    /**
     * Called by the provider to clone all highlights in the source panel and apply them to this
     * panel
     *
     * @param sourcePanel the panel that was cloned
     */
    public void cloneHighlights(EvtPanel sourcePanel) {

        EvtScript script = decompileData.getScript();
        cloneServiceHiglighters(sourcePanel);

        //
        // Keep only those secondary highlighters for the current function.  This ensures that the
        // clone will match the cloned decompiler.
        //
        Set<EvtHighlighter> secondaryHighlighters =
            sourcePanel.getSecondaryHighlihgtersByFunction(script);

        //
        // We do NOT clone the secondary highlighters.  This allows the user the remove them
        // from the primary provider without effecting the cloned provider and vice versa.
        //
        for (EvtHighlighter highlighter : secondaryHighlighters) {

            if (!(highlighter instanceof EvtTokenHighlighter clangHighlighter)) {
                continue;
            }

            EvtHighlighter newHighlighter = clangHighlighter.copy(this);
            highlightersById.put(newHighlighter.getId(), newHighlighter);
            applySecondaryHighlights(newHighlighter);
        }
    }

//==================================================================================================
// End Highlight Methods
//==================================================================================================

    @Override
    public void setBackground(Color bg) {
        originalBackgroundColor = bg;
        if (fieldPanel != null) {
            fieldPanel.setBackgroundColor(bg);
            scroller.setBackground(bg);
        }
        super.setBackground(bg);
    }

    /**
     * This function sets the current window display based on our display state
     *
     * @param decompileData the new data
     */
    void setDisassembleData(DisassembleData decompileData) {
        if (layoutController == null) {
            // we've been disposed!
            return;
        }

        DisassembleData oldData = this.decompileData;
        this.decompileData = decompileData;
        EvtScript script = decompileData.getScript();
        if (decompileData.hasDisassembleResults()) {
            layoutController.buildLayouts(script, decompileData.getDocroot(), null, true);
        }
        else {
            layoutController.buildLayouts(null, null, decompileData.getErrorMessage(), true);
        }

        setLocation(oldData, decompileData);

        decompilerHoverProvider.setProgram(decompileData.getProgram());

        // give user notice when seeing the decompile of a non-function
        setBackground(originalBackgroundColor);

        if (clipboard != null) {
            clipboard.selectionChanged(null);
        }

        // don't highlight search results across functions
        if (currentSearchResults != null) {
            currentSearchResults.disassemblerUpdated();
            currentSearchResults = null;
        }

        if (script != null) {
            highlightController.reapplyAllHighlights(script);
        }
    }

    private void setLocation(DisassembleData oldData, DisassembleData newData) {
        EvtScript script = oldData.getScript();
        if (SystemUtilities.isEqual(script, newData.getScript())) {
            return;
        }

        ProgramLocation location = newData.getLocation();
        if (location != null) {
            setLocation(location, newData.getViewerPosition());
        }
    }

    public EvtLayoutModel getLayoutController() {
        return layoutController;
    }

    public boolean containsLocation(ProgramLocation location) {
        return decompileData.contains(location);
    }

    public void setLocation(ProgramLocation location, ViewerPosition viewerPosition) {
        repaint();
        if (location.getAddress() == null) {
            return;
        }

        if (viewerPosition != null) {
            fieldPanel.setViewerPosition(viewerPosition.getIndex(), viewerPosition.getXOffset(),
                viewerPosition.getYOffset());
        }

        if (location instanceof EvtLocation) {
            EvtLocation decompilerLocation = (EvtLocation) location;
            fieldPanel.goTo(BigInteger.valueOf(decompilerLocation.getLineNumber()), 0, 0,
                decompilerLocation.getCharPos(), false);
            return;
        }

        List<EvtToken> tokens =
            EvtUtils.getTokensFromView(layoutController.getFields(), location.getAddress());
        goToBeginningOfLine(tokens);
    }

    /**
     * Put cursor on first token in the list
     *
     * @param tokens the tokens to search for
     */
    private void goToBeginningOfLine(List<EvtToken> tokens) {
        if (tokens.isEmpty()) {
            return;
        }

        int firstLineNumber =
            EvtUtils.findIndexOfFirstField(tokens, layoutController.getFields());
        if (firstLineNumber != -1) {
            fieldPanel.goTo(BigInteger.valueOf(firstLineNumber), 0, 0, 0, false);
        }
    }

    public void goToToken(EvtToken token) {

        EvtLine line = token.getLineParent();

        int offset = 0;
        List<EvtToken> tokens = line.getAllTokens();
        for (EvtToken lineToken : tokens) {
            if (lineToken.equals(token)) {
                break;
            }
            offset += lineToken.getText().length();
        }

        // -1 since the FieldPanel is 0-based; we are 1-based
        int lineNumber = line.getLineNumber() - 1;
        int column = offset;
        FieldLocation start = getCursorPosition();

        int distance = getOffscreenDistance(lineNumber);
        if (distance == 0) {
            fieldPanel.navigateTo(lineNumber, column);
            return;
        }

        ScrollingCallback callback = new ScrollingCallback(start, lineNumber, column, distance);
        AnimationUtils.executeSwingAnimationCallback(callback);
    }

    private int getOffscreenDistance(int line) {

        AnchoredLayout start = fieldPanel.getVisibleStartLayout();
        int visibleStartLine = start.getIndex().intValue();
        if (visibleStartLine > line) {
            // the end is off the top of the screen
            return visibleStartLine - line;
        }

        AnchoredLayout end = fieldPanel.getVisibleEndLayout();
        int visibleEndLine = end.getIndex().intValue();
        if (visibleEndLine < line) {
            // the end is off the bottom of the screen
            return line - visibleEndLine;
        }

        return 0;
    }

    void setSelection(ProgramSelection selection) {
        FieldSelection fieldSelection = null;
        if (selection == null || selection.isEmpty()) {
            fieldSelection = new FieldSelection();
        }
        else {
            List<EvtToken> tokens =
                EvtUtils.getTokens(layoutController.getDocument(), selection);
            fieldSelection = EvtUtils.getFieldSelection(tokens);
        }
        fieldPanel.setSelection(fieldSelection);
    }

    public void setDecompilerHoverProvider(EvtHoverProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Cannot set the hover handler to null!");
        }

        if (decompilerHoverProvider != null) {
            if (decompilerHoverProvider.isShowing()) {
                decompilerHoverProvider.closeHover();
            }
            decompilerHoverProvider.initializeListingHoverHandler(provider);
            decompilerHoverProvider.dispose();
        }
        decompilerHoverProvider = provider;
    }

    public void dispose() {
        setDisassembleData(new EmptyDisassembleData("Disposed"));
        layoutController = null;
        decompilerHoverProvider.dispose();
        highlighCursorUpdater.dispose();
        highlightController.dispose();
        highlightersById.clear();
    }

    public FontMetrics getFontMetrics() {
        Font font = options.getDefaultFont();
        return super.getFontMetrics(font);
    }

    private FontMetrics getFontMetrics(EvtOptions decompileOptions) {
        Font font = decompileOptions.getDefaultFont();
        return getFontMetrics(font);
    }

    /**
     * Passing false signals to disallow navigating to new functions from within the panel by using
     * the mouse.
     *
     * @param enabled false disabled mouse function navigation
     */
    void setMouseNavigationEnabled(boolean enabled) {
        navigationEnabled = enabled;
    }

    @Override
    public void buttonPressed(FieldLocation location, Field field, MouseEvent ev) {
        if (!decompileData.hasDisassembleResults()) {
            return;
        }

        int clickCount = ev.getClickCount();
        int buttonState = ev.getButton();
        if (buttonState == MouseEvent.BUTTON1) {
            if (DockingUtils.isControlModifier(ev) && clickCount == 2) {
                tryToGoto(location, field, ev, true);
            }
            else if (clickCount == 2) {
                tryToGoto(location, field, ev, false);
            }
            else if (DockingUtils.isControlModifier(ev) && ev.isShiftDown()) {
                controller.exportLocation();
            }
        }

        if (buttonState == middleMouseHighlightButton && clickCount == 1) {
            toggleMiddleMouseHighlight(location, field);
        }
    }

    private void tryToGoto(FieldLocation location, Field field, MouseEvent event,
            boolean newWindow) {
        if (!navigationEnabled) {
            return;
        }

        EvtTextField textField = (EvtTextField) field;
        EvtToken token = textField.getToken(location);

        if (token instanceof EvtAddrToken addr) {
            controller.goToAddress(addr.getTarget(), newWindow);
        }
        else
        {
            Address addr = token.getMinAddress();
            controller.goToAddress(addr, newWindow);
        }
    }

    // private void tryGoToComment(FieldLocation location, MouseEvent event, ClangTextField textField,
    //         boolean newWindow) {

    //     // comments may use annotations; tell the annotation it was clicked
    //     FieldElement clickedElement = textField.getClickedObject(location);
    //     if (clickedElement instanceof AnnotatedTextFieldElement) {
    //         AnnotatedTextFieldElement annotation = (AnnotatedTextFieldElement) clickedElement;
    //         controller.annotationClicked(annotation, event, newWindow);
    //         return;
    //     }

    //     String text = textField.getText();
    //     String word = StringUtilities.findWord(text, location.col);
    //     tryGoToScalar(word, newWindow);
    // }

    Program getProgram() {
        return decompileData.getProgram();
    }

    public ProgramLocation getCurrentLocation() {
        if (!decompileData.hasDisassembleResults()) {
            return null;
        }
        Field currentField = fieldPanel.getCurrentField();
        FieldLocation cursorPosition = fieldPanel.getCursorLocation();
        return getProgramLocation(currentField, cursorPosition);
    }

    @Override
    public void fieldLocationChanged(FieldLocation location, Field field, EventTrigger trigger) {
        if (!decompileData.hasDisassembleResults()) {
            return;
        }

        pendingHighlightUpdate = new PendingHighlightUpdate(location, field, trigger);
        highlighCursorUpdater.update();

        if (!(field instanceof EvtTextField)) {
            return;
        }

        EvtToken tok = ((EvtTextField) field).getToken(location);
        if (tok == null) {
            return;
        }

        // only broadcast when the user is clicking around
        if (trigger == EventTrigger.GUI_ACTION) {
            ProgramLocation programLocation = getProgramLocation(field, location);
            if (programLocation != null) {
                controller.locationChanged(programLocation);
            }
        }
    }

    @Override
    public void selectionChanged(FieldSelection selection, EventTrigger trigger) {
        if (clipboard != null) {
            clipboard.selectionChanged(selection);
        }
        if (!decompileData.hasDisassembleResults()) {
            return;
        }
        if (trigger != EventTrigger.API_CALL) {
            Program program = decompileData.getProgram();
            Field[] lines = layoutController.getFields();
            List<EvtToken> tokenList = EvtUtils.getTokensInSelection(selection, lines);
            AddressSpace functionSpace = decompileData.getFunctionSpace();
            AddressSet addrset =
                EvtUtils.findClosestAddressSet(program, functionSpace, tokenList);
            ProgramSelection programSelection = new ProgramSelection(addrset);
            controller.selectionChanged(programSelection);
        }
    }

    @Override
    public void layoutsChanged(List<AnchoredLayout> layouts) {
        pixmap.layoutsChanged(layouts);
        for (EvtMarginProvider element : marginProviders) {
            element.setProgram(getProgram(), layoutController, pixmap);
        }
    }

    private ProgramLocation getProgramLocation(Field field, FieldLocation location) {
        if (!(field instanceof EvtTextField)) {
            return null;
        }
        EvtToken token = ((EvtTextField) field).getToken(location);
        if (token == null) {
            return null;
        }

        Address address = EvtUtils.getClosestAddress(getProgram(), token);
        if (address == null) {
            address = EvtUtils.findAddressBefore(layoutController.getFields(), token);
        }

        EvtScript script = decompileData.getScript();
        if (address == null) {
            address = script.getStartAddress();
        }

        Address entryPoint = script.getStartAddress();
        DisassembleResults results = decompileData.getDisassembleResults();
        int lineNumber = location.getIndex().intValue();
        int charPos = location.col;
        EvtLocationInfo info =
            new EvtLocationInfo(entryPoint, results, token, lineNumber, charPos);
        Program program = decompileData.getProgram();

        return new DefaultEvtLocation(program, address, info);
    }

    public void clearSearchResults(EvtSearchResults searchResults) {
        if (currentSearchResults == searchResults) {
            currentSearchResults = null;
            repaint();
        }
    }

    public void setSearchResults(EvtSearchResults searchResults) {
        currentSearchResults = searchResults;

        if (currentSearchResults != null) {
            EvtSearchLocation location = currentSearchResults.getActiveLocation();
            if (location != null) {
                setCursorPosition(location.getFieldLocation());
            }
        }

        repaint();
    }

    public EvtSearchLocation getActiveSearchLocation() {
        if (currentSearchResults == null) {
            return null;
        }
        EvtSearchLocation location = currentSearchResults.getActiveLocation();
        return location;
    }

    public Color getCurrentVariableHighlightColor() {
        return currentVariableHighlightColor;
    }

    public Color getMiddleMouseHighlightColor() {
        return middleMouseHighlightColor;
    }

    /**
     * The color used in a primary highlight to mark the token that was clicked. This is used in
     * 'slice' actions to mark the source of the slice.
     *
     * @return the color
     */
    public Color getSpecialHighlightColor() {
        return SPECIAL_COLOR_DEF;
    }

    public String getHighlightedText() {
        EvtToken token = highlightController.getHighlightedToken();
        if (token == null) {
            return null;
        }
        // if (token instanceof ClangCommentToken) {
        //     return null; // comments are not single words that get highlighted
        // }
        return token.getText();
    }

    public String getTextUnderCursor() {

        FieldLocation location = fieldPanel.getCursorLocation();
        EvtTextField textField = (EvtTextField) fieldPanel.getCurrentField();
        if (textField == null) {
            return null;
        }

		// ClangToken token = textField.getToken(location);
		// if (!(token instanceof ClangCommentToken)) {
		// 	return token.getText(); // non-comment tokens are not multi-word; use the token's text
		// }

        FieldElement clickedElement = textField.getClickedObject(location);
        if (clickedElement instanceof AnnotatedTextFieldElement) {
            AnnotatedTextFieldElement annotation = (AnnotatedTextFieldElement) clickedElement;
            return annotation.getDisplayString();
        }

        String text = textField.getText();
        return StringUtilities.findWord(text, location.col);
    }

    public String getSelectedText() {
        FieldSelection selection = fieldPanel.getSelection();
        if (selection.isEmpty()) {
            return null;
        }

        return FieldSelectionHelper.getFieldSelectionText(selection, fieldPanel);
    }

    public FieldLocation getCursorPosition() {
        return fieldPanel.getCursorLocation();
    }

    public void setCursorPosition(FieldLocation fieldLocation) {
        fieldPanel.setCursorPosition(fieldLocation.getIndex(), fieldLocation.getFieldNum(),
            fieldLocation.getRow(), fieldLocation.getCol());
        fieldPanel.scrollToCursor();
    }

    /**
     * Returns a single selected token; null if there is no selection or multiple tokens selected.
     *
     * @return a single selected token; null if there is no selection or multiple tokens selected.
     */
    public EvtToken getSelectedToken() {
        FieldSelection selection = fieldPanel.getSelection();
        if (selection.isEmpty()) {
            return null;
        }

        Field[] lines = layoutController.getFields();
        List<EvtToken> tokens = EvtUtils.getTokensInSelection(selection, lines);

        long count = tokens.stream().filter(t -> !t.getText().trim().isEmpty()).count();
        if (count == 1) {
            return tokens.get(0);
        }
        return null;
    }

    public EvtToken getTokenAtCursor() {
        FieldLocation cursorPosition = fieldPanel.getCursorLocation();
        Field field = fieldPanel.getCurrentField();
        if (field == null) {
            return null;
        }
        return ((EvtTextField) field).getToken(cursorPosition);
    }

    /**
     * Get the line number for the given y position, relative to the scroll panel
     *
     * <p>
     * If the y position is below all the lines, the last line is returned.
     *
     * @param y the y position
     * @return the line number, or 0 if not applicable
     */
    public int getLineNumber(int y) {
        return pixmap.getIndex(y).intValue() + 1;
    }

    public EvtOptions getOptions() {
        return options;
    }

    public void addHoverService(EvtHoverService hoverService) {
        decompilerHoverProvider.addHoverService(hoverService);
    }

    public void removeHoverService(EvtHoverService hoverService) {
        decompilerHoverProvider.removeHoverService(hoverService);
    }

    public void setHoverMode(boolean enabled) {
        decompilerHoverProvider.setHoverEnabled(enabled);
        if (enabled) {
            fieldPanel.setHoverProvider(decompilerHoverProvider);
        }
        else {
            fieldPanel.setHoverProvider(null);
        }
    }

    public boolean isHoverShowing() {
        return decompilerHoverProvider.isShowing();
    }

    public List<EvtToken> findTokensByName(String name) {
        List<EvtToken> tokens = new ArrayList<>();
        doFindTokensByName(tokens, layoutController.getDocument(), name);
        return tokens;
    }

    private void doFindTokensByName(List<EvtToken> tokens, EvtDocument lines, String name) {
        for (EvtLine line : lines) {
            for (EvtToken token : line.getAllTokens()) {
                if (name.equals(token.getText())) {
                    tokens.add(token);
                }
            }
        }
    }

    public ViewerPosition getViewerPosition() {
        return fieldPanel.getViewerPosition();
    }

    public void setViewerPosition(ViewerPosition viewerPosition) {
        fieldPanel.setViewerPosition(viewerPosition.getIndex(), viewerPosition.getXOffset(),
            viewerPosition.getYOffset());
    }

    @Override
    public void requestFocus() {
        fieldPanel.requestFocus();
    }

    public void selectAll(EventTrigger trigger) {
        BigInteger numIndexes = layoutController.getNumIndexes();
        FieldSelection selection = new FieldSelection();
        selection.addRange(BigInteger.ZERO, numIndexes);
        fieldPanel.setSelection(selection, trigger);
    }

    public void optionsChanged(EvtOptions decompilerOptions) {
        setBackground(decompilerOptions.getBackgroundColor());
        currentVariableHighlightColor = options.getCurrentVariableHighlightColor();
        middleMouseHighlightColor = decompilerOptions.getMiddleMouseHighlightColor();
        middleMouseHighlightButton = decompilerOptions.getMiddleMouseHighlightButton();
        searchHighlightColor = decompilerOptions.getSearchHighlightColor();

        highlightController.setHighlightColor(currentVariableHighlightColor);

        if (options.isDisplayLineNumbers()) {
            if (lineNumbersMargin == null) {
                addMarginProvider(lineNumbersMargin = new LineNumberEvtMarginProvider());
            }
        }
        else {
            if (lineNumbersMargin != null) {
                removeMarginProvider(lineNumbersMargin);
                lineNumbersMargin = null;
            }
        }

        for (EvtMarginProvider element : marginProviders) {
            element.setOptions(options);
        }
    }

    public void addMarginProvider(EvtMarginProvider provider) {
        marginProviders.add(0, provider);
        provider.setOptions(options);
        provider.setProgram(getProgram(), layoutController, pixmap);
        buildPanels();
    }

    public void removeMarginProvider(EvtMarginProvider provider) {
        marginProviders.remove(provider);
        buildPanels();
    }

    @Override
    public synchronized void addFocusListener(FocusListener l) {
        // we are not focusable, defer to contained field panel
        fieldPanel.addFocusListener(l);
    }

    @Override
    public synchronized void removeFocusListener(FocusListener l) {
        // we are not focusable, defer to contained field panel
        fieldPanel.removeFocusListener(l);
    }

    private void buildPanels() {
        removeAll();
        add(buildLeftComponent(), BorderLayout.WEST);
        add(scroller, BorderLayout.CENTER);
    }

    private JComponent buildLeftComponent() {
        JPanel leftPanel = new JPanel(new ScrollpaneAlignedHorizontalLayout(scroller));
        for (EvtMarginProvider marginProvider : marginProviders) {
            leftPanel.add(marginProvider.getComponent());
        }
        return leftPanel;
    }

//==================================================================================================
// Inner Classes
//==================================================================================================

    private class SearchHighlightFactory implements FieldHighlightFactory {

        @Override
        public Highlight[] createHighlights(Field field, String text, int cursorTextOffset) {
            if (currentSearchResults == null) {
                return new Highlight[0];
            }

            EvtTextField cField = (EvtTextField) field;
            int lineNumber = cField.getLineNumber();
            Map<Integer, List<EvtSearchLocation>> locationsByLine =
                currentSearchResults.getLocationsByLine();
            List<EvtSearchLocation> locationsOnLine = locationsByLine.get(lineNumber);
            if (locationsOnLine == null) {
                return new Highlight[0];
            }

            EvtSearchLocation activeLocation = currentSearchResults.getActiveLocation();
            List<Highlight> highlights = new ArrayList<>();
            for (EvtSearchLocation location : locationsOnLine) {
                Color c =
                    location == activeLocation ? activeSearchHighlightColor : searchHighlightColor;
                int start = location.getStartIndexInclusive();
                int end = location.getEndIndexInclusive();
                highlights.add(new Highlight(start, end, c));
            }

            return highlights.toArray(Highlight[]::new);
        }
    }

    /**
     * A simple class that handles the animators callback to scroll the display
     */
    private class ScrollingCallback implements SwingAnimationCallback {

        private int startLine;
        private int endLine;
        private int endColumn;
        private int duration;

        ScrollingCallback(FieldLocation start, int endLineNumber, int endColumn, int distance) {
            this.startLine = start.getIndex().intValue();
            this.endLine = endLineNumber;
            this.endColumn = endColumn;

            // have things nearby execute more quickly so users don't wait needlessly
            double rate = Math.pow(distance, .8);
            int ms = (int) rate * 100;
            this.duration = Math.min(1000, ms);
        }

        @Override
        public int getDuration() {
            return duration;
        }

        @Override
        public void progress(double percentComplete) {

            int length = Math.abs(endLine - startLine);
            long offset = Math.round(length * percentComplete);
            int current = 0;
            if (startLine > endLine) {
                // backwards
                current = (int) (startLine - offset);
            }
            else {
                current = (int) (startLine + offset);
            }

            FieldLocation location = new FieldLocation(BigInteger.valueOf(current));
            fieldPanel.scrollTo(location);
        }

        @Override
        public void done() {
            fieldPanel.goTo(BigInteger.valueOf(endLine), 0, 0, endColumn, false);
        }
    }

    private class EvtFieldPanel extends FieldPanel {

        public EvtFieldPanel(LayoutModel model) {
            super(model, "Evt Disassembler");
            // In the decompiler each field represents a line, so make the field description
            // simply be the line number
            setFieldDescriptionProvider((l, f) -> {
                if (f == null) {
                    return null;
                }
                return "line " + (l.getIndex().intValue() + 1);
            });
        }

        /**
         * Moves this field panel to the given line and column. Further, this navigation will fire
         * an event to the rest of the tool. (This is in contrast to a field panel
         * <code>goTo</code>, which we use to simply move the cursor, but not trigger an tool-level
         * navigation event.)
         *
         * @param lineNumber the line number
         * @param column the column within the line
         */
        void navigateTo(int lineNumber, int column) {
            fieldPanel.goTo(BigInteger.valueOf(lineNumber), 0, 0, column, false,
                EventTrigger.GUI_ACTION);
        }
    }

	/**
	 * A class to track pending location updates. This allows us to buffer updates, only sending the
	 * last one received.
	 */
	private class PendingHighlightUpdate {

		private FieldLocation location;
		private Field field;
		private EventTrigger trigger;
		private long updateId;

		PendingHighlightUpdate(FieldLocation location, Field field, EventTrigger trigger) {
			this.location = location;
			this.field = field;
			this.trigger = trigger;
			this.updateId = highlightController.getUpdateId();
		}

		void doUpdate() {
			// Note: don't send this buffered cursor change highlight if some other highlight
			//       has been applied.  Otherwise, this highlight would overwrite the last
			//       applied highlight.
			long lastUpdateId = highlightController.getUpdateId();
			if (updateId == lastUpdateId) {
				highlightController.fieldLocationChanged(location, field, trigger);
			}
		}
	}

	private class MiddleMouseColorProvider implements EvtColorProvider {

		@Override
		public Color getColor(EvtToken token) {
			return middleMouseHighlightColor;
		}

		@Override
		public String toString() {
			return "Middle Mouse Color Provider " + middleMouseHighlightColor;
		}
	}

	/**
	 * A class to track the current middle moused token.
	 */
	private class ActiveMiddleMouse {

		private String tokenText;
		private EvtHighlighter highlighter;

		ActiveMiddleMouse(String tokenText) {
			this.tokenText = tokenText;

			EvtColorProvider cp = new MiddleMouseColorProvider();
			EvtNameTokenMatcher matcher = new EvtNameTokenMatcher(tokenText, cp);
			this.highlighter = createHighlighter(matcher);
		}

		EvtTokenHighlights getHighlights() {
			return highlightController.getHighlighterHighlights(highlighter);
		}

		boolean matches(EvtToken other) {
			return tokenText.equals(other.getText());
		}

		void clear() {
			highlightController.removeHighlighter(highlighter);
		}

		void apply() {
			applySecondaryHighlights(highlighter);
		}

		@Override
		public String toString() {
			return "Middle Mouse Token " + tokenText;
		}
	}
}
