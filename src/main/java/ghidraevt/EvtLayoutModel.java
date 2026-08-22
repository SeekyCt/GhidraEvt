package ghidraevt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import docking.widgets.fieldpanel.field.CompositeFieldElement;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.field.AttributedString;
import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.TextFieldElement;
import docking.widgets.fieldpanel.field.WrappingVerticalLayoutTextField;
import docking.widgets.fieldpanel.Layout;
import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.SingleRowLayout;

public class EvtLayoutModel implements LayoutModel {
    private int maxWidth = 100;
    private int indentWidth;
    // private SymbolInspector symbolInspector;
    // private DecompileOptions options;
    // private ClangTokenGroup docroot; // Root of displayed document
    private Field[] fieldList; // Array of fields comprising layout
    private FontMetrics metrics;
    private FieldHighlightFactory hlFactory;
    private List<LayoutModelListener> listeners = new ArrayList<>();
    // private Color[] syntaxColor; // Foreground colors.
    private BigInteger numIndexes = BigInteger.ZERO;
    private List<EvtLine> lines = new ArrayList<>();

    private boolean showLineNumbers = true;

    public EvtLayoutModel(FieldHighlightFactory hlFactory, FontMetrics met) {
        // syntaxColor = new Color[ClangToken.MAX_COLOR];
        this.hlFactory = hlFactory;
        this.metrics = met;

        buildLayouts();
    }

    private static class EvtTextField extends WrappingVerticalLayoutTextField {
        public EvtTextField(FieldElement textElement, int startX, int width, int maxLines,
                FieldHighlightFactory hlFactory) {
            // super(createSingleLineElement(fieldElements), x, width - x, 30, hlFactory, false, "");

            super(textElement, startX, width, maxLines, hlFactory);
        }
    }

    public void buildLayouts() {
        // updateOptions();

        indentWidth = metrics.stringWidth(" ");

        // Assume docroot has been built.

        GhidraPrinter printer = new GhidraPrinter(null, true, true); // TODO
        lines = printer.getLines();

        int lineCount = lines.size();
        fieldList = new Field[lineCount]; // One field for each "C" line
        numIndexes = BigInteger.valueOf(lineCount);

        for (int i = 0; i < lineCount; ++i) {
            fieldList[i] = createTextFieldForLine(lines.get(i), lineCount, showLineNumbers);
        }

        modelChanged(); // Inform the listeners that we have changed
    }

    private EvtTextField createTextFieldForLine(EvtLine line, int lineCount,
            boolean paintLineNumbers) {
        // List<ClangToken> tokens = line.getAllTokens();

        FieldElement[] elements = createFieldElementsForLine();
        CompositeFieldElement element = new CompositeFieldElement(elements);

        int indent = line.getIndent() * indentWidth;
        return new EvtTextField(element, indent, maxWidth, 30,
            hlFactory);
    }

    private FieldElement[] createFieldElementsForLine() {

        // FieldElement[] elements = new FieldElement[tokens.size()];
        FieldElement[] elements = new FieldElement[3];
        int columnPosition = 0;
        // Program program = decompilerPanel.getProgram();
        // ClangHighlightController hlController = decompilerPanel.getHighlightController();

        elements[0] = new TextFieldElement(
            new AttributedString("hello", new Color(0xffffffff), metrics),
            0, columnPosition);
        elements[1] = new TextFieldElement(
            new AttributedString("middle", new Color(0xffffffff), metrics),
            10, columnPosition);
        elements[2] = new TextFieldElement(
            new AttributedString("last", new Color(0xff00ffff), metrics),
            2, columnPosition);

        // for (int i = 0; i < tokens.size(); ++i) {
        //     ClangToken token = tokens.get(i);
        //     Color color = getTokenColor(token);

        //     if (token instanceof ClangCommentToken) {
        //         AttributedString prototype = new AttributedString("prototype", color, metrics);
        //         elements[i] =
        //             CommentUtils.parseTextForAnnotations(token.getText(), program, prototype, 0);
        //         columnPosition += elements[i].length();
        //     }
        //     else {
        //         AttributedString as = new AttributedString(token.getText(), color, metrics);
        //         elements[i] = new ClangFieldElement(hlController, token, as, columnPosition);
        //         columnPosition += as.length();
        //     }
        // }
        return elements;
    }

    /**
     * Adds a LayoutModelListener to be notified when changes occur.
     * @param listener the LayoutModelListener to add.
     */
    @Override
    public void addLayoutModelListener(LayoutModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a LayoutModelListener to be notified when changes occur.
     * @param listener the LayoutModelListener to remove.
     */
    @Override
    public void removeLayoutModelListener(LayoutModelListener listener) {
        listeners.remove(listener);
    }

    private void modelChanged() {
        for (LayoutModelListener listener : listeners)
            listener.modelSizeChanged(IndexMapper.IDENTITY_MAPPER);
    }

    /**
     * Returns true if the model knows about changes that haven't yet been told to the 
     * LayoutModelListeners.
     */
    @Override
    public void flushChanges() {
    }

    /**
     * Returns the closest larger index in the model that has a non-null layout.
     * @param index for which to find the next index with a non-null layout.
     * @return returns the closest larger index in the model that has a non-null layout.
     */
    @Override
    public BigInteger getIndexAfter(BigInteger index) {
        BigInteger nextIndex = index.add(BigInteger.ONE);
        if (nextIndex.compareTo(numIndexes) >= 0) {
            return null;
        }
        return nextIndex;
    }

    /**
     * Returns the closest smaller index in the model that has a non-null layout.
     * @param index for which to find the previous index with a non-null layout.
     * @return returns the closest smaller index in the model that has a non-null layout.
     */
    @Override
    public BigInteger getIndexBefore(BigInteger index) {
        if (index.compareTo(BigInteger.ZERO) <= 0) {
            return null;
        }
        return index.subtract(BigInteger.ONE);
    }

    /**
     * Returns a layout for the given index.
     * @param index the index of the layout to retrieve.
     */
    @Override
    public Layout getLayout(BigInteger index) {
        if (index.compareTo(numIndexes) >= 0) {
            return null;
        }
        return new SingleRowLayout(fieldList[index.intValue()]);
    }

    /**
     * Returns the total number of indexes.
     */
    @Override
    public BigInteger getNumIndexes() {
        return numIndexes;
    }

    /**
     * Returns the width of the largest possible layout.
     */
    @Override
    public Dimension getPreferredViewSize() {
        return new Dimension(maxWidth, 500);
    }

    /**
     * Returns true if every index returns a non-null layout and all the layouts
     * are the same height.
     */
    @Override
    public boolean isUniform() {
        return false;
    }
}
