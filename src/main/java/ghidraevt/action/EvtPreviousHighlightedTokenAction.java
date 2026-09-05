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
 * Modified from ghidra/app/plugin/core/decompile/actions/PreviousHighlightedTokenAction.java 
 * work with evt scripts
 */
package ghidraevt.action;

import java.util.List;

import docking.action.KeyBindingData;
import docking.action.MenuData;
import docking.widgets.fieldpanel.field.Field;
import ghidraevt.component.EvtPanel;
import ghidraevt.component.EvtTextField;
import ghidraevt.highlight.EvtHighlightToken;
import ghidraevt.highlight.EvtTokenHighlights;
import ghidraevt.token.EvtToken;
import ghidraevt.token.EvtTokenIterator;

/**
 * An action to navigate to the previous token highlighted by the user via the middle-mouse.
 */
public class EvtPreviousHighlightedTokenAction extends AbstractEvtAction {

	public EvtPreviousHighlightedTokenAction() {
		super("Previous Highlighted Token");

		setPopupMenuData(new MenuData(new String[] { "Previous Highlight" }, "Evt Disassembler"));
		setKeyBindingData(new KeyBindingData("Ctrl comma"));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		if (context.getScript() == null) {
			return false;
		}
		EvtPanel panel = context.getEvtPanel();
		EvtTokenHighlights highlights = panel.getMiddleMouseHighlights();
		if (highlights != null) {
			return highlights.size() > 1;
		}
		return false;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {

		EvtPanel panel = context.getEvtPanel();
		EvtTokenHighlights highlights = panel.getMiddleMouseHighlights();
		EvtToken cursorToken = context.getTokenAtCursor();
		EvtTokenIterator it = new EvtTokenIterator(cursorToken, false);
		it.next(); // ignore the current token

		if (goToNexToken(panel, it, highlights)) {
			return; // found another token in the current direction
		}

		// this means there are no more occurrences in the current direction; wrap the search
		EvtToken lastToken = getLastToken(panel);
		it = new EvtTokenIterator(lastToken, false);
		goToNexToken(panel, it, highlights);
	}

	private EvtToken getLastToken(EvtPanel panel) {
		List<Field> fields = panel.getFields();
		int lastLine = fields.size();
		Field line = fields.get(lastLine - 1);
		EvtTextField tf = (EvtTextField) line;
		return tf.getLastToken();
	}

	private boolean goToNexToken(EvtPanel panel, EvtTokenIterator it,
			EvtTokenHighlights highlights) {

		while (it.hasNext()) {
			EvtToken nextToken = it.next();
			EvtHighlightToken hlToken = highlights.get(nextToken);
			if (hlToken == null) {
				continue;
			}

			EvtToken token = hlToken.getToken();
			panel.goToToken(token);
			return true;
		}

		return false;
	}
}
