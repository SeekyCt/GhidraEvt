package ghidraevt;

import static ghidra.framework.model.DomainObjectEvent.*;
import static ghidra.program.util.ProgramEvent.*;

import java.util.Iterator;

import ghidra.framework.model.*;
import ghidra.program.database.SpecExtension;
import ghidra.program.model.listing.Program;
import ghidra.util.task.SwingUpdateManager;

/**
 * Listener of {@link Program} events for decompiler panels. Program events are buffered using 
 * a {@link SwingUpdateManager} before triggering a new decompile process.
*/
public class EvtProgramListener implements DomainObjectListener {

	private EvtController controller;
	private SwingUpdateManager updater;

	/**
	 * Construct a listener with a callback to be called when a decompile should occur. Program
	 * events are buffered using SwingUpdateManager before the callback is called.
	 * @param controller the EvtController
	 * @param callback the callback for when the decompile should be refreshed.
	 */
	public EvtProgramListener(EvtController controller, Runnable callback) {
		this(controller, new SwingUpdateManager(500, 5000, callback));
	}

	/**
	 * Construct a listener with a SwingUpdateManger that should be kicked for every
	 * program change.
	 * @param controller the EvtController
	 * @param updater A SwingUpdateManger to be kicked as program events are received which will
	 * eventually trigger a decompile refresh.
	 */
	public EvtProgramListener(EvtController controller, SwingUpdateManager updater) {
		this.controller = controller;
		this.updater = updater;
	}

	@Override
	public void domainObjectChanged(DomainObjectChangedEvent ev) {
		// Check for events that signal that a decompiler process' data is stale
		// and if so force a new process to be spawned
		if (ev.contains(MEMORY_BLOCK_ADDED, MEMORY_BLOCK_REMOVED, RESTORED)) {
			controller.resetDecompiler();
		}
		else if (ev.contains(DomainObjectEvent.PROPERTY_CHANGED)) {
			Iterator<DomainObjectChangeRecord> iter = ev.iterator();
			while (iter.hasNext()) {
				DomainObjectChangeRecord record = iter.next();
				if (record.getEventType() == DomainObjectEvent.PROPERTY_CHANGED) {
					if (record.getOldValue() instanceof String) {
						String value = (String) record.getOldValue();
						if (value.startsWith(SpecExtension.SPEC_EXTENSION)) {
							controller.resetDecompiler();
							break;
						}
					}
				}
			}
		}

		updater.update();
	}

	public void dispose() {
		updater.dispose();
	}
}
