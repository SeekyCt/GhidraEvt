package ghidraevt;

import java.io.IOException;
import java.util.List;


import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;

/**
 * Manages the threading involved with dealing with the decompiler. It uses a simpler approach
 * than previous versions.  Currently, there is only one Runnable ever scheduled to the RunManager.
 * If a new Decompile request comes in while a decompile is in progress, the new request is
 * first checked to see if it going to result in the same function being decompile. If so, then the
 * location is updated and the current decompile is allowed to continue.  If the new request is
 * a new function or the "forceDecompile" option is on, then the current decompile is stopped
 * and a new one is scheduled.  A SwingUpdateManger is used to prevent lots of decompile requests
 * from coming to quickly.
 *
 */
public class EvtManager {

	private EvtController decompilerController;
	private EvtOptions options;

	// TODO: settings
	private boolean snapToSymbol = true;
	private Game game = Game.SPM;
	private boolean strictMode = false;

	public EvtManager(EvtController decompilerController, EvtOptions options) {
		this.decompilerController = decompilerController;
		this.options = options;
	}

	/**
	 * Set the decompiler options for future decompiles.
	 */
	void setOptions(EvtOptions options) {
		this.options = options;
    }

	/**
	 * Requests a new decompile be scheduled.  If a current decompile is already in progress,
	 * the new request is checked to see if represents the same function. If so, only the
	 * location of the current decompile is updated and the current decompile is allowed to continue.
	 * Otherwise a new DecompileRunnable is created and scheduled to run using the updateManager.
	 * When the updateMangers runs, it will stop any current decompiles and begin the new decompile.
	 * @param program The program containing the function to be decompiled.
	 * @param location the location in the program to be decompiled and positioned to.
	 * @param debugFile if non-null, creates decompile debug output to this file.
	 * @param forceDecompile true forces a new decompile to be scheduled even if the current job
	 * is the same function.
	 */
	DisassembleResults disassemble(Program program, ProgramLocation location, ViewerPosition viewerPosition) {
	    if (location == null)
            return DisassembleResults.empty("No script selected.");

        Address address = location.getAddress();
        if (snapToSymbol) {
            CodeUnit cu = program.getListing().getCodeUnitContaining(address);
            if (cu != null)
                address = cu.getAddress();
        }
        EvtScript script = new EvtScript(address);

		MemoryBlock block = program.getMemory().getBlock(address);
        MemoryInputStream stream = new MemoryInputStream(program, address);

        List<Instr> docroot;
        try {
            docroot = Instr.disassemble(game, stream, strictMode);
        }
        catch (BadEvtException e) {
            String err = "Script appears invalid: " + e.getMessage();
            if (e.strictOnly()) {
                err += "\n(Triggered by Strict mode)";
            }
            return DisassembleResults.fail(script, err);
        }
        catch (IOException e) {
            Msg.error(this, "Disassembler failed", e);
            return DisassembleResults.fail(script, "Disassembler failed: " + e.getMessage());
        }
        catch (Exception e) {
            Msg.error(this, "Unhandled disassembler exception", e);
            return DisassembleResults.fail(script, "Unhandled disassembler exception: " + e.getMessage());
        }

        return DisassembleResults.success(script, docroot);
	}
}
