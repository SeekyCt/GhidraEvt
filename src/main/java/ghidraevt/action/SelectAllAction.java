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
 * Modified from ghidra/app/plugin/core/decompile/actions/SelectAllAction.java to work on evt
 * scripts
 */
package ghidraevt.action;

import java.awt.event.KeyEvent;

import docking.ActionContext;
import docking.DockingUtils;
import docking.action.DockingAction;
import docking.action.KeyBindingData;
import docking.widgets.EventTrigger;
import ghidraevt.component.EvtPanel;

/**
 * Action for adding all fields to the current format.
 */
public class SelectAllAction extends DockingAction {
	EvtPanel panel;

	public SelectAllAction(String owner, EvtPanel panel) {
		super("Select All", owner);
		this.panel = panel;
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_A, DockingUtils.CONTROL_KEY_MODIFIER_MASK));
	}

	@Override
	public void actionPerformed(ActionContext context) {
		panel.selectAll(EventTrigger.GUI_ACTION);
	}
}
