package ghidraevt;

import ghidra.app.nav.LocationMemento;
import ghidra.framework.options.SaveState;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import docking.widgets.fieldpanel.support.ViewerPosition;

public class EvtLocationMemento extends LocationMemento {

	private final ViewerPosition viewerPosition;
	
	public EvtLocationMemento(Program program, ProgramLocation location,
			ViewerPosition viewerPosition) {
		super(program, location);
		this.viewerPosition = viewerPosition;
	}
	public EvtLocationMemento(SaveState saveState, Program[] programs) {
		super(saveState, programs);
		int index = saveState.getInt("INDEX", 0);
		int xOffset = saveState.getInt("X_OFFSET", 0);
		int yOffset = saveState.getInt("Y_OFFSET", 0);
		viewerPosition = new ViewerPosition(index, xOffset, yOffset);
	}

	public ViewerPosition getViewerPosition() {
		return viewerPosition;
	}
	
	@Override
	public void saveState(SaveState saveState) {
		super.saveState( saveState );
		saveState.putInt("INDEX", viewerPosition.getIndexAsInt());
		saveState.putInt("X_OFFSET", viewerPosition.getXOffset());
		saveState.putInt("Y_OFFSET", viewerPosition.getYOffset());
	}
}
