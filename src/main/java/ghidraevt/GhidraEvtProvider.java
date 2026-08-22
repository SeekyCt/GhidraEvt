package ghidraevt;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import docking.ComponentProvider;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.Highlight;
import docking.widgets.indexedscrollpane.IndexedScrollPane;
import ghidra.framework.plugintool.*;
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

public class GhidraEvtProvider extends ComponentProvider {
    private JPanel panel;
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

    private static class EvtHighlightFactory implements FieldHighlightFactory {

        @Override
        public Highlight[] createHighlights(Field field, String text, int cursorTextOffset) {
            return new Highlight[0];
            // if (currentSearchResults == null) {
            //     return new Highlight[0];
            // }

            // ClangTextField cField = (ClangTextField) field;
            // int lineNumber = cField.getLineNumber();
            // Map<Integer, List<DecompilerSearchLocation>> locationsByLine =
            //     currentSearchResults.getLocationsByLine();
            // List<DecompilerSearchLocation> locationsOnLine = locationsByLine.get(lineNumber);
            // if (locationsOnLine == null) {
            //     return new Highlight[0];
            // }

            // DecompilerSearchLocation activeLocation = currentSearchResults.getActiveLocation();
            // List<Highlight> highlights = new ArrayList<>();
            // for (DecompilerSearchLocation location : locationsOnLine) {
            //     Color c =
            //         location == activeLocation ? activeSearchHighlightColor : searchHighlightColor;
            //     int start = location.getStartIndexInclusive();
            //     int end = location.getEndIndexInclusive();
            //     highlights.add(new Highlight(start, end, c));
            // }

            // return highlights.toArray(Highlight[]::new);
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
            disasmCallback);
        strictMode.setEnabled(true);
        strictMode.markHelpUnnecessary();

        showAddresses = new DockingToggle(
            "Show Addresses",
            getOwner(),
            false,
            disasmCallback);
        showAddresses.setEnabled(true);
        showAddresses.markHelpUnnecessary();

        showLineNumbers = new DockingToggle(
            "Show Line Numbers",
            getOwner(),
            false,
            disasmCallback);
        showLineNumbers.setEnabled(true);
        showLineNumbers.markHelpUnnecessary();

        snapToSymbol = new DockingToggle(
            "Snap to Symbol",
            getOwner(),
            true,
            disasmCallback);
        snapToSymbol.setEnabled(true);
        snapToSymbol.markHelpUnnecessary();

        game = new DockingToggle(
            "Game",
            getOwner(),
            true,
            disasmCallback);
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

        if (snapToSymbol.enabled()) {
            SymbolIterator iter = program.getSymbolTable().getSymbolIterator(address, false);
            if (iter.hasNext())
                address = iter.next().getAddress();
        }

        MemoryInputStream stream = new MemoryInputStream(
            program.getMemory().getBlock(address),
            address);

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

        GhidraPrinter printer =
            new GhidraPrinter(program, showLineNumbers.enabled(), showAddresses.enabled());
        return printer.print_evt(address, script);
    }

    private void updateDisasm() {
        // textArea.setText(tryDisasm(currentLocation));
    }

    public void locationChanged(ProgramLocation location) {
        currentLocation = location;

        // updateDisasm();
    }
}
