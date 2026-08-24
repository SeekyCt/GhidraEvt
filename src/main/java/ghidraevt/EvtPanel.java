package ghidraevt;

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
import ghidra.app.decompiler.*;
import ghidra.app.decompiler.component.ClangTextField;
import ghidra.app.decompiler.component.hover.DecompilerHoverService;
import ghidra.app.decompiler.component.margin.*;
import ghidra.app.decompiler.location.*;
import ghidra.app.plugin.core.decompile.DecompilerClipboardProvider;
import ghidra.app.plugin.core.decompile.actions.DecompilerSearchLocation;
import ghidra.app.plugin.core.decompile.actions.DecompilerSearchResults;
import ghidra.app.util.viewer.util.ScrollpaneAlignedHorizontalLayout;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.pcode.*;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.*;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import ghidra.util.task.SwingUpdateManager;

/**
 * Class to handle the display of a decompiled function
 */
public class EvtPanel extends JPanel implements FieldMouseListener, FieldLocationListener,
		FieldSelectionListener, LayoutListener {

	private final static Color NON_FUNCTION_BACKGROUND_COLOR_DEF = new GColor("color.bg.undefined");

	// Default color for specially highlighted tokens
	private final static Color SPECIAL_COLOR_DEF =
		new GColor("color.bg.decompiler.highlights.special");

	private final EvtController controller;
	private final EvtOptions options;
	private LineNumberDecompilerMarginProvider lineNumbersMargin;

	private final EvtFieldPanel fieldPanel;
	private EvtLayoutModel layoutController;
	private final IndexedScrollPane scroller;

	private final List<DecompilerMarginProvider> marginProviders = new ArrayList<>();
	private final VerticalLayoutPixelIndexMap pixmap = new VerticalLayoutPixelIndexMap();

	private FieldHighlightFactory hlFactory;

	private int middleMouseHighlightButton;
	private Color middleMouseHighlightColor;
	private Color currentVariableHighlightColor;

	private Color activeSearchHighlightColor;
	private Color searchHighlightColor;

	private DecompilerSearchResults currentSearchResults;

	private EvtData decompileData = new EmptyEvtData("No Function");
	private final EvtClipboardProvider clipboard;

	private Color originalBackgroundColor;
	private boolean navigationEnabled = true;

	// private DecompilerHoverProvider decompilerHoverProvider;

	EvtPanel(EvtController controller, EvtOptions options, EvtClipboardProvider clipboard) {
		this.controller = controller;
		this.options = options;
		this.clipboard = clipboard;
		FontMetrics metrics = getFontMetrics(options);
		if (clipboard != null) {
			clipboard.setFontMetrics(metrics);
		}
		// hlFactory = new SearchHighlightFactory();

		layoutController = new EvtLayoutModel(options, this, metrics, hlFactory);
		fieldPanel = new EvtFieldPanel(layoutController);

		scroller = new IndexedScrollPane(fieldPanel);
		fieldPanel.addFieldSelectionListener(this);
		fieldPanel.addFieldMouseListener(this);
		fieldPanel.addFieldLocationListener(this);
		fieldPanel.addLayoutListener(this);

		fieldPanel.setName("Decompiler View");
		fieldPanel.getAccessibleContext().setAccessibleName("Decompiler View");

		fieldPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				for (DecompilerMarginProvider provider : marginProviders) {
					provider.getComponent().invalidate();
				}
				validate();
			}
		});

		setBackground(options.getBackgroundColor());

		// decompilerHoverProvider = new DecompilerHoverProvider();

		activeSearchHighlightColor = options.getActiveSearchHighlightColor();
		searchHighlightColor = options.getSearchHighlightColor();
		currentVariableHighlightColor = options.getCurrentVariableHighlightColor();
		middleMouseHighlightColor = options.getMiddleMouseHighlightColor();
		middleMouseHighlightButton = options.getMiddleMouseHighlightButton();

		setLayout(new BorderLayout());
		add(scroller);

		setPreferredSize(new Dimension(600, 400));
		setEvtData(new EmptyEvtData("No Function"));

		// if (options.isDisplayLineNumbers()) {
		// 	addMarginProvider(lineNumbersMargin = new LineNumberDecompilerMarginProvider());
		// }
	}

	public EvtController getController() {
		return controller;
	}

	public List<EvtLine> getLines() {
		return layoutController.getLines();
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

	// public TokenHighlightColors getSecondaryHighlightColors() {
	// 	return highlightController.getSecondaryHighlightColors();
	// }

	// public boolean hasSecondaryHighlights(Function function) {
	// 	return highlightController.hasSecondaryHighlights(function);
	// }

	// public boolean hasSecondaryHighlight(EvtToken token) {
	// 	return highlightController.hasSecondaryHighlight(token);
	// }

	// public Color getSecondaryHighlight(EvtToken token) {
	// 	return highlightController.getSecondaryHighlight(token);
	// }

	// public TokenHighlights getHighlights(DecompilerHighlighter highligter) {
	// 	return highlightController.getHighlighterHighlights(highligter);
	// }

	// public TokenHighlights getMiddleMouseHighlights() {
	// 	if (activeMiddleMouse != null) {
	// 		return activeMiddleMouse.getHighlights();
	// 	}
	// 	return null;
	// }

	// private Set<DecompilerHighlighter> getSecondaryHighlihgtersByFunction(Function function) {
	// 	return highlightController.getSecondaryHighlighters(function);
	// }

	/**
	 * Removes all secondary highlights for the current function
	 *
	 * @param function the function containing the secondary highlights
	 */
	// public void removeSecondaryHighlights(Function function) {
	// 	highlightController.removeSecondaryHighlights(function);
	// }

	// public void removeSecondaryHighlight(EvtToken token) {
	// 	highlightController.removeSecondaryHighlights(token);
	// }

	// public void addSecondaryHighlight(EvtToken token) {
	// 	ColorProvider cp = highlightController.getGeneratedColorProvider();
	// 	addSecondaryHighlight(token.getText(), cp);
	// }

	// public void addSecondaryHighlight(EvtToken token, Color color) {
	// 	ColorProvider cp = new DefaultColorProvider("User Secondary Highlight", color);
	// 	addSecondaryHighlight(token.getText(), cp);
	// }

	// private void addSecondaryHighlight(String tokenText, ColorProvider colorProvider) {
	// 	NameTokenMatcher matcher = new NameTokenMatcher(tokenText, colorProvider);
	// 	DecompilerHighlighter highlighter = createHighlighter(matcher);
	// 	applySecondaryHighlights(highlighter);
	// }

	// private void applySecondaryHighlights(DecompilerHighlighter highlighter) {
	// 	Function function = decompileData.getData();
	// 	highlightController.addSecondaryHighlighter(function, highlighter);
	// 	highlighter.applyHighlights();
	// }

	// private void toggleMiddleMouseHighlight(FieldLocation location, Field field) {
	// 	EvtToken token = ((ClangTextField) field).getToken(location);

	// 	ActiveMiddleMouse previousMiddleMouse = activeMiddleMouse;
	// 	activeMiddleMouse = null;

	// 	if (previousMiddleMouse != null) {
	// 		// middle mousing always clears the last middle-mouse highlight
	// 		previousMiddleMouse.clear();

	// 		if (previousMiddleMouse.matches(token)) {
	// 			// middle mousing on the same token clears, but does not create a new highlight
	// 			return;
	// 		}
	// 	}

	// 	// exclude tokens that users do not want to highlight
	// 	if (shouldIgnoreOpToken(token)) {
	// 		return;
	// 	}
	// 	if (shouldIgnoreSyntaxTokenHighlight(token)) {
	// 		return;
	// 	}

	// 	ActiveMiddleMouse newMiddleMouse = new ActiveMiddleMouse(token.getText());
	// 	newMiddleMouse.apply();
	// 	activeMiddleMouse = newMiddleMouse;
	// }

	// private boolean shouldIgnoreOpToken(EvtToken token) {
	// 	if (!(token instanceof ClangOpToken)) {
	// 		return false;
	// 	}

	// 	// users would like to be able to highlight return statements
	// 	String text = token.toString();
	// 	return !text.equals("return");
	// }

	// private boolean shouldIgnoreSyntaxTokenHighlight(EvtToken token) {

	// 	if (!(token instanceof ClangSyntaxToken syntaxToken)) {
	// 		return false;
	// 	}

	// 	String string = syntaxToken.toString();
	// 	return ignoredMiddleMouseTokens.contains(string);
	// }

	// void addHighlighterHighlights(ClangDecompilerHighlighter highlighter,
	// 		Supplier<? extends Collection<EvtToken>> tokens, ColorProvider colorProvider) {
	// 	highlightController.addHighlighterHighlights(highlighter, tokens, colorProvider);
	// }

	// void removeHighlighterHighlights(DecompilerHighlighter highlighter) {
	// 	highlightController.removeHighlighterHighlights(highlighter);
	// }

	// private DecompilerHighlighter createHighlighter(CTokenHighlightMatcher tm) {
	// 	Function function = decompileData.getData();
	// 	return createHighlighter(function, tm);
	// }

	// public DecompilerHighlighter createHighlighter(Function f, CTokenHighlightMatcher tm) {
	// 	UUID uuId = UUID.randomUUID();
	// 	String id = uuId.toString();
	// 	return createHighlighter(id, f, tm);
	// }

	// public DecompilerHighlighter createHighlighter(String id, Function f,
	// 		CTokenHighlightMatcher tm) {
	// 	DecompilerHighlighter currentHighlighter = highlightersById.get(id);
	// 	if (currentHighlighter != null) {
	// 		currentHighlighter.dispose();
	// 	}

	// 	ClangDecompilerHighlighter newHighlighter = new ClangDecompilerHighlighter(id, this, f, tm);
	// 	highlightersById.put(id, newHighlighter);
	// 	highlightController.addHighlighter(newHighlighter);
	// 	return newHighlighter;
	// }

	// public DecompilerHighlighter getHighlighter(String id) {
	// 	return highlightersById.get(id);
	// }

	// void removeHighlighter(String id) {
	// 	DecompilerHighlighter highlighter = highlightersById.remove(id);
	// 	highlightController.removeHighlighter(highlighter);
	// }

	// public void clearPrimaryHighlights() {
	// 	highlightController.clearPrimaryHighlights();
	// }

	// public void addHighlights(Set<Varnode> varnodes, ColorProvider colorProvider) {
	// 	ClangTokenGroup root = layoutController.getRoot();
	// 	highlightController.addPrimaryHighlights(root, colorProvider);
	// }

	// public void addHighlights(Set<PcodeOp> ops, Color hlColor) {
	// 	ClangTokenGroup root = layoutController.getRoot();
	// 	highlightController.addPrimaryHighlights(root, ops, hlColor);
	// }

	// public void setHighlightController(ClangHighlightController highlightController) {
	// 	if (this.highlightController != null) {
	// 		this.highlightController.removeListener(this);
	// 	}

	// 	this.highlightController = ClangHighlightController.dummyIfNull(highlightController);
	// 	highlightController.setHighlightColor(currentVariableHighlightColor);
	// 	highlightController.addListener(this);
	// }

	// public ClangHighlightController getHighlightController() {
	// 	return highlightController;
	// }

	// @Override
	// public void tokenHighlightsChanged() {
	// 	repaint();
	// }

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
		// Color hlColor = highlightController.getSecondaryHighlight(token);
		// if (hlColor == null) {
		// 	return; // not highlighted
		// }

		// highlightController.removeSecondaryHighlights(token);

		// // Add the new highlighter when we have rebuilt the token
		// controller.doWhenNotBusy(() -> {
		// 	addSecondaryHighlight(newName, t -> hlColor);
		// });
	}

	private void repairMiddleMouseSelectionForRename(EvtToken token, String newName) {
		// if (activeMiddleMouse == null || !activeMiddleMouse.matches(token)) {
		// 	return;
		// }

		// activeMiddleMouse.clear();
		// activeMiddleMouse = new ActiveMiddleMouse(newName);

		// // Apply the new middle-mouse highlighter when we have rebuilt the token
		// controller.doWhenNotBusy(() -> {
		// 	activeMiddleMouse.apply();
		// });
	}

	// private void cloneServiceHiglighters(EvtPanel sourcePanel) {

	// 	Set<DecompilerHighlighter> serviceHighlighters =
	// 		sourcePanel.highlightController.getServiceHighlighters();

	// 	for (DecompilerHighlighter otherHighlighter : serviceHighlighters) {

	// 		if (!(otherHighlighter instanceof ClangDecompilerHighlighter clangHighlighter)) {
	// 			continue;
	// 		}

	// 		DecompilerHighlighter newHighlighter = clangHighlighter.clone(this);
	// 		highlightersById.put(newHighlighter.getId(), newHighlighter);

	// 		TokenHighlights otherHighlighterTokens =
	// 			sourcePanel.highlightController.getHighlighterHighlights(otherHighlighter);
	// 		if (otherHighlighterTokens == null || otherHighlighterTokens.isEmpty()) {
	// 			// The highlighter has been created but no highlights have been applied.  It is up
	// 			// to the client to apply the highlights. The new highlighter will respond to the
	// 			// client request if the later apply the highlights.
	// 			continue;
	// 		}

	// 		newHighlighter.applyHighlights();
	// 	}
	// }

	/**
	 * Called by the provider to clone all highlights in the source panel and apply them to this
	 * panel
	 *
	 * @param sourcePanel the panel that was cloned
	 */
	// public void cloneHighlights(EvtPanel sourcePanel) {

	// 	Function function = decompileData.getData();
	// 	cloneServiceHiglighters(sourcePanel);

	// 	//
	// 	// Keep only those secondary highlighters for the current function.  This ensures that the
	// 	// clone will match the cloned decompiler.
	// 	//
	// 	Set<DecompilerHighlighter> secondaryHighlighters =
	// 		sourcePanel.getSecondaryHighlihgtersByFunction(function);

	// 	//
	// 	// We do NOT clone the secondary highlighters.  This allows the user the remove them
	// 	// from the primary provider without effecting the cloned provider and vice versa.
	// 	//
	// 	for (DecompilerHighlighter highlighter : secondaryHighlighters) {

	// 		if (!(highlighter instanceof ClangDecompilerHighlighter clangHighlighter)) {
	// 			continue;
	// 		}

	// 		DecompilerHighlighter newHighlighter = clangHighlighter.copy(this);
	// 		highlightersById.put(newHighlighter.getId(), newHighlighter);
	// 		applySecondaryHighlights(newHighlighter);
	// 	}
	// }

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
	void setEvtData(EvtData decompileData) {
		if (layoutController == null) {
			// we've been disposed!
			return;
		}

		EvtData oldData = this.decompileData;
		this.decompileData = decompileData;
		Address address = decompileData.getAddress();
		if (decompileData.hasDecompileResults()) {
			layoutController.buildLayouts(address, decompileData.getScript(), null, true);
		}
		else {
			layoutController.buildLayouts(null, null, decompileData.getErrorMessage(), true);
		}

		setLocation(oldData, decompileData);

		// decompilerHoverProvider.setProgram(decompileData.getProgram());

		// give user notice when seeing the decompile of a non-function
		setBackground(originalBackgroundColor);

		if (clipboard != null) {
			clipboard.selectionChanged(null);
		}

		// don't highlight search results across functions
		if (currentSearchResults != null) {
			currentSearchResults.decompilerUpdated();
			currentSearchResults = null;
		}

		// if (function != null) {
			// highlightController.reapplyAllHighlights(function);
		// }
	}

	private void setLocation(EvtData oldData, EvtData newData) {
		Address address = oldData.getAddress();
		if (SystemUtilities.isEqual(address, newData.getAddress())) {
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
				EvtUtils.getTokens(layoutController.getLines(), selection);
			fieldSelection = EvtUtils.getFieldSelection(tokens);
		}
		fieldPanel.setSelection(fieldSelection);
	}

	// public void setDecompilerHoverProvider(DecompilerHoverProvider provider) {
	// 	if (provider == null) {
	// 		throw new IllegalArgumentException("Cannot set the hover handler to null!");
	// 	}

	// 	if (decompilerHoverProvider != null) {
	// 		if (decompilerHoverProvider.isShowing()) {
	// 			decompilerHoverProvider.closeHover();
	// 		}
	// 		decompilerHoverProvider.initializeListingHoverHandler(provider);
	// 		decompilerHoverProvider.dispose();
	// 	}
	// 	decompilerHoverProvider = provider;
	// }

	public void dispose() {
		setEvtData(new EmptyEvtData("Disposed"));
		layoutController = null;
		// decompilerHoverProvider.dispose();
		// highlighCursorUpdater.dispose();
		// highlightController.dispose();
		// highlightersById.clear();
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
		if (!decompileData.hasDecompileResults()) {
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

		// if (buttonState == middleMouseHighlightButton && clickCount == 1) {
			// toggleMiddleMouseHighlight(location, field);
		// }
	}

	private void tryToGoto(FieldLocation location, Field field, MouseEvent event,
			boolean newWindow) {
		if (!navigationEnabled) {
			return;
		}

		EvtTextField textField = (EvtTextField) field;
		EvtToken token = textField.getToken(location);
		tryGoToLabel(token, newWindow);
		// if (token instanceof ClangFuncNameToken) {
		// 	tryGoToFunction((ClangFuncNameToken) token, newWindow);
		// }
		// else if (token instanceof ClangLabelToken) {
		// 	tryGoToLabel((ClangLabelToken) token, newWindow);
		// }
		// else if (token instanceof ClangVariableToken) {
		// 	tryGoToVarnode((ClangVariableToken) token, newWindow);
		// }
		// else if (token instanceof ClangCommentToken) {
		// 	tryGoToComment(location, event, textField, newWindow);
		// }
		// else if (token instanceof ClangSyntaxToken) {
		// 	tryGoToSyntaxToken((ClangSyntaxToken) token);
		// }
	}

	// private void tryGoToComment(FieldLocation location, MouseEvent event, ClangTextField textField,
	// 		boolean newWindow) {

	// 	// comments may use annotations; tell the annotation it was clicked
	// 	FieldElement clickedElement = textField.getClickedObject(location);
	// 	if (clickedElement instanceof AnnotatedTextFieldElement) {
	// 		AnnotatedTextFieldElement annotation = (AnnotatedTextFieldElement) clickedElement;
	// 		controller.annotationClicked(annotation, event, newWindow);
	// 		return;
	// 	}

	// 	String text = textField.getText();
	// 	String word = StringUtilities.findWord(text, location.col);
	// 	tryGoToScalar(word, newWindow);
	// }

	// private void tryGoToFunction(ClangFuncNameToken functionToken, boolean newWindow) {
	// 	Function function = EvtUtils.getFunction(controller.getProgram(), functionToken);
	// 	if (function != null) {
	// 		controller.goToFunction(function, newWindow);
	// 		return;
	// 	}
	// }

	private void tryGoToLabel(EvtToken token, boolean newWindow) {
		// check for a goto label
		// ClangLabelToken destination = EvtUtils.getGoToTargetToken(root, token);
		// if (destination != null) {
		// 	goToToken(destination);
		// 	return;
		// }

		Address addr = token.getMinAddress();
		controller.goToAddress(addr, newWindow);
	}

	// private void tryGoToSyntaxToken(ClangSyntaxToken token) {

	// 	if (EvtUtils.isBrace(token)) {
	// 		ClangSyntaxToken otherBrace = EvtUtils.getMatchingBrace(token);
	// 		if (otherBrace != null) {
	// 			goToToken(otherBrace);
	// 		}
	// 	}
	// }

	// private void tryGoToVarnode(ClangVariableToken token, boolean newWindow) {
	// 	Varnode vn = token.getVarnode();
	// 	if (vn == null) {
	// 		PcodeOp op = token.getPcodeOp();
	// 		if (op == null) {
	// 			return;
	// 		}
	// 		int operation = op.getOpcode();
	// 		if (!(operation == PcodeOp.PTRSUB || operation == PcodeOp.PTRADD)) {
	// 			return;
	// 		}
	// 		vn = op.getInput(1);
	// 		if (vn == null) {
	// 			return;
	// 		}

	// 	}
	// 	HighVariable highVar = vn.getHigh();
	// 	if (highVar instanceof HighGlobal) {
	// 		vn = highVar.getRepresentative();
	// 	}
	// 	if (vn.isAddress()) {
	// 		Address addr = vn.getAddress();
	// 		if (addr.isMemoryAddress()) {
	// 			controller.goToAddress(vn.getAddress(), newWindow);
	// 		}
	// 	}
	// 	else if (vn.isConstant()) {
	// 		controller.goToScalar(vn.getOffset(), newWindow);
	// 	}
	// }

	// private void tryGoToScalar(String text, boolean newWindow) {
	// 	if (text.startsWith("0x")) {
	// 		text = text.substring(2);
	// 	}
	// 	else if (text.startsWith("(") && text.endsWith(")")) {
	// 		int commaIx = text.indexOf(",0x");
	// 		if (commaIx < 2) {
	// 			return;
	// 		}
	// 		String spaceName = text.substring(1, commaIx);
	// 		String offsetStr = text.substring(commaIx + 3, text.length() - 1);
	// 		try {
	// 			AddressSpace space =
	// 				decompileData.getProgram().getAddressFactory().getAddressSpace(spaceName);
	// 			if (space == null) {
	// 				return;
	// 			}
	// 			Address addr = space.getAddress(NumericUtilities.parseHexLong(offsetStr), true);
	// 			controller.goToAddress(addr, newWindow);
	// 		}
	// 		catch (AddressOutOfBoundsException e) {
	// 			// give-up
	// 		}
	// 		return;
	// 	}
	// 	try {
	// 		long value = NumericUtilities.parseHexLong(text);
	// 		controller.goToScalar(value, newWindow);
	// 	}
	// 	catch (NumberFormatException e) {
	// 		return; // give up
	// 	}
	// }

	Program getProgram() {
		return decompileData.getProgram();
	}

	public ProgramLocation getCurrentLocation() {
		if (!decompileData.hasDecompileResults()) {
			return null;
		}
		Field currentField = fieldPanel.getCurrentField();
		FieldLocation cursorPosition = fieldPanel.getCursorLocation();
		return getProgramLocation(currentField, cursorPosition);
	}

	@Override
	public void fieldLocationChanged(FieldLocation location, Field field, EventTrigger trigger) {
		if (!decompileData.hasDecompileResults()) {
			return;
		}

		// pendingHighlightUpdate = new PendingHighlightUpdate(location, field, trigger);
		// highlighCursorUpdater.update();

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
		if (!decompileData.hasDecompileResults()) {
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
		for (DecompilerMarginProvider element : marginProviders) {
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

		Address entryPoint = decompileData.getAddress();
		if (address == null) {
			address = entryPoint;
		}

		EvtResults results = decompileData.getDecompileResults();
		int lineNumber = location.getIndex().intValue();
		int charPos = location.col;
		EvtLocationInfo info =
			new EvtLocationInfo(entryPoint, results, token, lineNumber, charPos);
		Program program = decompileData.getProgram();

		return new DefaultEvtLocation(program, address, info);
	}

	public void clearSearchResults(DecompilerSearchResults searchResults) {
		if (currentSearchResults == searchResults) {
			currentSearchResults = null;
			repaint();
		}
	}

	public void setSearchResults(DecompilerSearchResults searchResults) {
		currentSearchResults = searchResults;

		if (currentSearchResults != null) {
			DecompilerSearchLocation location = currentSearchResults.getActiveLocation();
			if (location != null) {
				setCursorPosition(location.getFieldLocation());
			}
		}

		repaint();
	}

	public DecompilerSearchLocation getActiveSearchLocation() {
		if (currentSearchResults == null) {
			return null;
		}
		DecompilerSearchLocation location = currentSearchResults.getActiveLocation();
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

	// public String getHighlightedText() {
	// 	EvtToken token = highlightController.getHighlightedToken();
	// 	if (token == null) {
	// 		return null;
	// 	}
	// 	if (token instanceof ClangCommentToken) {
	// 		return null; // comments are not single words that get highlighted
	// 	}
	// 	return token.getText();
	// }

	public String getTextUnderCursor() {

		FieldLocation location = fieldPanel.getCursorLocation();
		EvtTextField textField = (EvtTextField) fieldPanel.getCurrentField();
		if (textField == null) {
			return null;
		}

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

	public void addHoverService(DecompilerHoverService hoverService) {
		// decompilerHoverProvider.addHoverService(hoverService);
	}

	public void removeHoverService(DecompilerHoverService hoverService) {
		// decompilerHoverProvider.removeHoverService(hoverService);
	}

	public void setHoverMode(boolean enabled) {
		// decompilerHoverProvider.setHoverEnabled(enabled);
		// if (enabled) {
		// 	fieldPanel.setHoverProvider(decompilerHoverProvider);
		// }
		// else {
		// 	fieldPanel.setHoverProvider(null);
		// }
	}

	// public boolean isHoverShowing() {
	// 	return decompilerHoverProvider.isShowing();
	// }

	public List<EvtToken> findTokensByName(String name) {
		List<EvtToken> tokens = new ArrayList<>();
		doFindTokensByName(tokens, layoutController.getLines(), name);
		return tokens;
	}

	private void doFindTokensByName(List<EvtToken> tokens, List<EvtLine> lines, String name) {
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

		// highlightController.setHighlightColor(currentVariableHighlightColor);

		// if (options.isDisplayLineNumbers()) {
		// 	if (lineNumbersMargin == null) {
		// 		addMarginProvider(lineNumbersMargin = new LineNumberDecompilerMarginProvider());
		// 	}
		// }
		// else {
		// 	if (lineNumbersMargin != null) {
		// 		removeMarginProvider(lineNumbersMargin);
		// 		lineNumbersMargin = null;
		// 	}
		// }

		// for (DecompilerMarginProvider element : marginProviders) {
		// 	element.setOptions(options);
		// }
	}

	// public void addMarginProvider(DecompilerMarginProvider provider) {
	// 	marginProviders.add(0, provider);
	// 	provider.setOptions(options);
	// 	provider.setProgram(getProgram(), layoutController, pixmap);
	// 	buildPanels();
	// }

	// public void removeMarginProvider(DecompilerMarginProvider provider) {
	// 	marginProviders.remove(provider);
	// 	buildPanels();
	// }

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
		for (DecompilerMarginProvider marginProvider : marginProviders) {
			leftPanel.add(marginProvider.getComponent());
		}
		return leftPanel;
	}

//==================================================================================================
// Inner Classes
//==================================================================================================

	// private class SearchHighlightFactory implements FieldHighlightFactory {

	// 	@Override
	// 	public Highlight[] createHighlights(Field field, String text, int cursorTextOffset) {
	// 		if (currentSearchResults == null) {
	// 			return new Highlight[0];
	// 		}

	// 		ClangTextField cField = (ClangTextField) field;
	// 		int lineNumber = cField.getLineNumber();
	// 		Map<Integer, List<DecompilerSearchLocation>> locationsByLine =
	// 			currentSearchResults.getLocationsByLine();
	// 		List<DecompilerSearchLocation> locationsOnLine = locationsByLine.get(lineNumber);
	// 		if (locationsOnLine == null) {
	// 			return new Highlight[0];
	// 		}

	// 		DecompilerSearchLocation activeLocation = currentSearchResults.getActiveLocation();
	// 		List<Highlight> highlights = new ArrayList<>();
	// 		for (DecompilerSearchLocation location : locationsOnLine) {
	// 			Color c =
	// 				location == activeLocation ? activeSearchHighlightColor : searchHighlightColor;
	// 			int start = location.getStartIndexInclusive();
	// 			int end = location.getEndIndexInclusive();
	// 			highlights.add(new Highlight(start, end, c));
	// 		}

	// 		return highlights.toArray(Highlight[]::new);
	// 	}
	// }

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
}
