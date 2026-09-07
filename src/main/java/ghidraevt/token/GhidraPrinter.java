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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generic.theme.GColor;
import ghidra.app.util.SymbolInspector;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Namespace;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.Msg;
import ghidraevt.component.EvtOptions;
import ghidraevt.component.EvtScript;
import jevt.Arg;
import jevt.Instr;
import jevt.Opcode;

public abstract class GhidraPrinter {
    public static Color COLOR_LW      = new GColor("color.fg.ghidraevt.lw");
    public static Color COLOR_LF      = new GColor("color.fg.ghidraevt.lf");
    public static Color COLOR_LSW     = new GColor("color.fg.ghidraevt.lsw");
    public static Color COLOR_LSWF    = new GColor("color.fg.ghidraevt.lswf");
    public static Color COLOR_GW      = new GColor("color.fg.ghidraevt.gw");
    public static Color COLOR_GF      = new GColor("color.fg.ghidraevt.gf");
    public static Color COLOR_GSW     = new GColor("color.fg.ghidraevt.gsw");
    public static Color COLOR_GSWF    = new GColor("color.fg.ghidraevt.gswf");
    public static Color COLOR_UW      = new GColor("color.fg.ghidraevt.uw");
    public static Color COLOR_UF      = new GColor("color.fg.ghidraevt.uf");
    public static Color COLOR_INSTR   = new GColor("color.fg.ghidraevt.instr");
    public static Color COLOR_COMMENT = new GColor("color.fg.ghidraevt.comment");
    public static Color COLOR_HEADER  = new GColor("color.fg.ghidraevt.header");

    public static Color COLOR_EXTERNAL_FUNCTION = new GColor("color.fg.decompiler.external.function");

    // A single character of indentation
    public static String INDENT_CHAR = " ";

    protected Program program;
    protected SymbolInspector symbolInspector;
    protected EvtOptions decompileOptions;

    protected EvtScript script;
    protected List<Instr> docroot;
    protected int indent;
    protected int line;
    protected int displayLine;
    protected EvtDocument doc;
    protected Address currentAddr;

    protected GhidraPrinter(Program program, SymbolInspector symbolInspector,
        EvtOptions decompileOptions, EvtScript script, List<Instr> docroot) {
        this.program = program;
        this.symbolInspector = symbolInspector;
        this.decompileOptions = decompileOptions;
        this.script = script;
        this.docroot = docroot;

        this.indent = 0;
        this.line = 1;
        this.displayLine = 1;
        this.doc = new EvtDocument();
        this.currentAddr = script.getStartAddress();

        buildLines();
    }

    private Color variableToColor(Arg.Variable v) {
        return switch (v) {
            case Arg.UF(int id) -> COLOR_UF;
            case Arg.UW(int id) -> COLOR_UW;
            case Arg.GSW(int id) -> COLOR_GSW;
            case Arg.LSW(int id) -> COLOR_LSW;
            case Arg.GSWF(int id) -> COLOR_GSWF;
            case Arg.LSWF(int id) -> COLOR_LSWF;
            case Arg.GF(int id) -> COLOR_GF;
            case Arg.LF(int id) -> COLOR_LF;
            case Arg.GW(int id) -> COLOR_GW;
            case Arg.LW(int id) -> COLOR_LW;
        };
    }

    private Color getFunctionColor(Function function) {
        Symbol symbol = function.getSymbol();

        if (function.isExternal()) {
            return COLOR_EXTERNAL_FUNCTION;
        }

        if (function.isThunk()) {
            Function thunkedFunction = function.getThunkedFunction(true);
            if (thunkedFunction.isExternal()) {
                return COLOR_EXTERNAL_FUNCTION;
            }
        }

        return symbolInspector.getColor(symbol);
    }

    private Color getAddrColor(Address addr) {
        Function func = program.getFunctionManager().getFunctionAt(addr);
        if (func != null) {
            return getFunctionColor(func);
        }

        // TODO: undefined data? struct fields?

        return decompileOptions.getGlobalColor();
    }

    private boolean isROString(Data data) {
        return data.hasStringValue() && (
            data.isConstant() ||
            !program.getMemory().getBlock(data.getAddress()).isWrite()
        );
    }

    private void emitNamespace(List<EvtToken> ret, EvtScript script, String name, Address atAddr, long size) {
        ret.add(new EvtToken(script, name, decompileOptions.getGlobalColor(), atAddr, size));
        ret.add(new EvtToken(script, "::", decompileOptions.getDefaultColor(), atAddr, size));
    }

    protected List<EvtToken> symbolToTokens(EvtScript script, Address atAddr, Color color, Address target, long size) {
        Symbol symbol = program.getSymbolTable().getPrimarySymbol(target);
        if (symbol == null) {
            Msg.warn(this, "No symbol for " + target);
            return Arrays.asList(addrFailToken(script, atAddr, target, size));
        }
        else  {
            List<EvtToken> ret = new ArrayList<>();
            if (decompileOptions.isEnableNamespaces()) {
                Namespace ns = symbol.getParentNamespace();
                if (decompileOptions.isCMacroMode())
                    emitNamespace(ret, script, decompileOptions.getGameNamespace(), atAddr, size);
    
                while (!ns.isGlobal()) {
                    emitNamespace(ret, script, ns.getName(), atAddr, size);
                    ns = ns.getParentNamespace();
                }
            }
            ret.add(new EvtAddrToken(script, symbol.getName(), color, atAddr, target, size));
            return ret;
        }
    }

    private EvtToken addrFailToken(EvtScript script, Address atAddr, Address target, long size) {
        return new EvtAddrToken(script, "ERR_" + target, COLOR_EXTERNAL_FUNCTION, atAddr, target, size);
    }

    protected boolean isString(Arg.ADDR arg) {
        Address target = program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());
        CodeUnit cu = program.getListing().getCodeUnitAt(target);
        return (cu instanceof Data data && isROString(data));
    }

    protected boolean isFunction(Arg.ADDR arg) {
        Address target = program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());
        CodeUnit cu = program.getListing().getCodeUnitAt(target);
        return (cu instanceof Instruction);
    }

    protected List<EvtToken> addrToTokens(EvtScript script, Instr instr, Arg.ADDR arg, Address atAddr) {
        List<EvtToken> ret = new ArrayList<>();

        Address target = program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());

        Color color = getAddrColor(target);

        CodeUnit cu = program.getListing().getCodeUnitAt(target);
        if (cu == null) {
            Msg.warn(this, "No code unit for " + Long.toHexString(arg.value()));
            ret.add(addrFailToken(script, atAddr, target, Arg.bytesSize()));
        }
        else if (cu instanceof Data data && isROString(data)) {
            String value = (String) data.getValue();
            ret.add(new EvtAddrToken(script, "\"" + value + "\"", decompileOptions.getConstantColor(), atAddr, target, Arg.bytesSize()));
        }
        else {
            ret.addAll(symbolToTokens(script, atAddr, color, target, Arg.bytesSize()));
        }

        return ret;
    }

    protected List<EvtToken> floatToTokens(EvtScript script, Instr instr, float value, Address atAddr) {
        return Arrays.asList(EvtToken.argScalar(
            script,
            Float.toString(value),
            decompileOptions.getConstantColor(),
            atAddr,
            Float.floatToRawIntBits(value),
            true
        ));
    }

    protected List<EvtToken> intToTokens(EvtScript script, Instr instr, int value, Address atAddr) {
        return Arrays.asList(EvtToken.argScalar(
            script,
            Integer.toString(value),
            decompileOptions.getConstantColor(),
            atAddr,
            value,
            true
        ));
    }

    protected List<EvtToken> variableToTokens(EvtScript script, Instr instr, Arg.Variable variable, Address atAddr) {
        return Arrays.asList(EvtToken.var(
            script,
            variable.getName(),
            variableToColor(variable),
            atAddr,
            variable
        ));
    }

    protected List<EvtToken> noneToTokens(EvtScript script, Instr instr, Address atAddr) {
        return Arrays.asList(EvtToken.arg(
            script,
            "NONE",
            decompileOptions.getVariableColor(),
            atAddr
        ));
    }
    
    protected abstract int getMinIndent();
    protected abstract void buildHeader();
    protected abstract void startInstr(Instr instr, List<EvtToken> tokens);
    protected abstract void buildArgSeparator(boolean first, List<EvtToken> tokens);
    protected abstract void endInstr(Instr instr, List<EvtToken> tokens);
    protected abstract void buildFooter();

    private List<EvtToken> argToTokens(EvtScript script, Instr instr, Arg arg, Address atAddr) {
        return switch (arg) {
            case Arg.ADDR addr -> addrToTokens(script, instr, addr, atAddr);
            case Arg.FLOAT(float value) -> floatToTokens(script, instr, value, atAddr);
            case Arg.INT(int value) -> intToTokens(script, instr, value, atAddr);
            case Arg.Variable variable -> variableToTokens(script, instr, variable, atAddr);
            case Arg.NONE() -> noneToTokens(script, instr, atAddr);
        };
    }

    private void buildLines() {
        // Header
        buildHeader();

        indent = getMinIndent();

        Address lineAddr = currentAddr;
        for (Instr instr : docroot) {
            lineAddr = currentAddr;

            // Don't print terminator instruction
            Opcode opcode = instr.opcode();
            if (opcode == Opcode.END_SCRIPT)
                continue;

            // Unindent for this line
            indent -= opcode.unindent();
            indent = Math.max(indent, getMinIndent());

            List<EvtToken> tokens = new ArrayList<>();

            startInstr(instr, tokens);
            currentAddr = currentAddr.add(Instr.HEADER_SIZE);

            boolean first = true;
            for (Arg arg : instr.args())
            {
                buildArgSeparator(first, tokens);
                tokens.addAll(argToTokens(script, instr, arg, currentAddr));
                first = false; 
                currentAddr = currentAddr.add(Arg.bytesSize());
            }

            endInstr(instr, tokens);

            doc.addLine(new EvtLine(tokens, lineAddr, displayLine++, indent));

            // Indent for next line
            indent += opcode.indent();
        }

        buildFooter();

        List<EvtToken> blank = Arrays.asList(new EvtToken(script, "", decompileOptions.getDefaultColor(), lineAddr, 0));
        doc.addLine(new EvtLine(blank, currentAddr, displayLine++, 0));
    }

    public EvtDocument getLines() {
        return doc;
    }

    public static GhidraPrinter create(Program program, SymbolInspector symbolInspector,
            EvtOptions options, EvtScript script, List<Instr> docroot, boolean cMacroMode) {
        if (cMacroMode) {
            return new CMacroGhidraPrinter(program, symbolInspector, options, script, docroot);
        }
        else {
            return new PrettyGhidraPrinter(program, symbolInspector, options, script, docroot);
        }
    }
}
