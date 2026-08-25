package ghidraevt;

import java.util.Objects;

import ghidra.program.model.address.Address;

/**
 * Analogue of Ghidra's Function class, without requiring a Data to be defined
 */
public class EvtScript {
    private final Address startAddress;
    
    public EvtScript(Address startAddress) {
        this.startAddress = Objects.requireNonNull(startAddress);
    }

    public Address getStartAddress() {
        return startAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((startAddress == null) ? 0 : startAddress.hashCode());
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
        EvtScript other = (EvtScript) obj;
        if (startAddress == null) {
            if (other.startAddress != null)
                return false;
        }
        else if (!startAddress.equals(other.startAddress))
            return false;
        return true;
    }
}
