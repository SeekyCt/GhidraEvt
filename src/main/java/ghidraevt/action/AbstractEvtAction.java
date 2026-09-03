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
 * Modified from ghidra/app/plugin/core/decompile/actions/AbstractDecompilerAction.java to work on
 * evt scripts
 */
package ghidraevt.action;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.KeyBindingType;
import ghidra.app.util.datatype.DataTypeSelectionDialog;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.data.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;
import ghidra.util.data.DataTypeParser.AllowedDataTypes;
import ghidraevt.GhidraEvtPlugin;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

/**
 * A base class for {@link GhidraEvtPlugin} actions.  Each action is responsible for deciding its
 * enablement via {@link #isEnabledForEvtContext(EvtActionContext)}.  Each action must implement
 * {@link #evtActionPerformed(EvtActionContext)} to complete its work.
 */
public abstract class AbstractEvtAction extends DockingAction {

	protected AbstractEvtAction(String name) {
		super(name, GhidraEvtPlugin.class.getSimpleName());
	}

	protected AbstractEvtAction(String name, KeyBindingType kbType) {
		super(name, GhidraEvtPlugin.class.getSimpleName(), kbType);
	}

	@Override
	public boolean isValidContext(ActionContext context) {
		return context instanceof EvtActionContext;
	}

	@Override
	public boolean isEnabledForContext(ActionContext context) {
		return isEnabledForEvtContext((EvtActionContext) context);
	}

	@Override
	public void actionPerformed(ActionContext context) {
		evtActionPerformed((EvtActionContext) context);
	}

	protected static DataType chooseDataType(PluginTool tool, Program program,
			DataType currentDataType) {
		return chooseDataType(tool, program, currentDataType, AllowedDataTypes.FIXED_LENGTH);
	}

	protected static DataType chooseDataType(PluginTool tool, Program program,
			DataType currentDataType, AllowedDataTypes allowed) {
		DataTypeManager dataTypeManager = program.getDataTypeManager();
		DataTypeSelectionDialog chooserDialog = new DataTypeSelectionDialog(tool, dataTypeManager,
			Integer.MAX_VALUE, allowed);
		chooserDialog.setInitialDataType(currentDataType);
		tool.showDialog(chooserDialog);
		return chooserDialog.getUserChosenDataType();
	}

	protected Symbol getSymbolHighlighted(EvtActionContext context) {
		EvtToken token = context.getTokenAtCursor();
		if (token instanceof EvtAddrToken addr) {
			Program program = context.getProgram();
			SymbolTable symbolTable = program.getSymbolTable();
			return symbolTable.getPrimarySymbol(addr.getTarget());
		}
		else {
			return null;
		}
	}

	/**
	 * Subclasses return true if they are enabled for the given context
	 * 
	 * @param context the context
	 * @return true if enabled
	 */
	protected abstract boolean isEnabledForEvtContext(EvtActionContext context);

	/**
	 * Subclasses will perform their work in this method
	 * @param context the context
	 */
	protected abstract void evtActionPerformed(EvtActionContext context);
}
