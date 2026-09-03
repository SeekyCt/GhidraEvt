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
 * Modified from ghidra/app/decompiler/component/hover/ScalarValueDecompilerHover.java to work on
 * evt scripts
 */
package ghidraevt.hover;

import javax.swing.JComponent;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.GhidraOptions;
import ghidra.app.plugin.core.hover.AbstractScalarOperandHover;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.scalar.Scalar;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.EvtTextField;
import ghidraevt.token.EvtScalarToken;
import ghidraevt.token.EvtToken;

public class ScalarValueEvtHover extends AbstractScalarOperandHover
		implements EvtHoverService {

	// note: this is relative to other EvtHovers; a higher priority gets called first
	// Use high value so this hover gets called first.  The method for determining what the user
	// is hovering is less then perfect.  We choose to allow the more precise hovers to get a chance
    // to process the request first.
	private static final int PRIORITY = 30;

	private static final String NAME = "Scalar Operand Display (evt)";
	private static final String DESCRIPTION =
		"Scalars are shown as 1-, 2-, 4-, and 8-byte values, each in decimal, hexadecimal, and " +
			"as ASCII character sequences.";

	public ScalarValueEvtHover(PluginTool tool) {
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
		return GhidraOptions.CATEGORY_DECOMPILER_POPUPS;
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
        if (!(token instanceof EvtScalarToken)) {
            return null;
        }

        Scalar scalar = ((EvtScalarToken) token).getScalar();
		if (scalar == null) {
			return null;
		}
		Address addr = token.getMinAddress();
		String formatted = formatScalar(program, addr, scalar);
		return createTooltipComponent(formatted);
	}

}
