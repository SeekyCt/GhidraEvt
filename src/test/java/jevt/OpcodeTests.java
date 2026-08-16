package jevt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OpcodeTests {
    @Test
    void opcodeLimits() throws BadEvtException {
        assertEquals(Opcode.NEXT.encode(Game.TTYD), 0);
        assertEquals(Opcode.NEXT.encode(Game.SPM), 0);

        assertEquals(Opcode.DEBUG_BP.encode(Game.TTYD), 118);
        assertEquals(Opcode.DEBUG_BP.encode(Game.SPM), 119);

        assertEquals(Opcode.NEXT, Opcode.decode(Game.TTYD, 0));
        assertEquals(Opcode.NEXT, Opcode.decode(Game.SPM, 0));

        assertEquals(Opcode.DEBUG_BP, Opcode.decode(Game.TTYD, 118));
        assertEquals(Opcode.DEBUG_BP, Opcode.decode(Game.SPM, 119));
    }

    @Test
    void spmAdjustments() throws BadEvtException {
        assertThrows(IllegalArgumentException.class, () -> Opcode.CLAMP_INT.encode(Game.TTYD));
        assertEquals(Opcode.CLAMP_INT.encode(Game.SPM), 74);

        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.TTYD, 74));
        assertEquals(Opcode.CLAMP_INT, Opcode.decode(Game.SPM, 74));

        assertEquals(Opcode.READF_N.encode(Game.TTYD), 73);
        assertEquals(Opcode.READF_N.encode(Game.SPM), 73);
        assertEquals(Opcode.SET_USER_WRK.encode(Game.TTYD), 74);
        assertEquals(Opcode.SET_USER_WRK.encode(Game.SPM), 75);

        assertEquals(Opcode.READF_N, Opcode.decode(Game.TTYD, 73));
        assertEquals(Opcode.READF_N, Opcode.decode(Game.SPM, 73));
        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.TTYD, 74));
        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.SPM, 75));
    }

    @Test
    void badOpcodeThrows() {
        assertThrows(BadEvtException.class, () -> Opcode.decode(Game.SPM, -1));
        assertThrows(BadEvtException.class, () -> Opcode.decode(Game.SPM, 120));
    }
}
