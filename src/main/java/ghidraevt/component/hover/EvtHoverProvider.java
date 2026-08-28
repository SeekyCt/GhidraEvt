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
package ghidraevt.component.hover;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.app.plugin.core.hover.AbstractHoverProvider;
import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.EvtTextField;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

public class EvtHoverProvider extends AbstractHoverProvider {

	public EvtHoverProvider() {
		super("EvtHoverProvider");
	}

	public void addHoverService(EvtHoverService hoverService) {
		super.addHoverService(hoverService);
	}

	public void removeHoverService(EvtHoverService hoverService) {
		super.removeHoverService(hoverService);
	}

	@Override
	protected ProgramLocation getHoverLocation(FieldLocation fieldLocation, Field field,
			Rectangle fieldBounds, MouseEvent event) {

		if (!(field instanceof EvtTextField)) {
			return null;
		}

		EvtTextField decompilerField = (EvtTextField) field;
		EvtToken token = decompilerField.getToken(fieldLocation);

		if (token.getMinAddress() == null) {
			return null;
		}

        Address reference = null;
        if (token instanceof EvtAddrToken addr) {
            reference = addr.getTarget();
        }

		return new ProgramLocation(program, token.getMinAddress(), reference);
	}
}
