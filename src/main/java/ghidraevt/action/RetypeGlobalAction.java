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

import docking.DockingUtils;
import docking.action.KeyBindingData;
import docking.action.MenuData;
import ghidra.framework.cmd.Command;
import ghidra.app.cmd.data.CreateDataCmd;
import ghidra.app.cmd.data.CreateDataInStructureCmd;
import ghidra.app.util.HelpTopics;
import ghidra.app.util.datatype.DataTypeSelectionDialog;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressOverflowException;
import ghidra.program.model.data.DataType;
import ghidra.program.model.data.DataTypeManager;
import ghidra.program.model.data.DataUtilities;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.pcode.HighFunctionDBUtil;
import ghidra.program.model.pcode.HighSymbol;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.util.CodeUnitInsertionException;
import ghidra.program.util.CodeUnitLocation;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.*;
import ghidra.util.data.DataTypeParser.AllowedDataTypes;
import ghidra.util.exception.*;
import ghidraevt.component.EvtScript;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

import ghidra.app.plugin.core.data.ChooseDataTypeAction;;

/**
 * Action triggered from a specific token in the decompiler window to change the data-type
 * associated with a global variable. If the variable does not already exist in the program database,
 * it will be created using storage address the decompiler has assigned to the variable within its model.
 * In either case, there is a preexisting notion of variable storage. This action may allow the newly
 * selected data-type to be of a different size relative to this preexisting storage, constrained by
 * other global variables that might already consume storage.
 */
public class RetypeGlobalAction extends AbstractEvtAction {

	public RetypeGlobalAction() {
		super("Retype Global");
		setPopupMenuData(new MenuData(new String[] { "Retype Global" }, "Evt Disassembler"));
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_L, DockingUtils.CONTROL_KEY_MODIFIER_MASK));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		EvtScript script = context.getScript();
		if (script == null) {
			return false;
		}

		EvtToken tokenAtCursor = context.getTokenAtCursor();
		if (tokenAtCursor == null) {
			return false;
		}
		if (!(tokenAtCursor instanceof EvtAddrToken)) {
			return false;
		}
        Address addr = ((EvtAddrToken) tokenAtCursor).getTarget();
        if (context.getProgram().getFunctionManager().getFunctionAt(addr) != null)
            return false;
        else
            return true;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		Program program = context.getProgram();
		PluginTool tool = context.getTool();
		EvtToken tokenAtCursor = context.getTokenAtCursor();

        if (!(tokenAtCursor instanceof EvtAddrToken))
            return;
        EvtAddrToken addr = (EvtAddrToken) tokenAtCursor;
		Address address = addr.getTarget();

		DataType dataType = null;
        Data data = context.getProgram().getListing().getDataAt(address);

		if (data == null) {
			Msg.showError(this, tool.getToolFrame(), "Retype Failed",
			    "Failed to re-type global at '" + address + "': no data found.");
			return;
		}
		
		dataType = chooseDataType(tool, program, data.getDataType(), AllowedDataTypes.ALL);
		if (dataType == null)
			return;

		Command<Program> cmd  = new CreateDataCmd(address, true, false, dataType);
		context.getTool().execute(cmd, program);
	}
}
