package ghidraevt;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import generic.theme.GColor;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.app.decompiler.component.ClangLayoutController;
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
    // TODO: gcolor
    public static Color COLOR_LW = new Color(0xffa500); // orange
    public static Color COLOR_LF =  new Color(0xff4500); // orangered
    public static Color COLOR_LSW = new Color(0xff7f50); // coral
    public static Color COLOR_LSWF = new Color(0xd2691e); // chocolate

    public static Color COLOR_GW = new Color(0x7fffd4); // aquamarine
    public static Color COLOR_GF = new Color(0x00ffff); // aqua
    public static Color COLOR_GSW = new Color(0x90ee90); // lightgreen
    public static Color COLOR_GSWF = new Color(0xadff2f); // greenyellow

    public static Color COLOR_UW = new Color(0xffc0cb); // pink
    public static Color COLOR_UF = new Color(0x800080); // purple

    public static Color COLOR_KEYWORD = new GColor("color.fg.decompiler.keyword");
    public static Color COLOR_CONST = new GColor("color.fg.decompiler.constant");
    public static Color COLOR_DEFAULT = new GColor("color.fg.decompiler");

    // Program program;

    // boolean showLineNumbers;
    // boolean showAddresses;

    // public GhidraPrinter(Program program, boolean showLineNumbers, boolean showAddresses) {
    //     this.program = program;
    //     this.showLineNumbers = showLineNumbers;
    //     this.showAddresses = showAddresses;
    // }

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

    private EvtToken argToToken(Arg arg) {
        return switch (arg) {
            case Arg.ADDR(long value) -> new EvtToken(Long.toHexString(value), COLOR_DEFAULT); // TODO: dynamic color
            case Arg.FLOAT(float value) -> new EvtToken(Float.toString(value), COLOR_CONST);
            case Arg.INT(int value) -> new EvtToken(Integer.toString(value), COLOR_CONST);
            case Arg.Variable variable -> new EvtToken(variable.typeName() + "(" + variable.id() + ")", variableToColor(variable));
            case Arg.NONE() -> new EvtToken("NONE", COLOR_CONST);
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

            tokens.add(new EvtToken(opcode.name(), COLOR_KEYWORD));
            for (Arg arg : instr.args())
            {
                tokens.add(new EvtToken(" ", COLOR_DEFAULT));
                tokens.add(argToToken(arg));
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
