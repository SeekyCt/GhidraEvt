package jevt;

public enum Opcode {
    NEXT,
    END_SCRIPT,
    END_EVT,
    LBL,
    GOTO,
    DO,
    WHILE,
    DO_BREAK,
    DO_CONTINUE,
    WAIT_FRM,
    WAIT_MSEC,
    HALT,
    IF_STR_EQUAL,
    IF_STR_NOT_EQUAL,
    IF_STR_SMALL,
    IF_STR_LARGE,
    IF_STR_SMALL_EQUAL,
    IF_STR_LARGE_EQUAL,
    IFF_EQUAL,
    IFF_NOT_EQUAL,
    IFF_SMALL,
    IFF_LARGE,
    IFF_SMALL_EQUAL,
    IFF_LARGE_EQUAL,
    IF_EQUAL,
    IF_NOT_EQUAL,
    IF_SMALL,
    IF_LARGE,
    IF_SMALL_EQUAL,
    IF_LARGE_EQUAL,
    IF_FLAG,
    IF_NOT_FLAG,
    ELSE,
    END_IF,
    SWITCH,
    SWITCHI,
    CASE_EQUAL,
    CASE_NOT_EQUAL,
    CASE_SMALL,
    CASE_LARGE,
    CASE_SMALL_EQUAL,
    CASE_LARGE_EQUAL,
    CASE_ETC,
    CASE_OR,
    CASE_AND,
    CASE_FLAG,
    CASE_END,
    CASE_BETWEEN,
    SWITCH_BREAK,
    END_SWITCH,
    SET,
    SETI,
    SETF,
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    ADDF,
    SUBF,
    MULF,
    DIVF,
    SET_READ,
    READ,
    READ2,
    READ3,
    READ4,
    READ_N,
    SET_READF,
    READF,
    READF2,
    READF3,
    READF4,
    READF_N,
    CLAMP_INT,
    SET_USER_WRK,
    SET_USER_FLG,
    ALLOC_USER_WRK,
    AND,
    ANDI,
    OR,
    ORI,
    SET_FRAME_FROM_MSEC,
    SET_MSEC_FROM_FRAME,
    SET_RAM,
    SET_RAMF,
    GET_RAM,
    GET_RAMF,
    SETR,
    SETRF,
    GETR,
    GETRF,
    USER_FUNC,
    RUN_EVT,
    RUN_EVT_ID,
    RUN_CHILD_EVT,
    DELETE_EVT,
    RESTART_EVT,
    SET_PRI,
    SET_SPD,
    SET_TYPE,
    STOP_ALL,
    START_ALL,
    STOP_OTHER,
    START_OTHER,
    STOP_ID,
    START_ID,
    CHK_EVT,
    INLINE_EVT,
    INLINE_EVT_ID,
    END_INLINE,
    BROTHER_EVT,
    BROTHER_EVT_ID,
    END_BROTHER,
    DEBUG_PUT_MSG,
    DEBUG_MSG_CLEAR,
    DEBUG_PUT_REG,
    DEBUG_NAME,
    DEBUG_REM,
    DEBUG_BP;

    public int encode(Game game) {
        return switch (game) {
            case Game.SPM -> ordinal();
            case Game.TTYD ->  {
                if (this.ordinal() < CLAMP_INT.ordinal())
                    yield this.ordinal();
                else if (this == CLAMP_INT)
                    throw new IllegalArgumentException();
                else
                    yield this.ordinal() - 1;
            }
        };
    }

    public static Opcode decode(Game game, int id, boolean strict) throws BadEvtException {
        if (strict && id == NEXT.ordinal())
            throw new StrictEvtException("Opcode NEXT may not appear in scripts");

        id = switch (game) {
            case Game.SPM -> id;
            case Game.TTYD ->  {
                if (id < CLAMP_INT.ordinal())
                    yield id;
                else
                    yield id + 1;
            }
        };
        try {
            return Opcode.values()[id];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new BadEvtException("Invalid opcode " + id);
        }
    }

    public int indent() {
        return switch (this) {
            case Opcode.SWITCH, Opcode.SWITCHI -> 2;

            case Opcode.DO, Opcode.IF_STR_EQUAL, Opcode.IF_STR_NOT_EQUAL, Opcode.IF_STR_SMALL, Opcode.IF_STR_LARGE,
            Opcode.IF_STR_SMALL_EQUAL, Opcode.IF_STR_LARGE_EQUAL, Opcode.IFF_EQUAL, Opcode.IFF_NOT_EQUAL,
            Opcode.IFF_SMALL, Opcode.IFF_LARGE, Opcode.IFF_SMALL_EQUAL, Opcode.IFF_LARGE_EQUAL, Opcode.IF_EQUAL,
            Opcode.IF_NOT_EQUAL, Opcode.IF_SMALL, Opcode.IF_LARGE, Opcode.IF_SMALL_EQUAL, Opcode.IF_LARGE_EQUAL,
            Opcode.IF_FLAG, Opcode.IF_NOT_FLAG, Opcode.INLINE_EVT, Opcode.INLINE_EVT_ID, Opcode.BROTHER_EVT,
            Opcode.BROTHER_EVT_ID, Opcode.ELSE, Opcode.CASE_EQUAL, Opcode.CASE_NOT_EQUAL, Opcode.CASE_SMALL, Opcode.CASE_LARGE,
            Opcode.CASE_SMALL_EQUAL, Opcode.CASE_LARGE_EQUAL, Opcode.CASE_ETC, Opcode.CASE_OR, Opcode.CASE_AND, Opcode.CASE_FLAG,
            Opcode.CASE_BETWEEN -> 1;

            default -> 0;
        };
    }

    public int unindent() {
        return switch (this) {
            case Opcode.END_SWITCH -> 2;

            case Opcode.WHILE, Opcode.ELSE, Opcode.END_IF, Opcode.CASE_EQUAL, Opcode.CASE_NOT_EQUAL, Opcode.CASE_SMALL, Opcode.CASE_LARGE,
            Opcode.CASE_SMALL_EQUAL, Opcode.CASE_LARGE_EQUAL, Opcode.CASE_ETC, Opcode.CASE_OR, Opcode.CASE_AND, Opcode.CASE_FLAG,
            Opcode.CASE_BETWEEN, Opcode.END_INLINE, Opcode.END_BROTHER -> 1;

            default -> 0;
        };
    }

    public String niceName() {
        return this.name().toLowerCase();
    }

    public String macroName() {
        return this.name();
    }
}
