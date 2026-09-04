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
 * Modified from ghidra/app/plugin/core/decompile/actions/HighlightDefinedUseAction.java to work on
 * evt scipts
 */
package ghidraevt.action;

import java.awt.Color;
import java.util.Set;

import ghidraevt.component.EvtColorProvider;
import ghidraevt.component.EvtPanel;
import ghidraevt.token.EvtToken;
import ghidraevt.token.EvtVariableToken;
import jevt.Arg;

/**
 * A class to provider a color for highlight a variable using one of the 'slice' actions
 * 
 * @see EvtForwardSliceAction
 * @see EvtBackwardsSliceAction
 */
public class EvtSliceHighlightColorProvider implements EvtColorProvider {

	private Set<Arg.Variable> varnodes;
	private Arg.Variable specialVn;
	private EvtToken specialToken;
	private Color hlColor;
	private Color specialHlColor;

	EvtSliceHighlightColorProvider(EvtPanel panel, Set<Arg.Variable> varnodes, Arg.Variable specialVn,
			EvtToken specialToken) {
		this.varnodes = varnodes;
		this.specialVn = specialVn;
		this.specialToken = specialToken;

		hlColor = panel.getCurrentVariableHighlightColor();
		specialHlColor = panel.getSpecialHighlightColor();
	}

	@Override
	public Color getColor(EvtToken token) {
		if (!(token instanceof EvtVariableToken))
			return null;
		Arg.Variable vn = ((EvtVariableToken) token).getVar();

		Color c = null;
		if (varnodes.contains(vn)) {
			c = hlColor;
		}

		if (specialToken == null) {
			return c;
		}

		// look for specific varnode to label with special color
		if (vn == specialVn && token == specialToken) {
			c = specialHlColor;
		}
		return c;
	}

	@Override
	public String toString() {
		return "Slice Color Provider " + hlColor;
	}
}
