package ghidraevt;

import java.io.IOException;
import java.io.InputStream;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;

public class MemoryInputStream extends InputStream {
    private MemoryBlock block;
    private Address addr;

    public MemoryInputStream(MemoryBlock block, Address startAddr) {
        this.addr = startAddr;
        this.block = block;
    }

    public MemoryInputStream(Program program, Address startAddr) {
        this(program.getMemory().getBlock(startAddr), startAddr);
    }

    @Override
    public int read() throws IOException {
        int ret;
        try {
            ret = block.getByte(addr) & 0xff;
        }
        catch (MemoryAccessException e) {
            throw new IOException("Failed to read memory at " + addr);
        }

        addr = addr.add(1);
        return ret;
    }

}
