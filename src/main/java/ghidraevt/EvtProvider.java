package ghidraevt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import docking.widgets.fieldpanel.field.Field;
import docking.WindowPosition;
import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.fieldpanel.support.FieldSelection;
import docking.widgets.fieldpanel.support.FieldSelectionHelper;
import docking.widgets.fieldpanel.support.Highlight;
import docking.widgets.indexedscrollpane.IndexedScrollPane;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.component.ClangTextField;
import ghidra.app.events.ProgramSelectionPluginEvent;
import ghidra.app.nav.LocationMemento;
import ghidra.app.plugin.core.decompile.DecompilePlugin;
import ghidra.app.services.ClipboardContentProviderService;
import ghidra.app.services.ClipboardService;
import ghidra.app.util.ListingHighlightProvider;
import ghidra.framework.plugintool.NavigatableComponentProviderAdapter;
import ghidra.framework.plugintool.Plugin;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;
import resources.Icons;

public class EvtProvider extends NavigatableComponentProviderAdapter {
    private JPanel panel;
    private FieldPanel fieldPanel;

    private ProgramLocation currentLocation;
	private ProgramSelection currentSelection;

    // TODO: save settings

    private DockingToggle strictMode; // TODO: type-based mode
    private DockingToggle showAddresses;
    private DockingToggle showLineNumbers;
    private DockingToggle snapToSymbol;
    private DockingToggle game;

    private EvtHighlightFactory hlFactory = new EvtHighlightFactory();
    private EvtLayoutModel layout;

    private ClipboardService clipboardService;

    private EvtClipboardProvider clipboardProvider;

	private final GhidraEvtPlugin plugin;

    public EvtProvider(GhidraEvtPlugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner, null);

        this.clipboardProvider = new EvtClipboardProvider(plugin, this); // TODO
        this.plugin = plugin;

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

        layout = new EvtLayoutModel(panel, getTool(), hlFactory);
        fieldPanel = new FieldPanel(layout, "Evt Field Panel");

        IndexedScrollPane scrollPane = new IndexedScrollPane(fieldPanel);
        scrollPane.setName("Evt Scroll Pane");

        panel.add(scrollPane);
        setIcon(Icons.INFO_ICON);
        setDefaultWindowPosition(WindowPosition.RIGHT);
        setVisible(true);
    }

	public EvtToken getTokenAtCursor() {
		FieldLocation cursorPosition = fieldPanel.getCursorLocation();
		Field field = fieldPanel.getCurrentField();
		if (field == null) {
			return null;
		}
		return ((EvtTextField) field).getToken(cursorPosition);
	}


    public String getCursorText() {
        EvtToken token = getTokenAtCursor();
		// ClangToken token = panel.getTokenAtCursor();
		// if (token == null) {
		// 	return null;
		// }

		// if (token instanceof ClangFuncNameToken functionToken) {
		// 	Function function = DecompilerUtils.getFunction(currentProgram, functionToken);
		// 	if (function != null) {
		// 		return function.getName();
		// 	}
		// }

		String text = token.getText();
		return text;        
    }


    public JPanel getPanel() {
        return panel;
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

    private EvtData tryDisasm(ProgramLocation location) {
        if (location == null)
            return EvtData.empty("No script selected.");

        Program program = location.getProgram();
        Address address = location.getAddress();
        
        if (snapToSymbol.enabled()) {
            CodeUnit cu = program.getListing().getCodeUnitContaining(address);
            if (cu != null)
                address = cu.getAddress();
        }
        Address startAddress = address;

        
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
            return new EvtData(program, startAddress, null, err);
        }
        catch (IOException e) {
            Msg.error(this, "Disassembler failed", e);
            return EvtData.fail(program, startAddress, "Disassembler failed: " + e.getMessage());
        }
        catch (Exception e) {
            Msg.error(this, "Unhandled disassembler exception", e);
            return EvtData.fail(program, startAddress, "Unhandled disassembler exception: " + e.getMessage());
        }

        return EvtData.success(program, startAddress, script);
    }

    private void updateDisasm() {
        EvtData data = tryDisasm(currentLocation);
        layout.buildLayouts(data);
    }

    public void locationChanged(ProgramLocation location) {
        currentLocation = location;

        updateDisasm();
    }

	void setClipboardService(ClipboardService service) {
		clipboardService = service;
		if (clipboardService != null) {
			clipboardService.registerClipboardContentProvider(clipboardProvider);
		}
	}

    @Override
	public void dispose() {
		super.dispose();

        // TODO

		if (clipboardService != null) {
			clipboardService.deRegisterClipboardContentProvider(clipboardProvider);
		}

		currentLocation = null;
        currentSelection = null;
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
    public ProgramLocation getLocation() {
        return currentLocation;
    }

    @Override
    public LocationMemento getMemento() {
        if (currentLocation == null)
            return null;
        return new LocationMemento(currentLocation.getProgram(), currentLocation);
    }

    @Override
    public Program getProgram() {
        if (currentLocation == null)
            return null;
        return currentLocation.getProgram();
    }

    @Override
    public String getTextSelection() {
		FieldSelection selection = fieldPanel.getSelection();
		if (selection.isEmpty()) {
			return null;
		}

		return FieldSelectionHelper.getFieldSelectionText(selection, fieldPanel);
	}

    @Override
	public boolean goTo(Program gotoProgram, ProgramLocation location) {
        plugin.locationChanged(location);
        return true;
	}

    @Override
    public void removeHighlightProvider(ListingHighlightProvider highlightProvider, Program p) {

    }

    @Override
    public void setHighlight(ProgramSelection highlight) {

    }

    @Override
    public void setHighlightProvider(ListingHighlightProvider highlightProvider, Program p) {

    }

    @Override
    public void setMemento(LocationMemento memento) {

    }

    @Override
    public void setSelection(ProgramSelection selection) {
        currentSelection = selection;
        tool.contextChanged(this);

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
    public boolean supportsHighlight() {
        return false;
    }

    public EvtLayoutModel getLayout() {
        return layout;
    }
}
