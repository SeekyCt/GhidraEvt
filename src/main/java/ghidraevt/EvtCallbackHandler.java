package ghidraevt;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import utility.function.Callback;

public interface EvtCallbackHandler {
    void disassembleDataChanged(DisassembleData data);

    void contextChanged();

    void setStatusMessage(String message);

    void locationChanged(ProgramLocation programLocation);

    void selectionChanged(ProgramSelection programSelection);

    void annotationClicked(AnnotatedTextFieldElement annotation, boolean newWindow);

    void goToAddress(Address addr, boolean newWindow);

    void exportLocation();

    void doWhenNotBusy(Callback c);
}
