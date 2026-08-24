package ghidraevt;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generic.theme.GColor;
import ghidra.program.model.address.Address;
import ghidra.program.model.data.AbstractStringDataType;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Namespace;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.Msg;
import ghidra.util.UndefinedFunction;
import ghidra.app.decompiler.ClangFuncNameToken;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.component.ClangLayoutController;
import ghidra.app.decompiler.component.DecompilerUtils;
import ghidra.app.util.SymbolInspector;
import jevt.Instr;
import jevt.Opcode;
import jevt.Arg;
import jevt.Arg.GF;
import jevt.Arg.GSW;
import jevt.Arg.GSWF;
import jevt.Arg.GW;
import jevt.Arg.LF;
import jevt.Arg.LSW;
import jevt.Arg.LSWF;
import jevt.Arg.LW;
import jevt.Arg.UF;
import jevt.Arg.UW;

public class GhidraPrinter {
    public static Color COLOR_LW =    new GColor("color.fg.ghidraevt.lw");
    public static Color COLOR_LF =    new GColor("color.fg.ghidraevt.lf");
    public static Color COLOR_LSW =   new GColor("color.fg.ghidraevt.lsw");
    public static Color COLOR_LSWF =  new GColor("color.fg.ghidraevt.lswf");
    public static Color COLOR_GW =    new GColor("color.fg.ghidraevt.gw");
    public static Color COLOR_GF =    new GColor("color.fg.ghidraevt.gf");
    public static Color COLOR_GSW =   new GColor("color.fg.ghidraevt.gsw");
    public static Color COLOR_GSWF =  new GColor("color.fg.ghidraevt.gswf");
    public static Color COLOR_UW =    new GColor("color.fg.ghidraevt.uw");
    public static Color COLOR_UF =    new GColor("color.fg.ghidraevt.uf");
    public static Color COLOR_INSTR = new GColor("color.fg.ghidraevt.instr");

    public static Color COLOR_GLOBAL =            new GColor("color.fg.decompiler.global");
    public static Color COLOR_CONST =             new GColor("color.fg.decompiler.constant");
    public static Color COLOR_VAR =               new GColor("color.fg.decompiler.variable");
    public static Color COLOR_DEFAULT =           new GColor("color.fg.decompiler");
	public static Color COLOR_EXTERNAL_FUNCTION = new GColor("color.fg.decompiler.external.function");

    Program program;
    SymbolInspector symbolInspector;

    // boolean showLineNumbers;
    // boolean showAddresses;

    public GhidraPrinter(Program program, SymbolInspector symbolInspector /*, boolean showLineNumbers, boolean showAddresses*/ ) {
        this.program = program;
        this.symbolInspector = symbolInspector;
        // this.showLineNumbers = showLineNumbers;
        // this.showAddresses = showAddresses;
    }

    // private String printArg(Arg argument) {
    //     switch (argument) {
    //         case Arg.NONE arg:
    //             return "NONE";

    //         case Arg.ADDR arg:
    //             Address addr =
    //                 program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());
    //             Symbol sym = program.getSymbolTable().getPrimarySymbol(addr);
    //             if (sym != null)
    //                 return sym.getName();
    //             else
    //                 return "UNK_" + Long.toHexString(arg.value());

    //         case Arg.FLOAT arg:
    //             // TODO: would want macro wrapping in exports
    //             return Float.toString(arg.value());

    //         case Arg.INT arg:
    //             return Integer.toString(arg.value());

    //         case Arg.Variable arg:
    //             return String.format("%s(%d)", argument.typeName(), arg.id());
    //     }
    // }

    // public String print_evt(Address startAddress, List<Instr> script) {
    //     StringBuilder ret = new StringBuilder();
    //     int indent = 0;
    //     int line = 1;
    //     Address addr = startAddress;
    //     for (Instr instr : script) {
    //         Opcode opcode = instr.opcode();

    //         // Unindent for this line
    //         indent -= opcode.unindent();

    //         if (showLineNumbers)
    //             ret.append(String.format("[%03d] ", line));

    //         if (showAddresses)
    //             ret.append(addr + " ");

    //         for (int i = 0; i < indent; i++)
    //             ret.append("    ");

    //         ret.append(opcode.name());

    //         for (Arg arg : instr.args()) {
    //             ret.append(" " + printArg(arg));
    //         }

    //         ret.append("\n");

    //         // Indent for next line
    //         indent += opcode.indent();
    //         line += 1;
    //         addr = addr.add(instr.bytesSize());
    //     }
    //     return ret.toString();
    // }

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

        // TODO: undefined data?

        return COLOR_GLOBAL;
        // switch(ct->getMetatype())
        // case TYPE_UNKNOWN:
        // push_integer(val,ct->getSize(),false,tag,vn,op,displayFormat);

        // case TYPE_PTR:
        // case TYPE_PTRREL:
        // if (option_NULL&&(val==0)) { // A null pointer
        // pushAtom(Atom(nullToken,vartoken,EmitMarkup::var_color,op,vn));
        // return;
        // }
        // subtype = ((TypePointer *)ct)->getPtrTo();
        // if (subtype->isCharPrint()) {
        // if (pushPtrCharConstant(val,(const TypePointer *)ct,vn,op))
        // return;
        // }
        // else if (subtype->getMetatype()==TYPE_CODE) {
        // if (pushPtrCodeConstant(val,(const TypePointer *)ct,vn,op))
        // return;
        // }
        // break;

    }

    private boolean isROString(Data data) {
        return data.hasStringValue() && (
            data.isConstant() ||
            !program.getMemory().getBlock(data.getAddress()).isWrite()
        );
    }

    private List<EvtToken> addrToTokens(Arg.ADDR arg) {
        List<EvtToken> ret = new ArrayList<>();

        Address addr = program.getAddressFactory().getDefaultAddressSpace().getAddress(arg.value());

        Color color = getAddrColor(addr);

        EvtToken fail = new EvtToken("ERR_" + Long.toHexString(arg.value()), COLOR_EXTERNAL_FUNCTION);

        CodeUnit cu = program.getListing().getCodeUnitAt(addr);
        if (cu == null) {
            Msg.warn(this, "No code unit for " + Long.toHexString(arg.value()));
            ret.add(fail);
        }
        else if (cu instanceof Data data && isROString(data)) {
            String value = (String) data.getValue();
            ret.add(new EvtToken("\"" + value + "\"", COLOR_CONST));
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
                    ret.add(new EvtToken(ns.getName(), COLOR_GLOBAL));
                    ret.add(new EvtToken("::", COLOR_DEFAULT));
                    ns = ns.getParentNamespace();
                }
                ret.add(new EvtToken(symbol.getName(), color));
            }
        }


        return ret;
    }


    private List<EvtToken> argToTokens(Arg arg) {
        return switch (arg) {
            case Arg.ADDR addr -> addrToTokens(addr); // TODO: dynamic color
            case Arg.FLOAT(float value) -> Arrays.asList(
                new EvtToken(Float.toString(value), COLOR_CONST)
            );
            case Arg.INT(int value) -> Arrays.asList(
                new EvtToken(Integer.toString(value), COLOR_CONST)
            );
            case Arg.Variable variable -> Arrays.asList(
                new EvtToken(variable.typeName() + "(" + variable.id() + ")", variableToColor(variable))
            );
            case Arg.NONE() ->  Arrays.asList(
                new EvtToken("NONE", COLOR_VAR)
            );
        };
    }

    public List<EvtLine> getLines(Address startAddress, List<Instr> script) {
        List<EvtLine> ret = new ArrayList<>();
        int indent = 0;
        int line = 1;
        Address addr = startAddress;
        for (Instr instr : script) {
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            List<EvtToken> tokens = new ArrayList<>();

            tokens.add(new EvtToken(opcode.name(), COLOR_INSTR));
            for (Arg arg : instr.args())
            {
                tokens.add(new EvtToken(" ", COLOR_DEFAULT));
                tokens.addAll(argToTokens(arg));
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
