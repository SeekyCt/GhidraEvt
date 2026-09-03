/* ###
 * IP: GHIDRA
 *
 * Copyright 2026 SeekyCt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified from ghidra/app/plugin/core/decompile/actions/DecompilerSearchResults.java to work on
 * evt scripts
 */
package ghidraevt.action;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import docking.widgets.SearchLocation;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.search.SearchResults;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import ghidra.util.worker.Worker;
import ghidraevt.component.EvtController;
import ghidraevt.component.EvtPanel;
import ghidraevt.component.EvtScript;

public class EvtSearchResults extends SearchResults {
	// the location when the search was performed; used to know when the script has changed
	private ProgramLocation programLocation;
	private EvtPanel evtPanel;
	private String searchText;
	private List<SearchLocation> searchLocations;
	private Map<Integer, List<EvtSearchLocation>> locationsByLine;
	private TreeMap<LinePosition, EvtSearchLocation> matchesByPosition = new TreeMap<>();

	private EvtSearchLocation activeLocation;

	EvtSearchResults(Worker worker, EvtPanel evtPanel, String searchText,
			List<SearchLocation> searchLocations) {
		super(worker);
		this.evtPanel = evtPanel;
		this.searchText = searchText;

		this.searchLocations = searchLocations;
		this.programLocation = evtPanel.getCurrentLocation();

		for (SearchLocation location : searchLocations) {
			int line = location.getLineNumber();
			int col = location.getStartIndexInclusive();
			LinePosition lp = new LinePosition(line, col);
			EvtSearchLocation esl = (EvtSearchLocation) location;
			matchesByPosition.put(lp, esl);
		}
	}

	@Override
	public String getName() {
		EvtController controller = evtPanel.getController();
		EvtScript script = controller.getScript();
		return script.getName() + "()";
	}

	ProgramLocation getDisassembleLocation() {
		return programLocation;
	}

	boolean isInvalid(String otherSearchText) {
		if (isDifferentScript()) {
			return true;
		}
		return !searchText.equals(otherSearchText);
	}

	@Override
	public boolean isEmpty() {
		return searchLocations.isEmpty();
	}

	@Override
	public List<SearchLocation> getLocations() {
		return searchLocations;
	}

	public Map<Integer, List<EvtSearchLocation>> getLocationsByLine() {
		if (locationsByLine == null) {
			locationsByLine = searchLocations.stream()
					.map(l -> (EvtSearchLocation) l)
					.collect(Collectors.groupingBy(l -> l.getLineNumber()));
		}
		return locationsByLine;
	}

	private boolean isDifferentScript() {
		return !evtPanel.containsLocation(programLocation);
	}

	private boolean isMyScript() {
		return evtPanel.containsLocation(programLocation);
	}

	public EvtSearchLocation getContainingLocation(FieldLocation fieldLocation,
			boolean searchForward) {

		// getNextLocation() will find the next matching location, starting at the given field
		// location.  The next location may or may not actually contain the given field location.
		EvtSearchLocation nextLocation = getNextLocation(fieldLocation, searchForward);
		if (nextLocation != null && nextLocation.contains(fieldLocation)) {
			return nextLocation;
		}
		return null;
	}

	@Override
	public EvtSearchLocation getActiveLocation() {
		return activeLocation;
	}

	private void installSearchResults() {
		if (isDifferentScript()) {
			return; // a different script was disassembled while we were running
		}
		evtPanel.setSearchResults(this);
	}

	private void clearSearchResults() {
		evtPanel.clearSearchResults(this);
	}

	public void disassemblerUpdated() {
		// The disassembler has updated.  It may have been upon our request.  If not, deactivate.
		if (isDifferentScript()) {
			deactivate();
		}
	}

	@Override
	public void deactivate() {
		FindJob job = new SwingJob(this::clearSearchResults);
		runJob(job);
	}

	@Override
	public void activate() {
		FindJob job = createActivationJob().thenRunSwing(this::installSearchResults);
		runJob(job);
	}

	@Override
	public void setActiveLocation(SearchLocation location) {

		if (activeLocation == location) {
			return;
		}

		activeLocation = (EvtSearchLocation) location;
		if (location == null) {
			return;
		}

		// activate() will set the active search location
		activate();
	}

	private ActivationJob createActivationJob() {
		if (isMyScript()) {
			return createFinishedActivationJob(); // nothing to do
		}

		return (ActivationJob) new ActivateScriptJob()
				.thenWait(this::isMyScript, Duration.ofSeconds(5));
	}

	protected ActivationJob createFinishedActivationJob() {
		return new ActivationJob();
	}

	@Override
	public void dispose() {
		setActiveLocation(null);
		evtPanel.clearSearchResults(this);
		searchLocations.clear();
	}

	EvtSearchLocation getNextLocation(FieldLocation startLocation,
			boolean searchForward) {

		Entry<LinePosition, EvtSearchLocation> entry;
		int line = startLocation.getIndex().intValue() + 1; // +1 for zero based
		int col = startLocation.getCol();
		LinePosition lp = new LinePosition(line, col);
		if (searchForward) {
			entry = matchesByPosition.ceilingEntry(lp);
		}
		else {
			entry = matchesByPosition.floorEntry(lp);
		}

		if (entry == null) {
			return null; // no more matches in the current direction
		}

		return entry.getValue();
	}

//=================================================================================================
// Inner Classes
//=================================================================================================	

	private class ActivateScriptJob extends ActivationJob {
		@Override
		protected void doRun(TaskMonitor monitor) throws CancelledException {
			if (isMyScript()) {
				return; // nothing to do
			}

			EvtController controller = evtPanel.getController();
			Program program = programLocation.getProgram();
			controller.refreshDisplay(program, programLocation, null);
		}
	}

	private record LinePosition(int line, int col) implements Comparable<LinePosition> {

		@Override
		public int compareTo(LinePosition other) {

			int result = line - other.line;
			if (result != 0) {
				return result;
			}

			return col - other.col;
		}
	}

}
