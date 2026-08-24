package ghidraevt;

import java.util.List;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import jevt.Instr;

public class EvtData {
	private final Program program;
	private final Data data;
	private final ProgramLocation location;
	private final EvtResults evtResults;
	private final String message;
	// private final ViewerPosition viewerPosition;
    // private Address startAddress;

	public EvtData(Program program, Data data, ProgramLocation location,
			EvtResults evtResults, String errorMessage) {
		this.program = program;
		this.data = data;
		this.location = location;
		this.evtResults = evtResults;
		this.message = errorMessage;
		// this.viewerPosition = viewerPosition;
	}


    public boolean hasDecompileResults() {
		if (evtResults == null) {
			return false;
		}
		return evtResults.getCCodeMarkup() != null;
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

	public Data getData() {
		return data;
	}

	public ProgramLocation getLocation() {
		return location;
	}

	public List<Instr> getCCodeMarkup() {
		if (evtResults == null) {
			return null;
		}
		return evtResults.getCCodeMarkup();
	}

	public String getErrorMessage() {
		if (message != null) {
			return message;
		}
		if (data == null) {
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

        return data.contains(address);
	}

	public AddressSpace getFunctionSpace() {
		return data.getAddress().getAddressSpace();
	}

	// public ViewerPosition getViewerPosition() {
	// 	return viewerPosition;
	// }

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
