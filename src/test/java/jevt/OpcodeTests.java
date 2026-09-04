/* ###
 * Copyright 2026 SeekyCt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jevt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

        assertEquals(Opcode.NEXT, Opcode.decode(Game.TTYD, 0, false));
        assertEquals(Opcode.NEXT, Opcode.decode(Game.SPM, 0, false));

        assertEquals(Opcode.DEBUG_BP, Opcode.decode(Game.TTYD, 118, false));
        assertEquals(Opcode.DEBUG_BP, Opcode.decode(Game.SPM, 119, false));
    }

    @Test
    void spmAdjustments() throws BadEvtException {
        assertThrows(IllegalArgumentException.class, () -> Opcode.CLAMP_INT.encode(Game.TTYD));
        assertEquals(Opcode.CLAMP_INT.encode(Game.SPM), 74);

        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.TTYD, 74, false));
        assertEquals(Opcode.CLAMP_INT, Opcode.decode(Game.SPM, 74, false));

        assertEquals(Opcode.READF_N.encode(Game.TTYD), 73);
        assertEquals(Opcode.READF_N.encode(Game.SPM), 73);
        assertEquals(Opcode.SET_USER_WRK.encode(Game.TTYD), 74);
        assertEquals(Opcode.SET_USER_WRK.encode(Game.SPM), 75);

        assertEquals(Opcode.READF_N, Opcode.decode(Game.TTYD, 73, false));
        assertEquals(Opcode.READF_N, Opcode.decode(Game.SPM, 73, false));
        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.TTYD, 74, false));
        assertEquals(Opcode.SET_USER_WRK, Opcode.decode(Game.SPM, 75, false));
    }

    @Test
    void badOpcodeThrows() {
        assertThrows(BadEvtException.class, () -> Opcode.decode(Game.SPM, -1, false));
        assertThrows(BadEvtException.class, () -> Opcode.decode(Game.SPM, 120, false));
    }

    @Test
    void strictMode() {
        assertDoesNotThrow(() -> Opcode.decode(Game.SPM, 0, false));
        assertThrows(StrictEvtException.class, () -> Opcode.decode(Game.SPM, 0, true));
    }
}
