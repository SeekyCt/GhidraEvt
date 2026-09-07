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
 * Modified from ghidra/app/plugin/core/decompile/actions/FindReferencesToDataTypeAction.java to
 * work on evt scripts
 */
package ghidraevt.action;

import docking.ActionContext;
import docking.action.MenuData;
import ghidra.app.actions.AbstractFindReferencesDataTypeAction;
import ghidra.app.plugin.core.navigation.locationreferences.LocationReferencesService;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.data.DataType;
import ghidraevt.component.EvtUtils;

public class EvtFindReferencesToDataTypeAction extends AbstractFindReferencesDataTypeAction {

    public EvtFindReferencesToDataTypeAction(String owner, PluginTool tool) {
        super(tool, NAME, owner, DEFAULT_KEY_STROKE);

        setPopupMenuData(
            new MenuData(new String[] { LocationReferencesService.MENU_GROUP, "Find Uses of " }));
    }

    @Override
    public DataType getDataType(ActionContext context) {
        return EvtUtils.getDataType((EvtActionContext) context);
    }

    @Override
    public boolean isEnabledForContext(ActionContext context) {
        if (!(context instanceof EvtActionContext)) {
            return false;
        }

        DataType dataType = getDataType(context);
        updateMenuName(dataType);
        return super.isEnabledForContext(context);
    }

    private void updateMenuName(DataType type) {
        if (type == null) {
            return; // not sure if this can happen
        }

        String typeName = type.getName();
        String menuName = "Find Uses of " + typeName;
        String fieldName = getDataTypeField(type);
        if (fieldName != null) {
            menuName += '.' + fieldName;
        }

        MenuData data = getPopupMenuData().cloneData();
        data.setMenuPath(new String[] { LocationReferencesService.MENU_GROUP, menuName });
        setPopupMenuData(data);
    }
}
