package jevt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// TODO: TTYD tests

public class ArgTests {
    @Test
    void decode() {
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x8000_0000L),
            new Arg.ADDR(0x8000_0000L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x8000_0001L),
            new Arg.ADDR(0x8000_0001L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x9000_0000L),
            new Arg.ADDR(0x9000_0000L)
        );
        assertEquals(
            Arg.decode(Game.SPM, (int) 0x9000_0001L),
            new Arg.ADDR(0x9000_0001L)
        );

        assertEquals(Arg.decode(Game.SPM, (int)0xf1b1e400L), new Arg.FLOAT(0.0f));
        assertEquals(Arg.decode(Game.SPM, (int)0xf1b1e800L), new Arg.FLOAT(1.0f));

        assertEquals(Arg.decode(Game.SPM, (int)0xf37ba780L), new Arg.UF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf37ba781L), new Arg.UF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf4acd480L), new Arg.UW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf4acd481L), new Arg.UW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf5de0180L), new Arg.GSW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf5de0181L), new Arg.GSW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf70f2e80L), new Arg.LSW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf70f2e81L), new Arg.LSW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf8405b80L), new Arg.GSWF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf8405b81L), new Arg.GSWF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xf9718880L), new Arg.LSWF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xf9718881L), new Arg.LSWF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfaa2b580L), new Arg.GF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfaa2b581L), new Arg.GF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfbd3e280L), new Arg.LF(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfbd3e281L), new Arg.LF(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfd050f80L), new Arg.GW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfd050f81L), new Arg.GW(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xfe363c80L), new Arg.LW(0));
        assertEquals(Arg.decode(Game.SPM, (int)0xfe363c81L), new Arg.LW(1));

        assertEquals(Arg.decode(Game.SPM, -1), new Arg.INT(-1));
        assertEquals(Arg.decode(Game.SPM, 0), new Arg.INT(0));
        assertEquals(Arg.decode(Game.SPM, 1), new Arg.INT(1));

        assertEquals(Arg.decode(Game.SPM, (int)0xefe82080L), new Arg.NONE());
    }

    @Test
    void encode() {
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
    void round_trip() {
        for (long i = 0x8000_0000L; i < 0x8180_0000L; i++) {
            Arg before = new Arg.ADDR(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (long i = 0x9000_0000L; i < 0x9400_0000L; i++) {
            Arg before = new Arg.ADDR(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 1; i < 10; i++) {
            Arg before = new Arg.FLOAT(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < 10; i++) {
            Arg before = new Arg.UF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < 10; i++) {
            Arg before = new Arg.UW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.GSW_COUNT; i++) {
            Arg before = new Arg.GSW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.LSW_COUNT; i++) {
            Arg before = new Arg.LSW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.GSWF_COUNT; i++) {
            Arg before = new Arg.GSWF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.LSWF_COUNT; i++) {
            Arg before = new Arg.LSWF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.GF_COUNT; i++) {
            Arg before = new Arg.GF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.LF_COUNT; i++) {
            Arg before = new Arg.LF(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.GW_COUNT; i++) {
            Arg before = new Arg.GW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = 0; i < Arg.LW_COUNT; i++) {
            Arg before = new Arg.LW(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        for (int i = -10; i < 10; i++) {
            Arg before = new Arg.INT(i);
            assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
        }

        Arg before = new Arg.NONE();
        assertEquals(before, Arg.decode(Game.SPM, before.encode(Game.SPM)));
    }

    // A decode->encode round trip is not guaranteed due to float rounding    
}
