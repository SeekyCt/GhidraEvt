package ghidraevt;

import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import jevt.Instr;

public class DisassembleData {
	private final Program program;
	private final EvtScript script;
	private final ProgramLocation location;
	private final DisassembleResults disassembleResults;
	private final String message;
	private final ViewerPosition viewerPosition;

	public DisassembleData(Program program, EvtScript script, ProgramLocation location,
			DisassembleResults disassembleResults, String errorMessage, ViewerPosition viewerPosition) {
		this.program = program;
		this.script = script;
		this.location = location;
		this.disassembleResults = disassembleResults;
		this.message = errorMessage;
		this.viewerPosition = viewerPosition;
	}

    public boolean hasDisassembleResults() {
		if (disassembleResults == null) {
			return false;
		}
		return disassembleResults.getDocroot() != null;
	}

	public boolean isValid() {
		return disassembleResults != null && disassembleResults.isValid();
	}

	public DisassembleResults getDisassembleResults() {
		return disassembleResults;
	}

	public Program getProgram() {
		return program;
	}

	public EvtScript getScript() {
		return script;
	}

	public ProgramLocation getLocation() {
		return location;
	}

	public List<Instr> getDocroot() {
		if (disassembleResults == null) {
			return null;
		}
		return disassembleResults.getDocroot();
	}

	public String getErrorMessage() {
		if (message != null) {
			return message;
		}
		if (location == null || location.getAddress() == null) {
			return "No data";
		}
		if (disassembleResults != null) {
			String err = disassembleResults.getErrorMessage();
			if (err != null) {
				return err;
			}
		}
		return "Unknown Error";
	}

	public boolean contains(ProgramLocation programLocation) {
		if (!hasDisassembleResults()) {
			return false;
		}
		if (programLocation.getProgram() != getProgram()) {
			return false;
		}
		Address address = programLocation.getAddress();
		if (address == null) {
			return false;
		}

		Address startAddress = script.getStartAddress();
		return startAddress.compareTo(address) < 0 &&
			address.subtract(startAddress) < disassembleResults.bytesSize();
	}

	public AddressSpace getFunctionSpace() {
		return location.getAddress().getAddressSpace();
	}

	public ViewerPosition getViewerPosition() {
		return viewerPosition;
	}
}
