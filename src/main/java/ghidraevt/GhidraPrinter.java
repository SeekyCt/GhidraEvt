package ghidraevt;

import java.util.List;

import ghidra.program.model.address.Address;
import jevt.Instr;
import jevt.Opcode;
import jevt.Arg;

public class GhidraPrinter {
    boolean showLineNumbers;
    boolean showAddresses;

    public GhidraPrinter(boolean showLineNumbers, boolean showAddresses) {
        this.showLineNumbers = showLineNumbers;
        this.showAddresses = showAddresses;
    }

    public String print_evt(Address startAddress, List<Instr> script) {
        StringBuilder ret = new StringBuilder();
        int indent = 0;
        int line = 1;
        Address addr = startAddress;
        for (Instr instr : script) {
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            if (showLineNumbers)
                ret.append(String.format("[%03d] ", line));

            if (showAddresses)
                ret.append(addr + " ");

            for (int i = 0; i < indent; i++)
                ret.append("    ");

            ret.append(opcode.name());

            for (Arg arg : instr.args()) {
                ret.append(" " + arg.toString());
            }

            ret.append("\n");

            // Indent for next line
            indent += opcode.indent();
            line += 1;
            addr = addr.add(instr.bytesSize());
        }
        return ret.toString();
    }
}
