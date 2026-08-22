package ghidraevt;

import ghidra.program.model.address.Address;
import jevt.Instr;

public class EvtLine {
    private int lineNumber;
    private int indent;
    private Instr instr;
    private Address addr;

    public EvtLine(Instr instr, Address addr, int lineNumber, int indent) {
        this.instr = instr;
        this.addr = addr;
        this.lineNumber = lineNumber;
        this.indent = indent;
    }

    public Instr instr() {
        return instr;
    }

    public Address addr() {
        return addr;
    }

    public int linenumber() {
        return lineNumber;
    }

    public int indent() {
        return indent;
    }

}
