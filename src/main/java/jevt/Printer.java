package jevt;

import java.util.List;

public class Printer {
    public String print_evt(List<Instr> script) {
        StringBuilder ret = new StringBuilder();
        int indent = 0;
        for (Instr instr : script) {
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            for (int i = 0; i < indent; i++)
                ret.append("    ");

            ret.append(opcode.name());

            for (Arg arg : instr.args()) {
                ret.append(arg.toString());
            }

            ret.append("\n");

            // Indent for next line
            indent += opcode.indent();
        }
        return ret.toString();
    }
}
