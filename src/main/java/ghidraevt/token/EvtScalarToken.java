package ghidraevt.token;

import java.awt.Color;

import ghidra.program.model.address.Address;
import ghidra.program.model.scalar.Scalar;
import ghidraevt.component.EvtScript;
import jevt.Arg;

public class EvtScalarToken extends EvtToken {
    private Scalar scalar;

    public EvtScalarToken(EvtScript script, String txt, Color color, Address minAddress, long value, boolean signed) {
        super(script, txt, color, minAddress, Arg.bytesSize());
        this.scalar = new Scalar(32, value, signed);
    }

    public Scalar getScalar() {
        return scalar;
    }
}
