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

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.KeyBindingType;
import ghidra.app.util.datatype.DataTypeSelectionDialog;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.data.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.pcode.*;
import ghidra.program.model.symbol.*;
import ghidra.util.data.DataTypeParser.AllowedDataTypes;
import ghidraevt.GhidraEvtPlugin;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

/**
 * A base class for {@link GhidraEvtPlugin} actions that handles checking whether the
 * disassembler is busy.   Each action is responsible for deciding its enablement via
 * {@link #isEnabledForEvtContext(EvtActionContext)}.  Each action must implement
 * {@link #evtActionPerformed(EvtActionContext)} to complete its work.
 * 
 * <p>This parent class uses the {@link EvtActionContext} to check for the disassembler's
 * busy status.  If the disassembler is busy, then the action will report that it is enabled.  We
 * do this so that any keybindings registered for this action will get consumed and not passed up
 * to the global context.   Then, if the action is executed, this class does not call the child
 * class, but will instead show an information message indicating that the disassembler is busy.
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

	/**
	 * Get the structure/union associated with a field token
	 * @param tok is the token representing a field
	 * @return the structure/union which contains this field
	 */
	// public static Composite getCompositeDataType(ClangToken tok) {
	// 	// We already know tok is a ClangFieldToken
	// 	ClangFieldToken fieldtok = (ClangFieldToken) tok;
	// 	DataType dt = fieldtok.getDataType();
	// 	if (dt == null) {
	// 		return null;
	// 	}
	// 	if (dt instanceof TypeDef) {
	// 		dt = ((TypeDef) dt).getBaseDataType();
	// 	}
	// 	if (dt instanceof Composite) {
	// 		return (Composite) dt;
	// 	}
	// 	return null;
	// }

	/**
	 * Compare the given HighFunction's idea of the prototype with the Function's idea.
	 * Return true if there is a difference. If a specific symbol is being changed,
	 * it can be passed in to check whether or not the prototype is being affected.
	 * @param highSymbol (if not null) is the symbol being modified
	 * @param hfunction is the given HighFunction
	 * @return true if there is a difference (and a full commit is required)
	 */
	protected static boolean checkFullCommit(HighSymbol highSymbol, HighFunction hfunction) {
		if (highSymbol != null && !highSymbol.isParameter()) {
			return false;
		}
		Function function = hfunction.getFunction();
		Parameter[] parameters = function.getParameters();
		LocalSymbolMap localSymbolMap = hfunction.getLocalSymbolMap();
		int numParams = localSymbolMap.getNumParams();
		if (numParams != parameters.length) {
			return true;
		}

		for (int i = 0; i < numParams; i++) {
			HighSymbol param = localSymbolMap.getParamSymbol(i);
			if (param.getCategoryIndex() != i) {
				return true;
			}
			VariableStorage storage = param.getStorage();
			// Don't compare using the equals method so that DynamicVariableStorage can match
			if (0 != storage.compareTo(parameters[i].getVariableStorage())) {
				return true;
			}
		}

		return false;
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
