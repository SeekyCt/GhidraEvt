package ghidraevt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import docking.widgets.fieldpanel.Layout;
import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.field.AttributedString;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.TextFieldElement;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.fieldpanel.support.SingleRowLayout;
import ghidra.app.util.SymbolInspector;
import ghidra.framework.plugintool.ServiceProvider;
import ghidra.program.model.address.Address;
import jevt.Instr;

public class EvtLayoutModel implements LayoutModel, LayoutModelListener {
    private int maxWidth; // Max line width in pixels
    private int indentCharWidth; // Width of a space character in pixels
    private SymbolInspector symbolInspector;
    private EvtPanel evtPanel;
    private EvtOptions options;
    private Field[] fieldList; // Each field is a line of disassembly
    private FontMetrics metrics;
    private FieldHighlightFactory hlFactory;
    private List<LayoutModelListener> listeners;
    private BigInteger numIndexes = BigInteger.ZERO;
    private List<EvtLine> lines;

    private int maxLines = 30; // Maximum number of times 1 line of disassembly can be wrapped
    private int indentSize = 4; // Number of indentation characters to add per level

    private boolean showLineNumbers = true;

    public EvtLayoutModel(EvtOptions opt, EvtPanel evtPanel, FontMetrics met,
            FieldHighlightFactory hlFactory) {
        options = opt;
        this.evtPanel = evtPanel;
        metrics = met;
        this.hlFactory = hlFactory;
        listeners = new ArrayList<>();
        buildLayouts(null, null, null, false);

        ServiceProvider serviceProvider = evtPanel.getController().getServiceProvider();
        symbolInspector = new SymbolInspector(serviceProvider, evtPanel);
    }

    public List<EvtLine> getLines() {
        return lines;
    }

    @Override
    public boolean isUniform() {
        return false;
    }

    @Override
    public Dimension getPreferredViewSize() {
        return new Dimension(maxWidth, 500);
    }

    @Override
    public BigInteger getNumIndexes() {
        return numIndexes;
    }

    @Override
    public Layout getLayout(BigInteger index) {
        if (index.compareTo(numIndexes) >= 0) {
            return null;
        }
        return new SingleRowLayout(fieldList[index.intValue()]);
    }

    @Override
    public void addLayoutModelListener(LayoutModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLayoutModelListener(LayoutModelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void modelSizeChanged(IndexMapper mapper) {
        for (LayoutModelListener listener : listeners) {
            listener.modelSizeChanged(mapper);
        }
    }

    public void modelChanged() {
        for (LayoutModelListener listener : listeners) {
            listener.modelSizeChanged(IndexMapper.IDENTITY_MAPPER);
        }
    }

    @Override
    public void dataChanged(BigInteger start, BigInteger end) {
        for (LayoutModelListener listener : listeners) {
            listener.dataChanged(start, end);
        }
    }

    public void layoutChanged() {
        for (LayoutModelListener listener : listeners) {
            listener.dataChanged(BigInteger.ZERO, numIndexes);
        }
    }

    @Override
    public BigInteger getIndexAfter(BigInteger index) {
        BigInteger nextIndex = index.add(BigInteger.ONE);
        if (nextIndex.compareTo(numIndexes) >= 0) {
            return null;
        }
        return nextIndex;
    }

    @Override
    public BigInteger getIndexBefore(BigInteger index) {
        if (index.compareTo(BigInteger.ZERO) <= 0) {
            return null;
        }
        return index.subtract(BigInteger.ONE);
    }

    Field[] getFields() {
        return fieldList;
    }

    private EvtTextField createTextFieldForLine(EvtLine line) {
        List<EvtToken> tokens = line.getAllTokens();

        FieldElement[] elements = createFieldElementsForLine(tokens);

        int indent = line.getIndent() * indentSize * indentCharWidth;
        return new EvtTextField(tokens, elements, indent, line.getLineNumber(), maxWidth, maxLines,
            hlFactory);
    }

    private FieldElement[] createFieldElementsForLine(List<EvtToken> tokens) {

        FieldElement[] elements = new FieldElement[tokens.size()];
        int columnPosition = 0;
        for (int i = 0; i < tokens.size(); ++i) {
            EvtToken token = tokens.get(i);

            // if (token instanceof ClangCommentToken) {
            //     AttributedString prototype = new AttributedString("prototype", color, metrics);
            //     Program program = evtPanel.getProgram();
            //     elements[i] =
            //         CommentUtils.parseTextForAnnotations(token.getText(), program, prototype, 0);
            //     columnPosition += elements[i].length();
            // }

            AttributedString as = new AttributedString(token.getText(), token.getColor(), metrics);
            elements[i] = new TextFieldElement(as, 0, columnPosition);
            columnPosition += as.length();
        }
        return elements;
    }

    /**
     * Update to the current options
     */
    @SuppressWarnings("deprecation")
    // ignoring the deprecated call for toolkit
    private void updateOptions() {
        // setting the metrics here will indirectly trigger the new font to be used deeper in
        // the bowels of the FieldPanel (you can get the font from the metrics)
        metrics = Toolkit.getDefaultToolkit().getFontMetrics(options.getDefaultFont());
        indentCharWidth = metrics.stringWidth(GhidraPrinter.INDENT_CHAR);
        maxWidth = indentCharWidth * options.getMaxWidth();

        showLineNumbers = options.isDisplayLineNumbers();
    }

    private void splitToMaxWidthLines(List<String> res, String line) {
        int maxchar;
        if ((maxWidth == 0) || (indentCharWidth == 0)) {
            maxchar = 40;
        }
        else {
            maxchar = maxWidth / indentCharWidth;
        }
        String[] toklist = line.split("[ \t]+");
        StringBuilder buf = new StringBuilder();
        int cursize = 0;
        boolean atleastone = false;
        int i = 0;
        while (i < toklist.length) {
            if (!atleastone) {
                buf.append(' ');
                buf.append(toklist[i]);
                atleastone = true;
                cursize += toklist[i].length() + 1;
                i += 1;
                continue;
            }
            if (cursize + toklist[i].length() >= maxchar) {
                String finishLine = buf.toString();
                res.add(finishLine);
                cursize = 5;
                atleastone = false;
                buf = new StringBuilder();
                buf.append("     ");
            }
            else {
                buf.append(' ');
                buf.append(toklist[i]);
                cursize += toklist[i].length() + 1;
                i += 1;
            }
        }
        String finalLine = buf.toString();
        if (finalLine.length() != 0) {
            res.add(finalLine);
        }
    }

    private void addErrorLines(List<EvtLine> lines, String errmsg) { // Add indicated error message to display
        if (errmsg == null) {
            return; // No error message to add
        }
        String[] errlines_init = errmsg.split("[\n\r]+");
        List<String> errlines = new ArrayList<>();
        for (String element : errlines_init) {
            splitToMaxWidthLines(errlines, element);
        }
        int i = 0;
        for (String errline : errlines) {
            lines.add(0, new EvtLine(
                Arrays.asList(new EvtToken(errline, GhidraPrinter.COLOR_COMMENT, null, 0)),
                Address.NO_ADDRESS,
                i,
                0));
        }
    }

    public void buildLayouts(EvtScript script, List<Instr> docroot, String errmsg,
            boolean display) {
        updateOptions();

        if (docroot != null) {
            GhidraPrinter printer =
                new GhidraPrinter(evtPanel.getProgram(), symbolInspector, options);
            lines = printer.getLines(script, docroot);
        }
        else {
            lines = new ArrayList<>();
        }

        addErrorLines(lines, errmsg);

        int lineCount = lines.size();
        fieldList = new Field[lineCount]; // One field for each "C" line
        numIndexes = BigInteger.valueOf(lineCount);

        for (int i = 0; i < lineCount; ++i) {
            EvtLine oneLine = lines.get(i);
            fieldList[i] = createTextFieldForLine(oneLine);
        }

        if (display) {
            modelChanged(); // Inform the listeners that we have changed
        }

    }

    public void locationChanged(FieldLocation loc, Field field, Color locationColor,
            Color parenColor) {
        // Highlighting is now handled through the decompiler panel's highlight controller.
    }

    @Override
    public void flushChanges() {
        // nothing to do
    }
}
