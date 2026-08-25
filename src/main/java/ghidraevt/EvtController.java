package ghidraevt;

import java.awt.event.MouseEvent;
import java.io.File;

import docking.widgets.fieldpanel.support.ViewerPosition;
import ghidra.framework.plugintool.ServiceProvider;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import utility.function.Callback;

/**
 * Coordinates the interactions between the EvtProvider, EvtPanel, and the EvtManager
*/
public class EvtController {
	private ServiceProvider serviceProvider;
	private EvtPanel evtPanel;
	private EvtManager evtMgr;
	private final EvtCallbackHandler callbackHandler;
	private EvtData currentEvtData;
	private ProgramSelection currentSelection;

	public EvtController(ServiceProvider serviceProvider, EvtCallbackHandler handler,
        EvtOptions options, EvtClipboardProvider clipboard) {
		this.serviceProvider = serviceProvider;
		this.callbackHandler = handler;
		evtMgr = new EvtManager(this, options);
		evtPanel = new EvtPanel(this, options, clipboard);

		evtPanel.setHoverMode(true);
	}

	public ServiceProvider getServiceProvider() {
		return serviceProvider;
	}

	public EvtPanel getEvtPanel() {
		return evtPanel;
	}

//==================================================================================================
//  Methods call by the provider
//==================================================================================================

	/**
	 * Called by the provider when the provider is disposed. Once dispose is called, it should never
	 * be used again.
	 */
	public void dispose() {
		evtPanel.dispose();
	}

	/**
	 * clears all internal state and releases all resources. Called when the provider is no longer
	 * visible or the currently displayed program is closed.
	 */
	public void clear() {
		currentSelection = null;
		setEvtData(new EmptyEvtData("No Function"));
	}

	/**
	 * Shows the script containing the given location in the evtPanel. Also, positions the
	 * evtPanel's cursor to the closest equivalent position. If the evtPanel is already displaying
	 * the function, then only the cursor is repositioned. To force a re-disassemble use
	 * {@link #refreshDisplay(Program, ProgramLocation, File)}.
	 *
	 * @param program the program for the given location
	 * @param location the location containing the script to be displayed and the location in that
	 *            script to position the cursor.
	 * @param viewerPosition the viewer position
	 */
	public void display(Program program, ProgramLocation location, ViewerPosition viewerPosition) {

		if (evtPanel.containsLocation(location)) {
			evtPanel.setLocation(location, viewerPosition);
			return;
		}

		EvtResults results = evtMgr.disassemble(program, location, viewerPosition);

		setEvtData(
			new EvtData(program, results.getScript(), location, results, null, viewerPosition)
		);
			
	}

	public void setSelection(ProgramSelection selection) {
		evtPanel.setSelection(selection);
	}

	/**
	 * Sets new options and triggers a new disassemble.
	 * 
	 * @param options the options
	 */
	public void setOptions(EvtOptions options) {
		evtMgr.setOptions(options);
		evtPanel.optionsChanged(options);
	}

	public void setMouseNavigationEnabled(boolean enabled) {
		evtPanel.setMouseNavigationEnabled(enabled);
	}

//==================================================================================================
//  Methods call by the EvtManager
//==================================================================================================

	/**
	 * Called by the EvtManager to update the currently displayed EvtData
	 * 
	 * @param EvtData the new data
	 */
	public void setEvtData(EvtData evtData) {
		currentEvtData = evtData;
		evtPanel.setEvtData(evtData);
		evtPanel.setSelection(currentSelection);
		callbackHandler.evtDataChanged(evtData);
	}

//==================================================================================================
//  Methods called by actions and other miscellaneous classes
//==================================================================================================

	public void doWhenNotBusy(Callback c) {
		callbackHandler.doWhenNotBusy(c);
	}

	/**
	 * Always disassembles the function containing the given location before positioning the
	 * evtPanel's cursor to the closest equivalent position.
	 * 
	 * @param program the program for the given location
	 * @param location the location containing the function to be displayed and the location in that
	 *            function to position the cursor.
	 * @param debugFile the debug file
	 */
	public void refreshDisplay(Program program, ProgramLocation location, File debugFile) {
		evtMgr.disassemble(program, location, null);
	}

	public boolean hasDisassembleResults() {
		if (currentEvtData != null) {
			return currentEvtData.hasDisassembleResults();
		}
		return false;
	}

	public void setStatusMessage(String message) {
		callbackHandler.setStatusMessage(message);
	}

	public Program getProgram() {
		if (currentEvtData != null) {
			return currentEvtData.getProgram();
		}
		return null;
	}

	public Address getAddress() {
		if (currentEvtData != null) {
			return currentEvtData.getScript().getStartAddress();
		}
		return null;
	}
	public ProgramLocation getLocation() {
		if (currentEvtData != null) {
			return currentEvtData.getLocation();
		}
		return null;
	}

	void locationChanged(ProgramLocation programLocation) {
		callbackHandler.locationChanged(programLocation);
	}

	void selectionChanged(ProgramSelection programSelection) {
		currentSelection = programSelection;
		callbackHandler.selectionChanged(programSelection);
	}

	void annotationClicked(AnnotatedTextFieldElement annotation, MouseEvent event,
			boolean newWindow) {
		callbackHandler.annotationClicked(annotation, newWindow);
	}

	void goToAddress(Address addr, boolean newWindow) {
		callbackHandler.goToAddress(addr, newWindow);
	}

	public EvtData getEvtData() {
		return currentEvtData;
	}

	public void exportLocation() {
		callbackHandler.exportLocation();
	}
}
