package ghidraevt;

import java.awt.Color;

import ghidra.program.model.address.Address;
import jevt.Arg;

public class EvtAddrToken extends EvtToken {
    private Address target;

    public EvtAddrToken(String txt, Color color, Address minAddress, Address target) {
        super(txt, color, minAddress, Arg.bytesSize());
        this.target = target;
    }

    public Address getTarget() {
        return target;
    }
}
