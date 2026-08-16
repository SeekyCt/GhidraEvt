package ghidraevt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang3.ArrayUtils;

import docking.ActionContext;
import docking.ComponentProvider;
import docking.WindowPosition;
import docking.action.DockingAction;
import docking.action.ToolBarData;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;
import jevt.Printer;
import resources.Icons;
import ghidra.framework.Application;

public class GhidraEvtProvider extends ComponentProvider {
    private JPanel panel;
	private JTextArea textArea;
	private ProgramLocation currentLocation;

    private boolean strictMode;
    private DockingAction toggleStrict;

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

        strictMode = false;

        buildPanel();
        createActions();
    }

    // Customize GUI
    private void buildPanel() {
        panel = new JPanel(new BorderLayout());
        textArea = new JTextArea(5, 25);
        textArea.setEditable(false);
        textArea.setText("No script selected.");
        panel.add(new JScrollPane(textArea));
        setVisible(true);
        setIcon(Icons.INFO_ICON);
    }
    
    void updateStrictModeAction() {
        String desc;
        Icon icon;
        if (strictMode) {
            icon = Icons.ERROR_ICON;
            desc = "Toggle Strict Mode (enabled)";
        }
        else {
            icon = Icons.ADD_ICON;
            desc = "Toggle Strict Mode (disabled";
        }

        toggleStrict.setDescription(desc);
        toggleStrict.setToolBarData(new ToolBarData(icon, null));

        updateDisasm();
    }

    private void createActions() {
        toggleStrict = new DockingAction("Toggle Strict Mode", getOwner()) {
            @Override
            public void actionPerformed(ActionContext context) {
                GhidraEvtProvider.this.strictMode = !GhidraEvtProvider.this.strictMode;
                updateStrictModeAction();
            }
        };

        // TODO: type-based mode
        updateStrictModeAction();
        toggleStrict.setEnabled(true);
        toggleStrict.markHelpUnnecessary();
        dockingTool.addLocalAction(this, toggleStrict);
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    private String tryDisasm(ProgramLocation location) {
        if (location == null)
            return "No code selected.";

        Address address = location.getAddress();
        MemoryInputStream stream = new MemoryInputStream(
            location.getProgram().getMemory().getBlock(address),
            address
        );

        List<Instr> script;
        try {
            script = Instr.disassemble(Game.SPM, stream, this.strictMode);
        }
        catch (BadEvtException e) {
            String err = "Script appears invalid: " + e.getMessage();
            if (e.strictOnly()) {
                err += "\n(Triggered by Strict mode)";
            }
            return err;
        }
        catch (IOException e) {
            return "Disassembler failed: " + e.getMessage();
        }

        Printer printer = new Printer();
        return printer.print_evt(script);
    }

    private void updateDisasm()
    {
        textArea.setText(tryDisasm(this.currentLocation));
    }

    public void locationChanged(ProgramLocation location)
	{
		this.currentLocation = location;

        updateDisasm();
	}
}
