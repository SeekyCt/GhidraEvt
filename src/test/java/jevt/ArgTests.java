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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// TODO: TTYD tests

public class ArgTests {
    @Test
    void decode() throws BadEvtException {
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x8000_0000L, false),
            new Arg.ADDR(0x8000_0000L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x8000_0001L, false),
            new Arg.ADDR(0x8000_0001L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x9000_0000L, false),
            new Arg.ADDR(0x9000_0000L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x9000_0001L, false),
            new Arg.ADDR(0x9000_0001L)
        );

        assertEquals(Arg.decode(Game.SPM, (int)0xf1b1e400L, false), new Arg.FLOAT(0.0f));
        assertEquals(Arg.decode(Game.SPM, (int)0xf1b1e800L, false), new Arg.FLOAT(1.0f));

        assertEquals(Arg.decode(Game.SPM, (int)0xf37ba780L, false), new Arg.UF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf37ba781L, false), new Arg.UF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf4acd480L, false), new Arg.UW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf4acd481L, false), new Arg.UW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf5de0180L, false), new Arg.GSW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf5de0181L, false), new Arg.GSW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf70f2e80L, false), new Arg.LSW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf70f2e81L, false), new Arg.LSW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf8405b80L, false), new Arg.GSWF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf8405b81L, false), new Arg.GSWF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf9718880L, false), new Arg.LSWF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf9718881L, false), new Arg.LSWF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfaa2b580L, false), new Arg.GF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfaa2b581L, false), new Arg.GF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfbd3e280L, false), new Arg.LF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfbd3e281L, false), new Arg.LF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfd050f80L, false), new Arg.GW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfd050f81L, false), new Arg.GW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfe363c80L, false), new Arg.LW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfe363c81L, false), new Arg.LW(1));

        assertEquals(Arg.decode(Game.SPM, -1, false), new Arg.INT(-1));
        assertEquals(Arg.decode(Game.SPM, 0, false), new Arg.INT(0));
        assertEquals(Arg.decode(Game.SPM, 1, false), new Arg.INT(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xefe82080L, false), new Arg.NONE());
    }

    @Test
    void encode() throws BadEvtException {
        assertEquals(
            new Arg.ADDR(0x8000_0000L).encode(Game.SPM),
            (int)0x8000_0000L
        );
        assertEquals(
            new Arg.ADDR(0x8000_0001L).encode(Game.SPM),
            (int)0x8000_0001L
        );
        assertEquals(
            new Arg.ADDR(0x9000_0000L).encode(Game.SPM),
            (int)0x9000_0000L
        );
        assertEquals(
            new Arg.ADDR(0x9000_0001L).encode(Game.SPM),
            (int)0x9000_0001L
        );

        assertEquals(new Arg.FLOAT(0.0f).encode(Game.SPM), (int)0xf1b1e400L);
        assertEquals(new Arg.FLOAT(1.0f).encode(Game.SPM), (int)0xf1b1e800L);

        assertEquals(new Arg.UF(0).encode(Game.SPM), (int)0xf37ba780L);
        assertEquals(new Arg.UF(1).encode(Game.SPM), (int)0xf37ba781L);

        assertEquals(new Arg.UW(0).encode(Game.SPM), (int)0xf4acd480L);
        assertEquals(new Arg.UW(1).encode(Game.SPM), (int)0xf4acd481L);

        assertEquals(new Arg.GSW(0).encode(Game.SPM), (int)0xf5de0180L);
        assertEquals(new Arg.GSW(1).encode(Game.SPM), (int)0xf5de0181L);

        assertEquals(new Arg.LSW(0).encode(Game.SPM), (int)0xf70f2e80L);
        assertEquals(new Arg.LSW(1).encode(Game.SPM), (int)0xf70f2e81L);

        assertEquals(new Arg.GSWF(0).encode(Game.SPM), (int)0xf8405b80L);
        assertEquals(new Arg.GSWF(1).encode(Game.SPM), (int)0xf8405b81L);

        assertEquals(new Arg.LSWF(0).encode(Game.SPM), (int)0xf9718880L);
        assertEquals(new Arg.LSWF(1).encode(Game.SPM), (int)0xf9718881L);

        assertEquals(new Arg.GF(0).encode(Game.SPM), (int)0xfaa2b580L);
        assertEquals(new Arg.GF(1).encode(Game.SPM), (int)0xfaa2b581L);

        assertEquals(new Arg.LF(0).encode(Game.SPM), (int)0xfbd3e280L);
        assertEquals(new Arg.LF(1).encode(Game.SPM), (int)0xfbd3e281L);

        assertEquals(new Arg.GW(0).encode(Game.SPM), (int)0xfd050f80L);
        assertEquals(new Arg.GW(1).encode(Game.SPM), (int)0xfd050f81L);

        assertEquals(new Arg.LW(0).encode(Game.SPM), (int)0xfe363c80L);
        assertEquals(new Arg.LW(1).encode(Game.SPM), (int)0xfe363c81L);

        assertEquals(new Arg.INT(-1).encode(Game.SPM), -1);
        assertEquals(new Arg.INT(0).encode(Game.SPM), 0);
        assertEquals(new Arg.INT(1).encode(Game.SPM), 1);

        assertEquals((new Arg.NONE()).encode(Game.SPM), (int)0xefe82080L);
    }

    @Test
    void round_trip() throws BadEvtException {
        for (long i = 0x8000_0000L; i < 0x8180_0000L; i++) {
            Arg before = new Arg.ADDR(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (long i = 0x9000_0000L; i < 0x9400_0000L; i++) {
            Arg before = new Arg.ADDR(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 1; i < 10; i++) {
            Arg before = new Arg.FLOAT(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < 10; i++) {
            Arg before = new Arg.UF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < 10; i++) {
            Arg before = new Arg.UW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.GSW_COUNT; i++) {
            Arg before = new Arg.GSW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.LSW_COUNT; i++) {
            Arg before = new Arg.LSW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.GSWF_COUNT; i++) {
            Arg before = new Arg.GSWF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.LSWF_COUNT; i++) {
            Arg before = new Arg.LSWF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.GF_COUNT; i++) {
            Arg before = new Arg.GF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.LF_COUNT; i++) {
            Arg before = new Arg.LF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.GW_COUNT; i++) {
            Arg before = new Arg.GW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = 0; i < Arg.LW_COUNT; i++) {
            Arg before = new Arg.LW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        for (int i = -10; i < 10; i++) {
            Arg before = new Arg.INT(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
        }

        Arg before = new Arg.NONE();
        assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM), false));
    }

    // A decode->encode round trip is not guaranteed due to float rounding

    // TODO: test strict mode
}
