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
 * Modified from ghidra/app/decompiler/component/hover/FunctionSignatureDecompilerHover.java to
 * work on evt scripts
 */
package ghidraevt.component.hover;

import javax.swing.JComponent;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.GhidraOptions;
import ghidra.app.plugin.core.hover.AbstractConfigurableHover;
import ghidra.app.util.ToolTipUtils;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.EvtTextField;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

/**
 * A hover service to show tool tip text for hovering over a function name in the disassembler.
 * The tooltip shows the function signature per the listing.
*/
public class FunctionSignatureEvtHover extends AbstractConfigurableHover
		implements EvtHoverService {
	private static final String NAME = "Function Signature Display (evt)";
	private static final String DESCRIPTION =
		"Show function signatures when hovering over a function name.";

	// note: this is relative to other EvtHovers; a higher priority gets called first
	private static final int PRIORITY = 20;

	public FunctionSignatureEvtHover(PluginTool tool) {
		super(tool, PRIORITY);
	}

	@Override
	protected String getName() {
		return NAME;
	}

	@Override
	protected String getDescription() {
		return DESCRIPTION;
	}

	@Override
	protected String getOptionsCategory() {
		return GhidraOptions.CATEGORY_DECOMPILER_POPUPS; // TODO
	}

	@Override
	public JComponent getHoverComponent(Program program, ProgramLocation programLocation,
			FieldLocation fieldLocation, Field field) {

		if (!enabled) {
			return null;
		}

		if (!(field instanceof EvtTextField)) {
			return null;
		}

		EvtToken token = ((EvtTextField) field).getToken(fieldLocation);
		if (token instanceof EvtAddrToken addr) {
			Function function = program.getFunctionManager().getFunctionAt(addr.getTarget());
			if (function == null) {
				return null; // no function in program; maybe bad address
			}
			String content = ToolTipUtils.getToolTipText(function, false);
			return createTooltipComponent(content);
		}

		return null;
	}
}
