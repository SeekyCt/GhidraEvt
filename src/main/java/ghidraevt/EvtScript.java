package ghidraevt;

import java.util.Objects;

import ghidra.program.model.address.Address;

/**
 * Analogue of Ghidra's Function class, without requiring a Data to be defined
 */
public class EvtScript {
    private final Address startAddress;
    private Address endAddress;
    
    public EvtScript(Address startAddress) {
        this.startAddress = Objects.requireNonNull(startAddress);
        this.endAddress = null;
    }

    public Address getStartAddress() {
        return startAddress;
    }

    public Address getEndAddress() {
        if (endAddress == null)
            throw new IllegalAccessError("End address is undefined");
        return endAddress;
    }

    public void setEndAddress(Address endAddress) {
        if (this.endAddress != null)
            throw new IllegalAccessError("End address already defined");

        this.endAddress = endAddress;
    }
    
    public boolean contains(Address address) {
        return getStartAddress().compareTo(address) <= 0 && address.compareTo(getEndAddress()) < 0;
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
