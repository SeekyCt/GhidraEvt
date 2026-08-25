package ghidraevt;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generic.theme.GColor;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Namespace;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.Msg;
import ghidra.app.util.SymbolInspector;
import jevt.Instr;
import jevt.Opcode;
import jevt.Arg;

public class GhidraPrinter {
    public static Color COLOR_LW    = new GColor("color.fg.ghidraevt.lw");
    public static Color COLOR_LF    = new GColor("color.fg.ghidraevt.lf");
    public static Color COLOR_LSW   = new GColor("color.fg.ghidraevt.lsw");
    public static Color COLOR_LSWF  = new GColor("color.fg.ghidraevt.lswf");
    public static Color COLOR_GW    = new GColor("color.fg.ghidraevt.gw");
    public static Color COLOR_GF    = new GColor("color.fg.ghidraevt.gf");
    public static Color COLOR_GSW   = new GColor("color.fg.ghidraevt.gsw");
    public static Color COLOR_GSWF  = new GColor("color.fg.ghidraevt.gswf");
    public static Color COLOR_UW    = new GColor("color.fg.ghidraevt.uw");
    public static Color COLOR_UF    = new GColor("color.fg.ghidraevt.uf");
    public static Color COLOR_INSTR = new GColor("color.fg.ghidraevt.instr");
    public static Color COLOR_COMMENT = new GColor("color.fg.ghidraevt.comment");

	public static Color COLOR_EXTERNAL_FUNCTION = new GColor("color.fg.decompiler.external.function");

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

    private List<EvtToken> addrToTokens(Arg.ADDR arg, Address atAddr) {
        List<EvtToken> ret = new ArrayList<>();

        Address target = program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());

        Color color = getAddrColor(target);

        EvtToken fail = new EvtAddrToken("ERR_" + Long.toHexString(arg.value()), COLOR_EXTERNAL_FUNCTION, atAddr, target);

        CodeUnit cu = program.getListing().getCodeUnitAt(target);
        if (cu == null) {
            Msg.warn(this, "No code unit for " + Long.toHexString(arg.value()));
            ret.add(fail);
        }
        else if (cu instanceof Data data && isROString(data)) {
            String value = (String) data.getValue();
            ret.add(new EvtAddrToken("\"" + value + "\"", decompileOptions.getConstantColor(), atAddr, target));
        }
        else {
            Symbol symbol = cu.getPrimarySymbol();
            if (symbol == null) {
                Msg.warn(this, "No symbol for " + Long.toHexString(arg.value()));
                ret.add(fail);
            }
            else  {
                Namespace ns = symbol.getParentNamespace();
                // TODO: options - off/on/sync decompiler, and also force in C macro mode with spm::
                while (!ns.isGlobal()) {
                    ret.add(EvtToken.arg(ns.getName(), decompileOptions.getGlobalColor(), atAddr));
                    ret.add(EvtToken.arg("::", decompileOptions.getDefaultColor(), atAddr));
                    ns = ns.getParentNamespace();
                }
                ret.add(new EvtAddrToken(symbol.getName(), color, atAddr, target));
            }
        }

        return ret;
    }

    private List<EvtToken> argToTokens(Arg arg, Address atAddr) {
        return switch (arg) {
            case Arg.ADDR addr -> addrToTokens(addr, atAddr); // TODO: dynamic color
            case Arg.FLOAT(float value) -> Arrays.asList(
                EvtToken.arg(Float.toString(value), decompileOptions.getConstantColor(), atAddr)
            );
            case Arg.INT(int value) -> Arrays.asList(
                EvtToken.arg(Integer.toString(value), decompileOptions.getConstantColor(), atAddr)
            );
            case Arg.Variable variable -> Arrays.asList(
                EvtToken.arg(variable.typeName() + "(" + variable.id() + ")", variableToColor(variable), atAddr)
            );
            case Arg.NONE() ->  Arrays.asList(
                EvtToken.arg("NONE", decompileOptions.getVariableColor(), atAddr)
            );
        };
    }

    public List<EvtLine> getLines(EvtScript script, List<Instr> docroot) {
        List<EvtLine> ret = new ArrayList<>();
        int indent = 0;
        int line = 1;
        Address addr = script.getStartAddress();
        for (Instr instr : docroot) {
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            List<EvtToken> tokens = new ArrayList<>();

            tokens.add(EvtToken.instr(opcode.name(), COLOR_INSTR, addr));
            for (Arg arg : instr.args())
            {
                tokens.add(EvtToken.syntax(" ", decompileOptions.getDefaultColor(), addr));
                tokens.addAll(argToTokens(arg, addr));
            }

            ret.add(new EvtLine(tokens, addr, line, indent));

            // Indent for next line
            indent += opcode.indent();
            line += 1;
            addr = addr.add(instr.bytesSize());
        }

        return ret;
    }
}
