package ghidraevt;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
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
    private boolean stopOnNextSymbol = true;

    public EvtManager(EvtOptions options) {
        this.options = options;
    }

    void setOptions(EvtOptions options) {
        this.options = options;
    }

    private Address snapToSymbol(Program program, Address address) {
        // Try containing data first
        Data data = program.getListing().getDataContaining(address);
        if (data != null && data.isDefined() && (data.getAddress().getOffset() & 3) == 0)
            return data.getAddress();

        // Try last symbol if not found
        SymbolIterator iter = program.getSymbolTable().getSymbolIterator(address, false);
        while (iter.hasNext()) {
            Symbol next = iter.next();
            if ((next.getAddress().getOffset() & 3) == 0) 
                return next.getAddress();
        }

        // Nothing to snap to
        return address;
    }

    private Symbol nextSymbol(Program program, Address address) {
        SymbolIterator iter = program.getSymbolTable().getSymbolIterator(address.add(1), true);
        if (iter.hasNext())
            return iter.next();
        else
            return null;
    }

    DisassembleResults disassemble(Program program, ProgramLocation location,
        ViewerPosition viewerPosition) {
        return disassemble(program, location, viewerPosition, this.snapToSymbol);
    }

    private DisassembleResults disassemble(Program program, ProgramLocation location,
        ViewerPosition viewerPosition, boolean snapToSymbol) {
        if (location == null)
            return DisassembleResults.empty("No script selected.");

        Address startAddress = location.getAddress();

        // Align to 4 bytes
        long offset = startAddress.getOffset() & 3;
        startAddress = startAddress.subtract(offset);

        if (snapToSymbol) {
            startAddress = snapToSymbol(program, startAddress);
        }

        EvtScript script = new EvtScript(startAddress);

        MemoryInputStream stream;
        if (stopOnNextSymbol) {
            Symbol next = nextSymbol(program, startAddress);
            stream = new MemoryInputStream(program, startAddress, next.getAddress());
        }
        else {
            stream = new MemoryInputStream(program, startAddress);
        }

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
        catch (EOFException e) {
            return DisassembleResults.fail(script, "Disassembler failed: reached start of next symbol");
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

        script.setEndAddress(
            startAddress.add(Instr.bytesSize(docroot))
        );

        // Ignore snapped script if it ends before the location
        if (!script.contains(location.getAddress()))
            return disassemble(program, location, viewerPosition, false);

        return DisassembleResults.success(script, docroot);
    }
}
