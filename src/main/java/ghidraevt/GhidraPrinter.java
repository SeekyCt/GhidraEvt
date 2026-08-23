package ghidraevt;

import java.util.ArrayList;
import java.util.List;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.app.decompiler.component.ClangLayoutController;
import jevt.Instr;
import jevt.Opcode;
import jevt.Arg;

public class GhidraPrinter {
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

            tokens.add(new EvtToken(opcode.name(), EvtToken.KEYWORD_COLOR));
            for (Arg arg : instr.args())
            {
                tokens.add(new EvtToken(" "));
                tokens.add(new EvtToken(arg.toString()));
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
