package ghidraevt;

import java.util.List;

import ghidra.program.model.address.Address;

public class EvtLine {
    private int lineNumber;
    private int indent;
    private List<EvtToken> tokens;
    private Address addr;

    public EvtLine(List<EvtToken> tokens, Address addr, int lineNumber, int indent) {
        this.tokens = tokens;
        this.addr = addr;
        this.lineNumber = lineNumber;
        this.indent = indent;
    }

    public List<EvtToken> getAllTokens() {
        return tokens;
    }

    public Address addr() {
        return addr;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int line() {
        return lineNumber;
    }

    public int indent() {
        return indent;
    }

}
