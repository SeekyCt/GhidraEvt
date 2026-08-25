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
    private Address endAddress;

    public MemoryInputStream(MemoryBlock block, Address startAddr, Address endAddress) {
        this.block = block;
        this.addr = startAddr;
        this.endAddress = endAddress;
    }

    public MemoryInputStream(MemoryBlock block, Address startAddr) {
        this(block, startAddr, null);
    }

    public MemoryInputStream(Program program, Address startAddr, Address endAddress) {
        this(program.getMemory().getBlock(startAddr), startAddr, endAddress);
    }

    public MemoryInputStream(Program program, Address startAddr) {
        this(program, startAddr, null);
    }

    @Override
    public int read() throws IOException {
        if (endAddress != null && addr.compareTo(endAddress) >= 0)
            return -1;

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
