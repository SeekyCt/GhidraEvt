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
package ghidraevt.token;

import java.util.ArrayList;
import java.util.List;

import ghidra.app.util.SymbolInspector;
import ghidra.program.model.listing.Program;
import ghidraevt.component.EvtOptions;
import ghidraevt.component.EvtScript;
import jevt.Instr;

public class PrettyGhidraPrinter extends GhidraPrinter {
    protected PrettyGhidraPrinter(Program program, SymbolInspector symbolInspector,
            EvtOptions decompileOptions, EvtScript script, List<Instr> docroot) {
        super(program, symbolInspector, decompileOptions, script, docroot);
    }

    @Override
    protected int getMinIndent() {
        return 0;
    }

    private static final String HEADER_DECORATION = "==========";

    @Override
    protected void buildHeader() {
        List<EvtToken> header = new ArrayList<>();
        header.add(EvtToken.syntax(script, HEADER_DECORATION + " ", decompileOptions.getDefaultColor(), currentAddr));
        header.addAll(symbolToTokens(script, currentAddr, COLOR_HEADER, currentAddr, 0));
        header.add(EvtToken.syntax(script, " " + HEADER_DECORATION, decompileOptions.getDefaultColor(), currentAddr));
        doc.addLine(new EvtLine(header, currentAddr, 0, 0));
    }

    @Override
    protected void startInstr(Instr instr, List<EvtToken> tokens) {
        tokens.add(
            EvtToken.instr(script, instr.opcode().prettyName(), COLOR_INSTR, currentAddr)
        );
    }

    @Override
    protected void buildArgSeparator(boolean first, List<EvtToken> tokens) {
        String sep = first ? " " : ", ";
        tokens.add(EvtToken.syntax(script, sep, decompileOptions.getDefaultColor(), currentAddr));
    }

    @Override
    protected void endInstr(Instr instr, List<EvtToken> tokens) {
    }

    @Override
    protected void buildFooter() {
    }    
}
