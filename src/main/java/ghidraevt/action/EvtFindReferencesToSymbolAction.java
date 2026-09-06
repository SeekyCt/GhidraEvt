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
 * Modified from ghidra/app/plugin/core/decompile/actions/FindReferencesToHighSymbolAction.java to
 * work on evt scripts
 */
package ghidraevt.action;

import docking.action.MenuData;
import ghidra.app.plugin.core.navigation.locationreferences.LocationReferencesService;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.util.LabelFieldLocation;
import ghidra.util.Msg;
import ghidraevt.component.EvtProvider;

/**
 * An action to show all references to the {@link Symbol} under the cursor in the Disassembler.
 */
public class EvtFindReferencesToSymbolAction extends AbstractEvtAction {

	private static final String MENU_ITEM_TEXT = "Find References to";
	public static final String NAME = "Find References to Symbol";

	public EvtFindReferencesToSymbolAction() {
		super(NAME);
		setPopupMenuData(
			new MenuData(new String[] { LocationReferencesService.MENU_GROUP, MENU_ITEM_TEXT }));
	}

	private void updateMenuName(String newName) {
		String menuName = MENU_ITEM_TEXT + ' ' + newName;
		MenuData data = getPopupMenuData().cloneData();
		data.setMenuPath(new String[] { LocationReferencesService.MENU_GROUP, menuName });
		setPopupMenuData(data);
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		Symbol symbol = getSymbolHighlighted(context);
		if (symbol == null) {
			return false;
		}
		updateMenuName(symbol.getName());
		return true;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		Symbol symbol = getSymbolHighlighted(context);
		LocationReferencesService service =
			context.getTool().getService(LocationReferencesService.class);
		if (service == null) {
			Msg.showError(this, null, "Missing Plugin",
				"The " + LocationReferencesService.class.getSimpleName() + " is not installed.\n" +
					"Please add the plugin implementing this service.");
			return;
		}

		LabelFieldLocation location = new LabelFieldLocation(symbol);
		EvtProvider provider = context.getComponentProvider();
		service.showReferencesToLocation(location, provider);
	}
}
