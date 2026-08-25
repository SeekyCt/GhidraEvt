package ghidraevt;

import java.awt.Color;

import ghidra.program.model.address.Address;

public class EvtAddrToken extends EvtToken {
    private Address target;

    public EvtAddrToken(String txt, Color color, Address minAddress, long size, Address target) {
        super(txt, color, minAddress, size);
        this.target = target;
    }

    public Address getTarget() {
        return target;
    }
    
}
