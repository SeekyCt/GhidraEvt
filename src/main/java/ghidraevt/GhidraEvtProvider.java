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
	private Program currentProgram;
	private ProgramLocation currentLocation;

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

        buildPanel();
        // createActions();
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

    // // Customize actions
    // private void createActions() {
    //     action = new DockingAction("My Action", getOwner()) {
    //         @Override
    //         public void actionPerformed(ActionContext context) {
    //             Msg.showInfo(getClass(), panel, "Custom Action", "Hello!");
    //         }
    //     };
    //     action.setToolBarData(new ToolBarData(Icons.ADD_ICON, null));
    //     action.setEnabled(true);
    //     action.markHelpUnnecessary();
    //     dockingTool.addLocalAction(this, action);
    // }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    private String tryDisasm(Address address) {
        MemoryInputStream stream = new MemoryInputStream(
            currentProgram.getMemory().getBlock(address),
            address
        );

        List<Instr> script;
        try {
            script = Instr.disassemble(Game.SPM, stream);
        }
        catch (BadEvtException e) {
            return "Script appears invalid: " + e.getMessage();
        }
        catch (IOException e) {
            return "Disassembler failed: " + e.getMessage();
        }

        Printer printer = new Printer();
        return printer.print_evt(script);
    }

    public void locationChanged(Program program, ProgramLocation location)
	{
		this.currentProgram = program;
		this.currentLocation = location;

        if (location == null) {
            textArea.setText("No code selected.");
        }
        else {
            String text = tryDisasm(location.getAddress());
            textArea.setText(text);
        }

	}
}
