package ghidraevt.location;

import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.DisassembleResults;
import ghidraevt.token.EvtToken;

/**
 * Represents a location in the disassembler. This interface allows the disassembler to subclass more
 * general {@link ProgramLocation}s while adding more detailed disassembler information.
 */
public interface EvtLocation {
    public Address getScriptEntryPoint();

    /**
     * Results from the disassembly
     * 
     * @return Results object. null if there are no results attached to this location
     */
    public DisassembleResults getDisassembly();

    /**
     * C text token at the current cursor location
     * 
     * @return token at this location, could be null if there are no disassembly results
     */
    public EvtToken getToken();

    /**
     * {@return the name of the token for the current location}
     */
    public String getTokenName();

    /**
     * {@return the line number}
     */
    public int getLineNumber();

    /**
     * {@return the character position}
     */
    public int getCharPos();
}
