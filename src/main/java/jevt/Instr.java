package jevt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class Instr {
    private Opcode opcode;
    private List<Arg> args;

    public Instr(Opcode opcode, List<Arg> args) {
        this.opcode = opcode;
        this.args = args;
    }

    public Opcode opcode() {
        return this.opcode;
    }

    public List<Arg> args() {
        return this.args;
    }

    public static int NARGS_BYTES_SIZE = 2;
    public static int OPCODE_BYTES_SIZE = 2;

    public int bytesSize() {
        return NARGS_BYTES_SIZE + OPCODE_BYTES_SIZE + (Arg.bytesSize() * args.size());
    }

    public static Instr decode(Game game, DataInputStream stream, boolean strict) throws IOException, BadEvtException {
        int nargs = stream.readShort() & 0xffff;
        if (strict && nargs > 0xff) // TODO: check per-instruction too like search.py
            throw new StrictEvtException("Invalid argument count " + nargs);

        // TODO: when strict check indentation validity, paired start-ends, etc

        short opcodeId = stream.readShort();
        Opcode opcode = Opcode.decode(game, opcodeId, strict);

        List<Arg> args = new ArrayList<>(nargs);
        for (int i = 0; i < nargs; i++)
        {
            int argVal = stream.readInt();
            Arg arg = Arg.decode(game, argVal, strict);
            args.add(arg);
        }

        return new Instr(opcode, args);
    }

    public void encode(Game game, DataOutputStream stream) throws IOException {
        short nargs = (short) this.args.size();
        stream.writeShort(nargs);

        short opcode = (short) this.opcode.encode(game);
        stream.writeShort(opcode);

        for (Arg arg : this.args) {
            stream.writeInt(arg.encode(game));
        }
    }

    public static List<Instr> disassemble(Game game, InputStream stream, boolean strict) throws IOException, BadEvtException {
        List<Instr> ret = new ArrayList<>();
        DataInputStream dataStream = new DataInputStream(stream);

        Opcode opcode = Opcode.NEXT;
        while (opcode != Opcode.END_SCRIPT) {
            Instr instr = Instr.decode(game, dataStream, strict);
            opcode = instr.opcode;
            ret.add(instr);
        }

        if (strict) {
            if (ret.size() < 2)
                throw new StrictEvtException("Script is too short");

            // TODO: check for code after return

            // TODO: this was in search.py, but seems like it could be flawed?
            if (ret.get(ret.size()-2).opcode() != Opcode.END_EVT)
                throw new StrictEvtException("Script didn't end with a return");
        }

        return ret;
    }

    public static void assemble(Game game, List<Instr> script, OutputStream stream) throws IOException {
        DataOutputStream dataStream = new DataOutputStream(stream);
        for (Instr instr : script) {
            instr.encode(game, dataStream);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((opcode == null) ? 0 : opcode.hashCode());
        result = prime * result + ((args == null) ? 0 : args.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Instr other = (Instr) obj;
        if (opcode != other.opcode)
            return false;
        if (args == null) {
            if (other.args != null)
                return false;
        }
        else if (!args.equals(other.args))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Instr [opcode=" + opcode + ", args=" + args + "]";
    }
}
