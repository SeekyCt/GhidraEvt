package ghidraevt.component;

import ghidra.framework.model.*;
import ghidra.program.model.listing.Program;
import ghidra.util.task.SwingUpdateManager;

/**
 * Listener of {@link Program} events for disassembler panels. Program events are buffered using 
 * a {@link SwingUpdateManager} before triggering a new disassemble process.
*/
public class EvtProgramListener implements DomainObjectListener {

    private SwingUpdateManager updater;

    /**
     * Construct a listener with a SwingUpdateManger that should be kicked for every
     * program change.
     * @param controller the EvtController
     * @param updater A SwingUpdateManger to be kicked as program events are received which will
     * eventually trigger a disassemble refresh.
     */
    public EvtProgramListener(SwingUpdateManager updater) {
        this.updater = updater;
    }

    @Override
    public void domainObjectChanged(DomainObjectChangedEvent ev) {
        updater.update();
    }

    public void dispose() {
        updater.dispose();
    }
}
