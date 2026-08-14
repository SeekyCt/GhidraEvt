package ghidraevt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

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
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import resources.Icons;
import ghidra.framework.Application;

public class GhidraEvtProvider extends ComponentProvider {

    private JPanel panel;
	private JTextArea textArea;
	private Program currentProgram;
	private ProgramLocation currentLocation;
    private IpcServer ipcServer;
    private Thread ipcThread;

    public GhidraEvtProvider(Plugin plugin, String owner) {
        super(plugin.getTool(), "Evt Disassembler", owner);

        buildPanel();
        // createActions();
        ipcServer = new IpcServer();
        ipcThread = new Thread(ipcServer, "GhidraEvtProvider-IPC");
        ipcThread.start();
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

    private String tryDisasm()
    {
        try {
            String path = Application.getOSFile("evt-rs").getAbsolutePath();

            Process disasm = new ProcessBuilder(
                path,
                "network",
                InetAddress.getLoopbackAddress().getHostAddress().toString(),
                Integer.toString(this.ipcServer.getPort())
            ).start();

            String errors = new String(
                disasm.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            if (errors.length() > 0)
                Msg.warn(this, "evt-rs printed to stderr: " + errors);

            return new String(
                disasm.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
        catch (InterruptedException e) {
            return "Disassembler connection failed: " + e.getMessage();
        }
        catch (FileNotFoundException e) {
            return "Disassembler failed: " + e.getMessage();
        }
        catch (IOException e) {
            return "Disassembler failed: " + e.getMessage();
        }
    }

	public void locationChanged(Program program, ProgramLocation location)
	{
		this.currentProgram = program;
		this.currentLocation = location;

        Msg.info(this, "-> tryDisasm");
        String text = tryDisasm();
        textArea.setText(text);
	}
}
