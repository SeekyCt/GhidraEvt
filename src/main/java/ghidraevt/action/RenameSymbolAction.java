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
 * Modified from Ghidra's decompiler UI source code to work on evt scripts
 */
package ghidraevt.action;

import java.awt.event.KeyEvent;

import docking.action.KeyBindingData;
import docking.action.MenuData;
import ghidra.app.util.AddEditDialog;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.*;

public class RenameSymbolAction extends AbstractEvtAction {
	public RenameSymbolAction() {
		super("Rename Symbol");
		setPopupMenuData(new MenuData(new String[] { "Rename Symbol" }, "Evt Disassembler"));
		setKeyBindingData(new KeyBindingData(KeyEvent.VK_L, 0));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
        return getSymbolHighlighted(context) != null;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		PluginTool tool = context.getTool();
		Symbol symbol = getSymbolHighlighted(context);
		if (symbol == null) {
			Msg.showError(this, tool.getToolFrame(), "Rename Failed",
				"Memory storage not found for symbol");
			return;
		}
		AddEditDialog dialog = new AddEditDialog("Rename Symbol", context.getTool());
		dialog.editLabel(symbol, context.getProgram());
	}
}
