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
import jevt.Arg;
import jevt.Instr;

public class CMacroGhidraPrinter extends GhidraPrinter {
    protected CMacroGhidraPrinter(Program program, SymbolInspector symbolInspector, EvtOptions options,
            EvtScript script, List<Instr> docroot) {
        super(program, symbolInspector, options, script, docroot);
    }


    @Override
    protected int getMinIndent() {
        return 1;
    }

    private EvtToken openBracket() {
        return EvtToken.syntax(script, "(", decompileOptions.getDefaultColor(), currentAddr);
    }

    private EvtToken closeBracket() {
        return EvtToken.syntax(script, ")", decompileOptions.getDefaultColor(), currentAddr);
    }

    private EvtToken emptyBrackets() {
        return EvtToken.syntax(script, "()", decompileOptions.getDefaultColor(), currentAddr);
    }

    @Override
    protected void buildHeader() {
        List<EvtToken> header = new ArrayList<>();
        header.add(EvtToken.syntax(script, "EVT_BEGIN", COLOR_INSTR, currentAddr));
        header.add(openBracket());
        header.addAll(symbolToTokens(script, currentAddr, COLOR_HEADER, currentAddr, 0));
        header.add(closeBracket());
        doc.addLine(new EvtLine(header, currentAddr, 0, 0));
    }

    @Override
    protected void startInstr(Instr instr, List<EvtToken> tokens) {
        tokens.add(
            EvtToken.instr(script, instr.opcode().prettyName(), COLOR_INSTR, currentAddr)
        );
        tokens.add(openBracket());
    }

    @Override
    protected void buildArg(boolean first, Arg arg, List<EvtToken> tokens) {
        if (!first)
            tokens.add(EvtToken.syntax(script, ", ", decompileOptions.getDefaultColor(), currentAddr));
        tokens.addAll(argToTokens(script, arg, currentAddr));
    }

    @Override
    protected void endInstr(Instr instr, List<EvtToken> tokens) {
        tokens.add(closeBracket());
    }

    @Override
    protected void buildFooter() {
        List<EvtToken> footer = new ArrayList<>();
        footer.add(EvtToken.syntax(script, "EVT_END", COLOR_INSTR, currentAddr));
        footer.add(emptyBrackets());
        doc.addLine(new EvtLine(footer, currentAddr, displayLine++, 0));
    }
}
