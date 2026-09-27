/* ###
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
 */
package ghidraevt.scripts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.MemBuffer;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBufferImpl;
import ghidra.util.Msg;
import jevt.BadEvtException;
import jevt.Game;
import jevt.Instr;
import jevt.analysis.EvtValidator;

public class FindEvts {
    record Results(Map<Class<?>, EvtValidator.Result> results, int size) {
        boolean isOk() {
            for (EvtValidator.Result result : results.values())
            {
                if (!result.isOk())
                    return false;
            }
            return true;
        }

        // List<String> getErrors() {
        //     List<String> ret = new ArrayList<>();
        //     for (EvtValidator.Result result : results.values()) {
        //         if (!result.isOk())
        //             ret.add(result.getError());
        //     }
        //     return ret;
        // }
    }

    private Results tryAddr(Program program, Address startAddress, Game game) {
        MemBuffer buffer = new MemoryBufferImpl(program.getMemory(), startAddress);
        InputStream stream = buffer.getInputStream();
        List<Instr> script;

        try {
            script = Instr.disassemble(game, stream, false);
        }
        catch (BadEvtException | IOException e) {
            return null;
        }

        Map<Class<?>, EvtValidator.Result> results = new HashMap<>();
        for (EvtValidator validator : EvtValidator.VALIDATORS)
            results.put(validator.getClass(), validator.checkScript(script));

        return new Results(results, Instr.bytesSize(script));
    }

    public void run(Program currentProgram, Game game, File path) {
        try (CSVPrinter out = new CSVPrinter(new FileWriter(path), CSVFormat.DEFAULT)) {
            Object[] header = new String[EvtValidator.VALIDATORS.length + 2];
            {
                int i = 0;
                header[i++] = "Address";
                header[i++] = "Size";
                for (EvtValidator validator : EvtValidator.VALIDATORS) {
                    header[i++] = validator.getClass().getName();                
                }
            }
            out.printRecord(header);

            for (MemoryBlock block : currentProgram.getMemory().getBlocks()) {
                if (block.isInitialized() && !block.isExecute()) {
                    Msg.info(this, "Process block " + block.getName());

                    for (Address p = block.getStart(); p.compareTo(block.getEnd()) < 0; p = p.add(4)) {
                        Results res = tryAddr(currentProgram, p, game);
                        if (res != null) {
                            Object[] rec = new Object[EvtValidator.VALIDATORS.length + 2];
                            int i = 0;
                            rec[i++] = Long.toHexString(p.getOffset());
                            rec[i++] = res.size;
                            for (EvtValidator validator : EvtValidator.VALIDATORS) {
                                EvtValidator.Result result = res.results().get(validator.getClass());
                                rec[i++] = result.isOk() ? "" : result.getError();
                            }
                            out.printRecord(rec);
                        }
                    }
                }
            }

            Msg.info(this, "yeah eahoufhe");
        }
        catch (IOException e) {

        }
    }
}
