package ghidraevt;

import java.io.IOException;
import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;

/**
 * Handles interaction with jevt
 */
public class EvtManager {

    private EvtOptions options;

    private boolean snapToSymbol = true;
    private Game game = Game.SPM;
    private boolean strictMode = false;

    public EvtManager(EvtOptions options) {
        this.options = options;
    }

    void setOptions(EvtOptions options) {
        this.options = options;
    }

    DisassembleResults disassemble(Program program, ProgramLocation location,
            ViewerPosition viewerPosition) {
        if (location == null)
            return DisassembleResults.empty("No script selected.");

        Address address = location.getAddress();

        // Align to 4 bytes
        long offset = address.getOffset() & 3;
        address = address.subtract(offset);

        if (snapToSymbol) {
            CodeUnit cu = program.getListing().getCodeUnitContaining(address);
            if (cu != null && (cu.getAddress().getOffset() & 3) == 0)
                address = cu.getAddress();
        }

        EvtScript script = new EvtScript(address);

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
            return DisassembleResults.fail(script,
                "Unhandled disassembler exception: " + e.getMessage());
        }

        return DisassembleResults.success(script, docroot);
    }
}
