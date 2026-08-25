package ghidraevt;

import java.util.Objects;

import ghidra.framework.options.SaveState;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;

/**
 * The default location handed out when the user clicks inside of the disassembler.
 */
public class DefaultEvtLocation extends ProgramLocation implements EvtLocation {
	private EvtLocationInfo info;

	public DefaultEvtLocation(Program program, Address address, EvtLocationInfo info) {
		super(program, address);
		this.info = info;
	}

	public DefaultEvtLocation() {
		// for restoring from xml
		info = new EvtLocationInfo();
	}

	@Override
	public Address getScriptEntryPoint() {
		return info.getScriptEntryPoint();
	}

	@Override
	public EvtResults getDisassembly() {
		return info.getDisassembly();
	}

	@Override
	public EvtToken getToken() {
		return info.getToken();
	}

	@Override
	public String getTokenName() {
		return info.getTokenName();
	}

	@Override
	public int getLineNumber() {
		return info.getLineNumber();
	}

	@Override
	public int getCharPos() {
		return info.getCharPos();
	}

	@Override
	public void saveState(SaveState ss) {
		super.saveState(ss);
		info.saveState(ss);
	}

	@Override
	public void restoreState(Program p, SaveState ss) {
		super.restoreState(p, ss);
		info.restoreState(p, ss);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = info.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		if (!super.equals(obj)) {
			return false;
		}

		DefaultEvtLocation other = (DefaultEvtLocation) obj;
		return Objects.equals(info, other.info);
	}
}
