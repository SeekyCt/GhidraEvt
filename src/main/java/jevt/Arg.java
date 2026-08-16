package jevt;

public sealed interface Arg permits
    Arg.ADDR, Arg.FLOAT, Arg.UF, Arg.UW, Arg.GSW, Arg.LSW,
    Arg.GSWF, Arg.LSWF, Arg.GF, Arg.LF, Arg.GW, Arg.LW, Arg.INT,
    Arg.NONE {

    record ADDR(long value) implements Arg {
        @Override
        public final String toString() {
            return "ADDR[value=" + Long.toHexString(value) + "]";
        }
    }
    record FLOAT(float value) implements Arg {}
    record UF(int id) implements Arg {}
    record UW(int id) implements Arg {}
    record GSW(int id) implements Arg {}
    record LSW(int id) implements Arg {}
    record GSWF(int id) implements Arg {}
    record LSWF(int id) implements Arg {}
    record GF(int id) implements Arg {}
    record LF(int id) implements Arg {}
    record GW(int id) implements Arg {}
    record LW(int id) implements Arg {}
    record INT(int value) implements Arg {}
    record NONE() implements Arg {}

    public final int GSW_COUNT = 2048;
    public final int LSW_COUNT = 1024;
    public final int GSWF_COUNT = 8192;
    public final int LSWF_COUNT = 512;
    public final int GF_COUNT = 96;
    public final int LF_COUNT = 96;
    public final int GW_COUNT = 32;
    public final int LW_COUNT = 16;

    public final int ADDR_MAX = -290000000;
    public final int FLOAT_MAX = -220000000;
    public final int UF_MAX = -200000000;
    public final int UW_MAX = -180000000;
    public final int GSW_MAX = -160000000;
    public final int LSW_MAX = -140000000;
    public final int GSWF_MAX = -120000000;
    public final int LSWF_MAX = -100000000;
    public final int GF_MAX = -80000000;
    public final int LF_MAX = -60000000;
    public final int GW_MAX = -40000000;
    public final int LW_MAX = -20000000;

    public final float FLOAT_SCALE = 1024.0f;

    public final int NONE_BASE_TTYD = -250000000;
    public final int NONE_BASE_SPM = -270000000;
    public final int FLOAT_BASE_TTYD = -230000000;
    public final int FLOAT_BASE_SPM = -240000000;
    public final int UF_BASE = -210000000;
    public final int UW_BASE = -190000000;
    public final int GSW_BASE = -170000000;
    public final int LSW_BASE = -150000000;
    public final int GSWF_BASE = -130000000;
    public final int LSWF_BASE = -110000000;
    public final int GF_BASE = -90000000;
    public final int LF_BASE = -70000000;
    public final int GW_BASE = -50000000;
    public final int LW_BASE = -30000000;

    public static int FLOAT_BASE(Game game) {
        return switch (game) {
            case Game.TTYD -> FLOAT_BASE_TTYD;
            case Game.SPM -> FLOAT_BASE_SPM;
        };
    }

    public static int NONE_BASE(Game game) {
        return switch (game) {
            case Game.TTYD -> NONE_BASE_TTYD;
            case Game.SPM -> NONE_BASE_SPM;
        };
    }

    public static float check_float(Game game, int val) {
        if (val <= FLOAT_MAX) {
            return (val - FLOAT_BASE(game)) / FLOAT_SCALE;
        } else {
            return val;
        }
    }

    public static int change_float(Game game, float val) {
        return (int) (val * FLOAT_SCALE) + FLOAT_BASE(game);
    }


    public static Arg decode(Game game, int val) {
        if (val == NONE_BASE(game)) {
            return new Arg.NONE();
        } else if (val <= ADDR_MAX) {
            return new Arg.ADDR(Integer.toUnsignedLong(val));
        } else if (val <= FLOAT_MAX) {
            return new Arg.FLOAT(check_float(game, val));
        } else if (val <= UF_MAX) {
            return new Arg.UF((val - UF_BASE));
        } else if (val <= UW_MAX) {
            return new Arg.UW((val - UW_BASE));
        } else if (val <= GSW_MAX) {
            return new Arg.GSW((val - GSW_BASE));
        } else if (val <= LSW_MAX) {
            return new Arg.LSW((val - LSW_BASE));
        } else if (val <= GSWF_MAX) {
            return new Arg.GSWF((val - GSWF_BASE));
        } else if (val <= LSWF_MAX) {
            return new Arg.LSWF((val - LSWF_BASE));
        } else if (val <= GF_MAX) {
            return new Arg.GF((val - GF_BASE));
        } else if (val <= LF_MAX) {
            return new Arg.LF((val - LF_BASE));
        } else if (val <= GW_MAX) {
            return new Arg.GW((val - GW_BASE));
        } else if (val <= LW_MAX) {
            return new Arg.LW((val - LW_BASE));
        } else {
            return new Arg.INT(val);
        }
    }

    public default int encode(Game game) {
        return switch (this) {
            case Arg.NONE arg -> NONE_BASE(game);
            case Arg.ADDR arg -> (int) arg.value;
            case Arg.FLOAT arg -> change_float(game, arg.value);
            case Arg.UF arg -> arg.id + UF_BASE;
            case Arg.UW arg -> arg.id + UW_BASE;
            case Arg.GSW arg -> arg.id + GSW_BASE;
            case Arg.LSW arg -> arg.id + LSW_BASE;
            case Arg.GSWF arg -> arg.id + GSWF_BASE;
            case Arg.LSWF arg -> arg.id + LSWF_BASE;
            case Arg.GF arg -> arg.id + GF_BASE;
            case Arg.LF arg -> arg.id + LF_BASE;
            case Arg.GW arg -> arg.id + GW_BASE;
            case Arg.LW arg -> arg.id + LW_BASE;
            case Arg.INT arg -> arg.value;
        };
    }
}
