package ghidraevt;

import java.util.List;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Program;
import jevt.Instr;

public class EvtData {
    private Program program;
    private Data data;
    private Address startAddress;
    private List<Instr> script;
    private String errorMessage;

    public EvtData(Program program, Data data, Address startAddress, List<Instr> script, String errorMesssage) {
        this.program = program;
        this.data = data;
        this.startAddress = startAddress;
        this.script = script;
        this.errorMessage = errorMesssage;
    }

    public Program getProgram() {
        return program;
    }

    public Data getData() {
        return data;
    }

    public Address getStartAddress() {
        return startAddress;
    }

    public List<Instr> getScript() {
        return script;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    public static EvtData success(Program program, Address startAddress, List<Instr> script) {
        return new EvtData(program, startAddress, script, null);
    }

    public static EvtData empty(String err) {
        return new EvtData(null, null, null, err);
    }

    public static EvtData fail(Program program, Address startAddress, String err) {
        return new EvtData(program, startAddress, null, err);
    }
}
