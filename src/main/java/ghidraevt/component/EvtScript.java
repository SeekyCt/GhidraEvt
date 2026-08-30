/* ###
 * IP: GHIDRA
 *
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
 * 
 * Modified from Ghidra's decompiler UI source code to work on evt scripts
 */
package ghidraevt.component;

import java.util.Objects;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;

/**
 * Analogue of Ghidra's Function class, without requiring a Data to be defined
 */
public class EvtScript {
    private Program program;
    private final Address startAddress;
    private Address endAddress;
    
    public EvtScript(Program program, Address startAddress) {
        this.program = program;
        this.startAddress = Objects.requireNonNull(startAddress);
        this.endAddress = null;
    }

    public Program getProgram() {
        return program;
    }

    public String getName() {
        if (startAddress == null)
            return null;

        Symbol symbol = program.getSymbolTable().getPrimarySymbol(startAddress);
        if (symbol == null)
            return null;

        return symbol.getName();
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
