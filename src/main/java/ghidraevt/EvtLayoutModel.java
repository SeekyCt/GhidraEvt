package ghidraevt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import docking.widgets.fieldpanel.Layout;
import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.field.AttributedString;
import docking.widgets.fieldpanel.field.CompositeFieldElement;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.TextFieldElement;
import docking.widgets.fieldpanel.field.WrappingVerticalLayoutTextField;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.SingleRowLayout;
import generic.theme.Gui;
import ghidra.app.util.SymbolInspector;
import ghidra.app.util.viewer.field.CommentUtils;
import ghidra.framework.plugintool.ServiceProvider;
import ghidra.util.Msg;

public class EvtLayoutModel implements LayoutModel {
    private int maxWidth;
    private int maxLines = 30;
    private int indentWidth;
    private SymbolInspector symbolInspector;
    // private DecompileOptions options;
    // private ClangTokenGroup docroot; // Root of displayed document
    private Field[] fieldList; // Array of fields comprising layout
    private FontMetrics metrics;
    private FieldHighlightFactory hlFactory;
    private List<LayoutModelListener> listeners = new ArrayList<>();
    private BigInteger numIndexes = BigInteger.ZERO;

    private boolean showLineNumbers = true;


    public EvtLayoutModel(JPanel evtPanel, ServiceProvider serviceProvider, FieldHighlightFactory hlFactory) {
        this.hlFactory = hlFactory;

		symbolInspector = new SymbolInspector(serviceProvider, evtPanel);
        buildLayouts(EvtData.empty("No script selected."));
    }

    private static class EvtTextField extends WrappingVerticalLayoutTextField {
        public EvtTextField(FieldElement textElement, int startX, int width, int maxLines,
                FieldHighlightFactory hlFactory) {
            // super(createSingleLineElement(fieldElements), x, width - x, 30, hlFactory, false, "");

            super(textElement, startX, width, maxLines, hlFactory);
        }
    }

	@SuppressWarnings("deprecation")
	// ignoring the deprecated call for toolkit
    public void buildLayouts(EvtData data) {
		Font font = Gui.getFont("font.decompiler");
		metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
		indentWidth = metrics.stringWidth(" ");
		maxWidth = indentWidth * 100;

        if (data.isError()) {
            TextFieldElement element = new TextFieldElement(
                new AttributedString(data.getErrorMessage(), new Color(0xff8080ff), metrics),
                0, 0
            );
            fieldList = new Field[1];
            fieldList[0] = new EvtTextField(element, 0, maxWidth, maxLines, hlFactory);
        }
        else {
            GhidraPrinter printer = new GhidraPrinter(data.getProgram(), symbolInspector); // TODO
            List<EvtLine> lines = printer.getLines(data.getStartAddress(), data.getScript());
            
            int lineCount = lines.size();
            fieldList = new Field[lineCount]; // One field for each "C" line
                
            for (int i = 0; i < lineCount; ++i) {
                fieldList[i] = createTextFieldForLine(lines.get(i), lineCount, showLineNumbers);
            }
        }
        
        numIndexes = BigInteger.valueOf(fieldList.length);
        modelChanged(); // Inform the listeners that we have changed
    }

    private EvtTextField createTextFieldForLine(EvtLine line, int lineCount,
            boolean paintLineNumbers) {
        FieldElement[] elements = createFieldElementsForLine(line.tokens());
        CompositeFieldElement element = new CompositeFieldElement(elements);

        int indent = line.indent() * 4 * indentWidth;
        return new EvtTextField(element, indent, maxWidth, maxLines,
            hlFactory);
    }

    private FieldElement[] createFieldElementsForLine(List<EvtToken> tokens) {
        FieldElement[] elements = new FieldElement[tokens.size()];
        int columnPosition = 0;

        for (int i = 0; i < tokens.size(); ++i) {
            EvtToken token = tokens.get(i);
            Color color = token.getColor();

            AttributedString as = new AttributedString(token.getText(), color, metrics);
            // elements[i] = new ClangFieldElement(hlController, token, as, columnPosition);
            elements[i] = new TextFieldElement(as, 0, columnPosition);
            columnPosition += as.length();
        }

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
