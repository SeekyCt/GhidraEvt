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
 * Modified from ghidra/app/plugin/core/decompile/actions/FindAction.java to work on evt scripts
 */
package ghidraevt.action;

import java.awt.event.KeyEvent;

import org.apache.commons.lang3.StringUtils;

import docking.DockingUtils;
import docking.action.*;
import docking.widgets.FindDialog;
import ghidraevt.component.EvtPanel;

public class FindAction extends AbstractEvtAction {
	private EvtFindDialog findDialog;

	public FindAction() {
		super("Find");
		setPopupMenuData(new MenuData(new String[] { "Find..." }, "Evt Disassembler"));
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_F, DockingUtils.CONTROL_KEY_MODIFIER_MASK));
		setEnabled(true);
	}

	@Override
	public KeyBindingType getKeyBindingType() {
		return KeyBindingType.SHARED;
	}

	@Override
	public void dispose() {
		if (findDialog != null) {
			findDialog.dispose();
		}
		super.dispose();
	}

	protected FindDialog getFindDialog(EvtPanel evtPanel) {
		if (findDialog == null) {
			findDialog = new EvtFindDialog(evtPanel);
		}
		return findDialog;
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		return true;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		EvtPanel evtPanel = context.getEvtPanel();
		FindDialog dialog = getFindDialog(evtPanel);
		String text = evtPanel.getSelectedText();
		if (text == null) {
			text = evtPanel.getHighlightedText();

			// note: if we decide to grab the text under the cursor, then use
			// text = decompilerPanel.getTextUnderCursor();
		}

		if (!StringUtils.isBlank(text)) {
			dialog.setSearchText(text);
		}

		if (dialog.isShowing()) {
			dialog.toFront();
			return;
		}

		// show over the root frame, so the user can still see the Decompiler window
		context.getTool().showDialog(dialog);
	}
}
