/* ###
 * IP: GHIDRA
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
 */
package ghidraevt.action;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;

import docking.DockingUtils;
import docking.action.KeyBindingData;
import docking.action.ToolBarData;
import generic.theme.GIcon;
import ghidra.app.util.HelpTopics;
import ghidra.util.HelpLocation;

public class CloneAction extends AbstractEvtAction {

	public CloneAction() {
		super("Disassembler Clone");
		Icon image = new GIcon("icon.decompiler.action.provider.clone");
		setToolBarData(new ToolBarData(image, "ZZZ"));
		setDescription("Create a snapshot (disconnected) copy of this Decompiler window ");
		setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ToolBarSnapshot"));
		setKeyBindingData(new KeyBindingData(KeyEvent.VK_T,
			DockingUtils.CONTROL_KEY_MODIFIER_MASK | InputEvent.SHIFT_DOWN_MASK));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		return context.getScript() != null;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		context.getComponentProvider().cloneWindow();
	}
}
