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

import docking.ActionContext;
import docking.action.MenuData;
import ghidra.app.plugin.core.datamgr.util.DataTypeUtils;
import ghidra.app.services.DataTypeManagerService;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.data.*;
import ghidra.program.model.listing.Data;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

public class EditDataTypeAction extends AbstractEvtAction {

	public EditDataTypeAction() {
		super("Edit Data Type");
		setPopupMenuData(new MenuData(new String[] { "Edit Data Type" }, "Evt Disassembler"));
	}

	@Override
	public boolean isValidContext(ActionContext context) {
		return (context instanceof EvtActionContext);
	}

	private boolean hasCustomEditorForBaseDataType(PluginTool tool, DataType dataType) {
		DataType baseDataType = DataTypeUtils.getBaseDataType(dataType);
		final DataTypeManagerService service = tool.getService(DataTypeManagerService.class);
		return baseDataType != null && service.isEditable(baseDataType);
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		EvtToken tokenAtCursor = context.getTokenAtCursor();
        if (!(tokenAtCursor instanceof EvtAddrToken))
            return false;
        EvtAddrToken addr = (EvtAddrToken) tokenAtCursor;

		Data data = context.getProgram().getListing().getDataAt(addr.getTarget());
		if (data == null) {
			return false;
		}
		DataType dataType = data.getDataType();
		if (dataType == null) {
			return false;
		}

		return hasCustomEditorForBaseDataType(context.getTool(), dataType);
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		EvtToken tokenAtCursor = context.getTokenAtCursor();
        if (!(tokenAtCursor instanceof EvtAddrToken))
            return;
        EvtAddrToken addr = (EvtAddrToken) tokenAtCursor;

		Data data = context.getProgram().getListing().getDataAt(addr.getTarget());
		DataType dataType = data.getDataType();
		if (dataType == null)
			return;

		DataType baseDataType = DataTypeUtils.getBaseDataType(dataType);
		DataTypeManager dataTypeManager = context.getProgram().getDataTypeManager();
		DataTypeManager baseDtDTM = baseDataType.getDataTypeManager();
		if (baseDtDTM != dataTypeManager) {
			baseDataType = baseDataType.clone(dataTypeManager);
		}

		DataTypeManagerService service =
			context.getTool().getService(DataTypeManagerService.class);

		service.edit(baseDataType);
	}
}
