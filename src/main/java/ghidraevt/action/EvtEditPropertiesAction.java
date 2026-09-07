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
 * Modified from ghidra/app/plugin/core/decompile/actions/EditPropertiesAction.java to work on evt
 * scripts
 */
package ghidraevt.action;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.MenuData;
import docking.options.OptionsService;
import ghidra.framework.plugintool.PluginTool;
import ghidraevt.GhidraEvtPlugin;

public class EvtEditPropertiesAction extends DockingAction {
	private final PluginTool tool;

	public EvtEditPropertiesAction(String owner, PluginTool tool) {
		super("EvtProperties", owner);
		this.tool = tool;
		setPopupMenuData( new MenuData( new String[]{ "Properties"}, "ZED" ) );
	}

	@Override
	public boolean isEnabledForContext(ActionContext context) {
		return tool.getService(OptionsService.class) != null;
	}
	
	@Override
	public void actionPerformed(ActionContext context) {
        OptionsService service = tool.getService( OptionsService.class );
        service.showOptionsDialog( GhidraEvtPlugin.OPTIONS_TITLE, "" );
	}
}
