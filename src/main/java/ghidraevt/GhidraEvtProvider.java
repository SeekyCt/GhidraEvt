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
    // private IpcServer ipcServer;
    // private Thread ipcThread;

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

        buildPanel();
        // createActions();
        // ipcServer = new IpcServer();
        // ipcThread = new Thread(ipcServer, "GhidraEvtProvider-IPC");
        // ipcThread.start();
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

    // private String tryDisasm(long address)
    // {
    //     try {
    //         String path = Application.getOSFile("evt-rs").getAbsolutePath();

    //         Process disasm = new ProcessBuilder(
    //             path,
    //             "network",
    //             InetAddress.getLoopbackAddress().getHostAddress().toString(),
    //             Integer.toString(this.ipcServer.getPort()),
    //             Long.toHexString(address)
    //         ).start();

    //         String errors = new String(
    //             disasm.getErrorStream().readAllBytes(),
    //             StandardCharsets.UTF_8
    //         );
    //         if (errors.length() > 0)
    //             Msg.warn(this, "evt-rs printed to stderr: " + errors);

    //         return new String(
    //             disasm.getInputStream().readAllBytes(),
    //             StandardCharsets.UTF_8
    //         );
    //     }
    //     catch (InterruptedException e) {
    //         return "Disassembler connection failed: " + e.getMessage();
    //     }
    //     catch (FileNotFoundException e) {
    //         return "Disassembler failed: " + e.getMessage();
    //     }
    //     catch (IOException e) {
    //         return "Disassembler failed: " + e.getMessage();
    //     }
    // }

    private String tryDisasm(long address) {
        Address asAddress = currentProgram.getAddressFactory().getDefaultAddressSpace().getAddress(address);
        MemoryBlock block = currentProgram.getMemory().getBlock(asAddress);
        byte[] data = new byte[0x1000];
        try {
            int finalLength = block.getBytes(asAddress, data);
            data = ArrayUtils.subarray(data, 0, finalLength);
        }
        catch (MemoryAccessException e) {
            return "Memory read failed: " + e.getMessage();
        }

        InputStream stream = new ByteArrayInputStream(data);
        List<Instr> script;
        try {
            script = Instr.disassemble(Game.SPM, stream);
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
        // this.ipcServer.setCurrentProgram(program);

        // TODO: own thread
        Msg.info(this, "-> tryDisasm");
        String text = tryDisasm(location.getAddress().getOffset());
        textArea.setText(text);
	}
}
