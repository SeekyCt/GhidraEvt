package ghidraevt;

import java.util.List;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import jevt.Instr;

public class EvtData {
	private final Program program;
	private final ProgramLocation location;
	private final EvtResults evtResults;
	private final String message;
	private final ViewerPosition viewerPosition;

	public EvtData(Program program, ProgramLocation location,
			EvtResults evtResults, String errorMessage, ViewerPosition viewerPosition) {
		this.program = program;
		this.location = location;
		this.evtResults = evtResults;
		this.message = errorMessage;
		this.viewerPosition = viewerPosition;
	}


    public boolean hasDecompileResults() {
		if (evtResults == null) {
			return false;
		}
		return evtResults.getScript() != null;
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

	public Address getAddress() {
		if (location != null)
			return location.getAddress();
		else
			return null;
	}

	public ProgramLocation getLocation() {
		return location;
	}

	public List<Instr> getScript() {
		if (evtResults == null) {
			return null;
		}
		return evtResults.getScript();
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
		if (!hasDecompileResults()) {
			return false;
		}
		if (programLocation.getProgram() != getProgram()) {
			return false;
		}
		Address address = programLocation.getAddress();
		if (address == null) {
			return false;
		}

		Address startAddress = location.getAddress();
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
