package ghidraevt;

import java.util.List;

import ghidra.program.model.address.Address;
import jevt.Instr;

public class EvtData {
    private Address startAddress;
    private List<Instr> script;
    private String errorMessage;

    public EvtData(Address startAddress, List<Instr> script, String errorMesssage) {
        this.startAddress = startAddress;
        this.script = script;
        this.errorMessage = errorMesssage;
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

    public static EvtData success(Address startAddress, List<Instr> script) {
        return new EvtData(startAddress, script, null);
    }

    public static EvtData empty(String err) {
        return new EvtData(null, null, err);
    }

    public static EvtData fail(Address startAddress, String err) {
        return new EvtData(startAddress, null, err);
    }
}
