package ghidraevt.token;

import java.awt.Color;

import ghidra.program.model.address.Address;
import ghidraevt.component.EvtScript;

public class EvtAddrToken extends EvtToken {
    private Address target;

    public EvtAddrToken(EvtScript script, String txt, Color color, Address minAddress, Address target, long size) {
        super(script, txt, color, minAddress, size);
        this.target = target;
    }

    public Address getTarget() {
        return target;
    }
}
