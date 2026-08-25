package ghidraevt;

import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import jevt.Instr;

public class EvtData {
	private final Program program;
	private final EvtScript script;
	private final ProgramLocation location;
	private final EvtResults evtResults;
	private final String message;
	private final ViewerPosition viewerPosition;

	public EvtData(Program program, EvtScript script, ProgramLocation location,
			EvtResults evtResults, String errorMessage, ViewerPosition viewerPosition) {
		this.program = program;
		this.script = script;
		this.location = location;
		this.evtResults = evtResults;
		this.message = errorMessage;
		this.viewerPosition = viewerPosition;
	}

    public boolean hasDisassembleResults() {
		if (evtResults == null) {
			return false;
		}
		return evtResults.getDocroot() != null;
	}

	public boolean isValid() {
		return evtResults != null && evtResults.isValid();
	}

	public EvtResults getDecompileResults() {
		return evtResults;
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
		if (evtResults == null) {
			return null;
		}
		return evtResults.getDocroot();
	}

	public String getErrorMessage() {
		if (message != null) {
			return message;
		}
		if (location == null || location.getAddress() == null) {
			return "No data";
		}
		if (evtResults != null) {
			String err = evtResults.getErrorMessage();
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
			address.subtract(startAddress) < evtResults.bytesSize();
	}

	public AddressSpace getFunctionSpace() {
		return location.getAddress().getAddressSpace();
	}

	public ViewerPosition getViewerPosition() {
		return viewerPosition;
	}

//     public EvtData(Program program, Data data, Address startAddress, EvtResults results, String errorMesssage) {
//         this.program = program;
//         this.data = data;
//         this.startAddress = startAddress;
//         this.results = results;
//         this.errorMessage = errorMesssage;
//     }

//     public boolean isError() {
//         return errorMessage != null;
//     }

//     public static EvtData success(Program program, Address startAddress, List<Instr> script) {
//         return new EvtData(program, startAddress, script, null);
//     }

//     public static EvtData empty(String err) {
//         return new EvtData(null, null, null, err);
//     }

//     public static EvtData fail(Program program, Address startAddress, String err) {
//         return new EvtData(program, startAddress, null, err);
//     }
}
