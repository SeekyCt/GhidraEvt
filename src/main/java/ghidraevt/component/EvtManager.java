/* ###
 * IP: GHIDRA
 *
 * Copyright 2026 SeekyCt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified from ghidra/app/decompiler/component/DecompilerManager.java to work on evt scripts
 */
package ghidraevt.component;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemBuffer;
import ghidra.program.model.mem.MemoryBufferImpl;
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

    private EvtController controller;
    private EvtOptions options;

    private boolean snapToSymbol = true;
    private Game game = Game.SPM;
    private boolean strictMode = false;
    private boolean stopOnNextSymbol = true;

    public EvtManager(EvtController controller, EvtOptions options) {
        this.controller = controller;
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

    void disassemble(Program program, ProgramLocation location,
        ViewerPosition viewerPosition) {
        DisassembleResults results = doDisassemble(program, location, viewerPosition, this.snapToSymbol);
        controller.setDisasssembleData(new DisassembleData(
            program, results.getScript(), location, results, null, viewerPosition
        ));
    }

    private DisassembleResults doDisassemble(Program program, ProgramLocation location,
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

        EvtScript script = new EvtScript(program, startAddress);

        int sizeLimit = Integer.MAX_VALUE;
        if (stopOnNextSymbol) {
            Symbol next = nextSymbol(program, startAddress);
            if (next != null)
                sizeLimit = (int) next.getAddress().subtract(startAddress);
        }

        MemBuffer test = new MemoryBufferImpl(program.getMemory(), startAddress);
        InputStream stream = test.getInputStream(0, sizeLimit);

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
            return doDisassemble(program, location, viewerPosition, false);

        return DisassembleResults.success(script, docroot);
    }
}
