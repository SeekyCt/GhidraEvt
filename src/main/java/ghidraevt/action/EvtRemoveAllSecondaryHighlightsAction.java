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
 * Modified from ghidra/app/plugin/core/decompile/actions/RemoveAllSecondaryHighlightsAction.java
 * to work on evt scripts
 */
package ghidraevt.action;

import docking.action.MenuData;
import ghidraevt.component.EvtPanel;
import ghidraevt.component.EvtScript;

/**
 * Removes all secondary highlights for the current function
 * 
 * @see EvtHighlightController
 */
public class EvtRemoveAllSecondaryHighlightsAction extends AbstractEvtAction {

	public static final String NAME = "Remove All Secondary Highlights";

	public EvtRemoveAllSecondaryHighlightsAction() {
		super(NAME);

		setPopupMenuData(new MenuData(
			new String[] { "Secondary Highlight", "Remove All Highlights" }, "Decompile"));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		if (context.getScript() == null) {
			return false;
		}

		EvtPanel panel = context.getEvtPanel();
		EvtScript script = context.getScript();
		return panel.hasSecondaryHighlights(script);
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		EvtPanel panel = context.getEvtPanel();
		EvtScript script = context.getScript();
		panel.removeSecondaryHighlights(script);
	}
}
