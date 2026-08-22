package ghidraevt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang3.ArrayUtils;

import docking.ActionContext;
import docking.ComponentProvider;
import docking.WindowPosition;
import docking.action.DockingAction;
import docking.action.ToolBarData;
import docking.widgets.fieldpanel.field.CompositeFieldElement;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.field.AttributedString;
import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.TextFieldElement;
import docking.widgets.fieldpanel.field.WrappingVerticalLayoutTextField;
import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.Layout;
import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.Highlight;
import docking.widgets.fieldpanel.support.SingleRowLayout;
import docking.widgets.indexedscrollpane.IndexedScrollPane;
import generic.theme.Gui;
import ghidra.app.decompiler.ClangCommentToken;
import ghidra.app.decompiler.ClangLine;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.component.ClangFieldElement;
import ghidra.app.decompiler.component.ClangHighlightController;
import ghidra.app.decompiler.component.ClangTextField;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.app.plugin.core.decompile.actions.DecompilerSearchLocation;
import ghidra.app.util.viewer.field.CommentUtils;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;
import resources.Icons;
import ghidra.framework.Application;
import generic.theme.Gui;

public class GhidraEvtProvider extends ComponentProvider {
    private JPanel panel;
    private JTextArea textArea;
    private ProgramLocation currentLocation;

    // TODO: save settings

    private DockingToggle strictMode; // TODO: type-based mode
    private DockingToggle showAddresses;
    private DockingToggle showLineNumbers;
    private DockingToggle snapToSymbol;
    private DockingToggle game;

    private FontMetrics fm;

    private FieldPanel fieldPanel;

    private EvtHighlightFactory hlFactory = new EvtHighlightFactory();

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

        buildPanel();
        createActions();
    }

    private static class EvtTextField extends WrappingVerticalLayoutTextField {
        public EvtTextField(FieldElement textElement, int startX, int width, int maxLines, FieldHighlightFactory hlFactory) {
    		// super(createSingleLineElement(fieldElements), x, width - x, 30, hlFactory, false, "");

            super(textElement, startX, width, maxLines, hlFactory);
        }
    }

	private static class EvtHighlightFactory implements FieldHighlightFactory {

		@Override
		public Highlight[] createHighlights(Field field, String text, int cursorTextOffset) {
            return new Highlight[0];
			// if (currentSearchResults == null) {
			// 	return new Highlight[0];
			// }

			// ClangTextField cField = (ClangTextField) field;
			// int lineNumber = cField.getLineNumber();
			// Map<Integer, List<DecompilerSearchLocation>> locationsByLine =
			// 	currentSearchResults.getLocationsByLine();
			// List<DecompilerSearchLocation> locationsOnLine = locationsByLine.get(lineNumber);
			// if (locationsOnLine == null) {
			// 	return new Highlight[0];
			// }

			// DecompilerSearchLocation activeLocation = currentSearchResults.getActiveLocation();
			// List<Highlight> highlights = new ArrayList<>();
			// for (DecompilerSearchLocation location : locationsOnLine) {
			// 	Color c =
			// 		location == activeLocation ? activeSearchHighlightColor : searchHighlightColor;
			// 	int start = location.getStartIndexInclusive();
			// 	int end = location.getEndIndexInclusive();
			// 	highlights.add(new Highlight(start, end, c));
			// }

			// return highlights.toArray(Highlight[]::new);
		}
	}


    private static class EvtLayoutModel implements LayoutModel {
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
                0, columnPosition
            );
            elements[1] = new TextFieldElement(
                new AttributedString("middle", new Color(0xffffffff), metrics),
                10, columnPosition
            );
            elements[2] = new TextFieldElement(
                new AttributedString("last", new Color(0xff00ffff), metrics),
                2, columnPosition
            );

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

        private void modelChanged()
        {
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

    // Customize GUI
    private void buildPanel() {
        panel = new JPanel(new BorderLayout());
        panel.setName("Evt Master Panel");

        fm = panel.getFontMetrics(panel.getFont());
        EvtLayoutModel layout = new EvtLayoutModel(hlFactory, fm);
        fieldPanel = new FieldPanel(layout, "Evt Field Panel");

        IndexedScrollPane scrollPane = new IndexedScrollPane(fieldPanel);
        scrollPane.setName("Evt Scroll Pane");

        // textArea = new JTextArea(5, 25);
        // textArea.setEditable(false);
        // textArea.setText("No script selected.");
        panel.add(fieldPanel);
        setVisible(true);
        setIcon(Icons.INFO_ICON);
    }

    private void createActions() {
        Runnable disasmCallback = new Runnable() {
            @Override
            public void run() {
                updateDisasm();
            }
        };

        strictMode = new DockingToggle(
            "Strict Mode",
            getOwner(),
            false,
            disasmCallback
        );
        strictMode.setEnabled(true);
        strictMode.markHelpUnnecessary();

        showAddresses = new DockingToggle(
            "Show Addresses",
            getOwner(),
            false,
            disasmCallback
        );
        showAddresses.setEnabled(true);
        showAddresses.markHelpUnnecessary();

        showLineNumbers = new DockingToggle(
            "Show Line Numbers",
            getOwner(),
            false, 
            disasmCallback
        );
        showLineNumbers.setEnabled(true);
        showLineNumbers.markHelpUnnecessary();

        snapToSymbol = new DockingToggle(
            "Snap to Symbol",
            getOwner(),
            true, 
            disasmCallback
        );
        snapToSymbol.setEnabled(true);
        snapToSymbol.markHelpUnnecessary();

        game = new DockingToggle(
            "Game",
            getOwner(),
            true, 
            disasmCallback
        );
        game.setEnabled(true);
        game.markHelpUnnecessary();

        dockingTool.addLocalAction(this, showLineNumbers);
        dockingTool.addLocalAction(this, showAddresses);
        dockingTool.addLocalAction(this, strictMode);
        dockingTool.addLocalAction(this, snapToSymbol);
        dockingTool.addLocalAction(this, game);
    }

    private Game game() {
        if (game.enabled())
            return Game.SPM;
        else
            return Game.TTYD;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    private String tryDisasm(ProgramLocation location) {
        if (location == null)
            return "No script selected.";

        Program program = location.getProgram();
        Address address = location.getAddress();

        if (snapToSymbol.enabled())
        {
            SymbolIterator iter = program.getSymbolTable().getSymbolIterator(address, false);
            if (iter.hasNext())
                address = iter.next().getAddress();
        }

        MemoryInputStream stream = new MemoryInputStream(
            program.getMemory().getBlock(address),
            address
        );

        List<Instr> script;
        try {
            script = Instr.disassemble(game(), stream, strictMode.enabled());
        }
        catch (BadEvtException e) {
            String err = "Script appears invalid: " + e.getMessage();
            if (e.strictOnly()) {
                err += "\n(Triggered by Strict mode)";
            }
            return err;
        }
        catch (IOException e) {
            Msg.error(this, "Disassembler failed", e);
            return "Disassembler failed: " + e.getMessage();
        }
        catch (Exception e) {
            Msg.error(this, "Unhandled disassembler exception", e);
            return "Unhandled disassembler exception: " + e.getMessage();
        }

        GhidraPrinter printer = new GhidraPrinter(program, showLineNumbers.enabled(), showAddresses.enabled());
        return printer.print_evt(address, script);
    }

    private void updateDisasm()
    {
        // textArea.setText(tryDisasm(currentLocation));
    }

    public void locationChanged(ProgramLocation location)
    {
        currentLocation = location;

        // updateDisasm();
    }
}
