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
 * Modified from ghidra/app/plugin/core/decompile/actions/HighlightDefinedUseAction.java to work on
 * evt scipts
 */
package ghidraevt.action;

import java.util.Set;

import docking.action.MenuData;
import ghidraevt.component.EvtPanel;
import ghidraevt.token.EvtToken;
import ghidraevt.token.EvtVariableToken;
import jevt.Arg;

/*
 * The basic infrastructure for this class to exist is done, but until more advanced variable
 * analysis is added it has no meaningful difference from the middle-mouse highlight, so is
 * disabled for now.
 */
public class EvtHighlightDefinedUseAction extends AbstractEvtAction {
	public EvtHighlightDefinedUseAction() {
		super("Highlight Defined Use");
		setPopupMenuData(new MenuData(new String[] { "Highlight", "Def-use" }, "Evt Disassembler"));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		EvtToken tokenAtCursor = context.getTokenAtCursor();
		return tokenAtCursor instanceof EvtVariableToken;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		EvtToken tokenAtCursor = context.getTokenAtCursor();
		if (!(tokenAtCursor instanceof EvtVariableToken)) {
			return;
		}
		Arg.Variable varnode = ((EvtVariableToken) tokenAtCursor).getVar();

		EvtPanel evtPanel = context.getEvtPanel();
		evtPanel.clearPrimaryHighlights();

		Set<Arg.Variable> varnodes = Set.of(varnode);
		EvtToken defToken = tokenAtCursor; // TODO
		EvtSliceHighlightColorProvider colorProvider =
			new EvtSliceHighlightColorProvider(evtPanel, varnodes, varnode, defToken);
		evtPanel.addHighlights(colorProvider);

	}
}
