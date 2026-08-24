package ghidraevt;

import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;

/**
 * Represents a location in the Decompiler.  This interface allows the Decompiler to subclass more
 * general {@link ProgramLocation}s while adding more detailed Decompiler information.
 */
public interface EvtLocation {

	public Address getFunctionEntryPoint();

	/**
	 * Results from the decompilation
	 * 
	 * @return C-AST, DFG, and CFG object. null if there are no results attached to this location
	 */
	public EvtResults getDecompile();

	/**
	 * C text token at the current cursor location
	 * 
	 * @return token at this location, could be null if there are no decompiler results
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
