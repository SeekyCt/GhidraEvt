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
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Namespace;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.Msg;
import ghidraevt.component.EvtOptions;
import ghidraevt.component.EvtScript;
import jevt.Arg;
import jevt.Instr;
import jevt.Opcode;

public class GhidraPrinter {
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

    Program program;
    SymbolInspector symbolInspector;
    EvtOptions decompileOptions;

    // boolean showLineNumbers;
    // boolean showAddresses;

    public GhidraPrinter(Program program, SymbolInspector symbolInspector, EvtOptions decompileOptions) {
        this.program = program;
        this.symbolInspector = symbolInspector;
        this.decompileOptions = decompileOptions;
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

    private List<EvtToken> symbolToTokens(EvtScript script, Address atAddr, Color color, Address target, long size) {
        Symbol symbol = program.getSymbolTable().getPrimarySymbol(target);
        if (symbol == null) {
            Msg.warn(this, "No symbol for " + target);
            return Arrays.asList(addrFailToken(script, atAddr, target, size));
        }
        else  {
            Namespace ns = symbol.getParentNamespace();
            List<EvtToken> ret = new ArrayList<>();
            // TODO: options - off/on/sync decompiler, and also force in C macro mode with spm::
            while (!ns.isGlobal()) {
                ret.add(new EvtToken(script, ns.getName(), decompileOptions.getGlobalColor(), atAddr, size));
                ret.add(new EvtToken(script, "::", decompileOptions.getDefaultColor(), atAddr, size));
                ns = ns.getParentNamespace();
            }
            ret.add(new EvtAddrToken(script, symbol.getName(), color, atAddr, target, size));
            return ret;
        }
    }

    private EvtToken addrFailToken(EvtScript script, Address atAddr, Address target, long size) {
        return new EvtAddrToken(script, "ERR_" + target, COLOR_EXTERNAL_FUNCTION, atAddr, target, size);
    }

    private List<EvtToken> addrToTokens(EvtScript script, Arg.ADDR arg, Address atAddr) {
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

    private List<EvtToken> argToTokens(EvtScript script, Arg arg, Address atAddr) {
        return switch (arg) {
            case Arg.ADDR addr -> addrToTokens(script, addr, atAddr);
            case Arg.FLOAT(float value) -> Arrays.asList(EvtToken.argScalar(
                script,
                Float.toString(value),
                decompileOptions.getConstantColor(),
                atAddr,
                Float.floatToRawIntBits(value),
                true
            ));
            case Arg.INT(int value) -> Arrays.asList(EvtToken.argScalar(
                script,
                Integer.toString(value),
                decompileOptions.getConstantColor(),
                atAddr,
                value,
                true
            ));
            case Arg.Variable variable -> Arrays.asList(EvtToken.var(
                script,
                variable.typeName() + "(" + variable.id() + ")",
                variableToColor(variable),
                atAddr,
                variable
            ));
            case Arg.NONE() ->  Arrays.asList(EvtToken.arg(
                script,
                "NONE",
                decompileOptions.getVariableColor(),
                atAddr
            ));
        };
    }
    
    private static final String HEADER_DECORATION = "==========";
    public List<EvtLine> getLines(EvtScript script, List<Instr> docroot) {
        List<EvtLine> ret = new ArrayList<>();
        int indent = 0;
        int line = 1;
        int displayLine = 1;
        Address addr = script.getStartAddress();

        // Header
        List<EvtToken> header = new ArrayList<>();
        header.add(EvtToken.syntax(script, HEADER_DECORATION + " ", decompileOptions.getDefaultColor(), addr));
        header.addAll(symbolToTokens(script, addr, COLOR_HEADER, addr, 0));
        header.add(EvtToken.syntax(script, " " + HEADER_DECORATION, decompileOptions.getDefaultColor(), addr));
        ret.add(new EvtLine(header, addr, line++, 0, indent));

        Address lineAddr = addr;
        for (Instr instr : docroot) {
            lineAddr = addr;
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            List<EvtToken> tokens = new ArrayList<>();

            tokens.add(EvtToken.instr(script, opcode.niceName(), COLOR_INSTR, addr));
            addr = addr.add(Instr.HEADER_SIZE);

            boolean first = true;
            for (Arg arg : instr.args())
            {
                String sep = first ? " " : ", ";
                first = false; 
                tokens.add(EvtToken.syntax(script, sep, decompileOptions.getDefaultColor(), addr));
                tokens.addAll(argToTokens(script, arg, addr));
                addr = addr.add(Arg.bytesSize());
            }

            ret.add(new EvtLine(tokens, lineAddr, line++, displayLine++, indent));

            // Indent for next line
            indent += opcode.indent();
        }

        List<EvtToken> blank = Arrays.asList(new EvtToken(script, "", decompileOptions.getDefaultColor(), lineAddr, 0));
        ret.add(new EvtLine(blank, addr, line++, displayLine++, indent));

        return ret;
    }
}
