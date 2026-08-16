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
import resources.Icons;
import ghidra.framework.Application;

public class GhidraEvtProvider extends ComponentProvider {
    private JPanel panel;
    private JTextArea textArea;
    private ProgramLocation currentLocation;

    // TODO: save settings

    private DockingToggle strictMode; // TODO: type-based mode
    private DockingToggle showAddresses;
    private DockingToggle showLineNumbers;

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

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

        dockingTool.addLocalAction(this, showLineNumbers);
        dockingTool.addLocalAction(this, showAddresses);
        dockingTool.addLocalAction(this, strictMode);
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    private String tryDisasm(ProgramLocation location) {
        if (location == null)
            return "No script selected.";

        Address address = location.getAddress();
        MemoryInputStream stream = new MemoryInputStream(
            location.getProgram().getMemory().getBlock(address),
            address
        );

        List<Instr> script;
        try {
            script = Instr.disassemble(Game.SPM, stream, strictMode.enabled());
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

        GhidraPrinter printer = new GhidraPrinter(showLineNumbers.enabled(), showAddresses.enabled());
        return printer.print_evt(address, script);
    }

    private void updateDisasm()
    {
        textArea.setText(tryDisasm(currentLocation));
    }

    public void locationChanged(ProgramLocation location)
    {
        currentLocation = location;

        updateDisasm();
    }
}
