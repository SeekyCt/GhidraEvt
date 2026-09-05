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
 * Modified from ghidra/app/plugin/core/decompile/actions/SetSecondaryHighlightAction.java to work
 * on evt scripts
 */
package ghidraevt.action;

import docking.action.MenuData;
import ghidraevt.token.EvtToken;

/**
 * Sets the secondary highlight on the selected token
 * 
 * @see EvtHighlightController
 */
public class EvtSetSecondaryHighlightAction extends EvtAbstractSetSecondaryHighlightAction {

	public static String NAME = "Set Secondary Highlight";

	public EvtSetSecondaryHighlightAction() {
		super(NAME);

		setPopupMenuData(
			new MenuData(new String[] { "Secondary Highlight", "Set Highlight" }, "Evt Disassembler"));
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {

		EvtToken token = context.getTokenAtCursor();
		context.getEvtPanel().addSecondaryHighlight(token);
	}
}
