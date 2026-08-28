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
 * Modified from Ghidra's decompiler UI source code to work on evt scripts
 */
package ghidraevt.component;

import java.util.List;

import jevt.Instr;

/**
 * Class for getting at the various structures returned by the disassembler. 
 * 
 * To check if disassembly completed normally use the disassembleCompleted method. If this returns
 * false, the getErrorMessage method may contain a useful error message. Its also possible that
 * getErrorMessage will return warning messages, even if disassembly did complete.
 */
public class DisassembleResults {
    private EvtScript script; // Script to which results pertain
    private String errMsg; // Error message

    private List<Instr> docroot;

    private DisassembleResults(EvtScript script, List<Instr> docroot, String e) {
        this.script = script;
        this.docroot = docroot;
        this.errMsg = e;
    }

    public static DisassembleResults success(EvtScript script, List<Instr> docroot) {
        return new DisassembleResults(script, docroot, null);
    }

    public static DisassembleResults fail(EvtScript script, String message) {
        return new DisassembleResults(script, null, message);
    }

    public static DisassembleResults empty(String message) {
        return new DisassembleResults(null, null, message);
    }

    /**
     * Returns true if the disassembly producing these results completed without aborting.  If it
     * was aborted, there will be no real results in this object, and an error message should be
     * available via getErrorMessage.
     * @return true if the disassembly completed.
     */
    public boolean decompileCompleted() {
        return docroot != null;
    }

    public EvtScript getScript() {
        return script;
    }

    /** 
     * Returns true if the disassembly completed normally
     * @return true if the disassembly completed normally
     */
    public boolean isValid() {
        return errMsg == null;
    }

    /**
     * Return any error message associated with the disassembly producing these results. Generally,
     * there will only be an error if the disassembly was aborted for some reason, but there could
     * conceivably be warnings obtainable via this method, even if the decompilation did complete.
     * @return any error message associated with these results
     */
    public String getErrorMessage() {
        return errMsg;
    }

    public List<Instr> getDocroot() {
        return docroot;
    }

    public int bytesSize() {
        return Instr.bytesSize(docroot);
    }
}
