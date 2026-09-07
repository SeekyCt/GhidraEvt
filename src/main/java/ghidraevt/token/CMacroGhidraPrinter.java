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
import java.util.Arrays;
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

    /*
        Disassembly is wrapped in a macro, everything inside should be indented
    */
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

    private EvtToken takePointer() {
        return EvtToken.syntax(script, "&", decompileOptions.getDefaultColor(), currentAddr);
    }

    /*
        Open with EVT_BEGIN macro
    */
    @Override
    protected void buildHeader() {
        List<EvtToken> header = new ArrayList<>();
        header.add(EvtToken.syntax(script, "EVT_BEGIN", COLOR_INSTR, currentAddr));
        header.add(openBracket());
        header.addAll(symbolToTokens(script, currentAddr, COLOR_HEADER, currentAddr, 0));
        header.add(closeBracket());
        doc.addLine(new EvtLine(header, currentAddr, 0, 0));
    }

    /*
        Print the instruction macro and open its bracket
    */
    @Override
    protected void startInstr(Instr instr, List<EvtToken> tokens) {
        tokens.add(
            EvtToken.instr(script, instr.opcode().macroName(), COLOR_INSTR, currentAddr)
        );
        tokens.add(openBracket());
    }

    /*
        Separate arguments with commas
    */
    @Override
    protected void buildArgSeparator(boolean first, List<EvtToken> tokens) {
        if (!first)
            tokens.add(EvtToken.syntax(script, ", ", decompileOptions.getDefaultColor(), currentAddr));
    }

    /*
        Close the bracket for the instruction macro
    */
    @Override
    protected void endInstr(Instr instr, List<EvtToken> tokens) {
        tokens.add(closeBracket());
    }

    /*
        End with EVT_END macro
    */
    @Override
    protected void buildFooter() {
        List<EvtToken> footer = new ArrayList<>();
        footer.add(EvtToken.syntax(script, "EVT_END", COLOR_INSTR, currentAddr));
        footer.add(emptyBrackets());
        doc.addLine(new EvtLine(footer, currentAddr, displayLine++, 0));
    }

    /*
        Wrap addresses in the PTR macro
    */
    @Override
    protected List<EvtToken> addrToTokens(EvtScript script, Instr instr, Arg.ADDR addr) {
        List<EvtToken> ret = new ArrayList<>(super.addrToTokens(script, instr, addr));

        // The USER_FUNC macro does not require PTR on its first argument
        if (instr.args().indexOf(addr) == 0)
            return ret;

        ret.add(0,
            new EvtToken(script, "PTR", decompileOptions.getDefaultColor(), currentAddr, displayLine)
        );
        ret.add(1, openBracket());

        // Functions and strings should not be prefixed with &
        if (!isString(addr) && !isFunction(addr))
            ret.add(2, takePointer());

        ret.add(closeBracket());
        return ret;
    }

    /*
        Wrap floats in the FLOAT macro
    */
    @Override
    protected List<EvtToken> floatToTokens(EvtScript script, Instr instr, float value) {
        List<EvtToken> ret = new ArrayList<>(super.floatToTokens(script, instr, value));
        ret.add(0,
            new EvtToken(script, "FLOAT", decompileOptions.getDefaultColor(), currentAddr, displayLine)
        );
        ret.add(1, openBracket());
        ret.add(closeBracket());
        return ret;
    }

    /*
        Represent none as the EVT_NULLPTR define
    */
    @Override
    protected List<EvtToken> noneToTokens(EvtScript script, Instr instr) {
        return Arrays.asList(EvtToken.arg(
            script,
            "EVT_NULLPTR",
            decompileOptions.getVariableColor(),
            currentAddr
        ));
    }
}
