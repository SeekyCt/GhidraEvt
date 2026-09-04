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
 * Modified from ghidra/app/decompiler/component/DecompilerPanel.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

import docking.widgets.fieldpanel.field.*;
import generic.theme.GThemeDefaults.Colors.Palette;
import ghidraevt.token.EvtToken;

public class EvtFieldElement extends AbstractTextFieldElement {

	private final EvtToken token;

	public EvtFieldElement(EvtToken token, AttributedString as, int col) {
		super(as, 0, col);
		this.token = token;
	}

	EvtToken getToken() {
		return token;
	}

	@Override
	public void paint(JComponent c, Graphics g, int x, int y) {
		Color highlightColor = token.getHighlight();
		if (highlightColor != null) {
			g.setColor(highlightColor);
			g.fillRect(x, y - getHeightAbove(), getStringWidth(),
				getHeightAbove() + getHeightBelow());
		}

		super.paint(c, g, x, y);

		if (token.isMatchingToken()) {
			// paint a bounding box around the token
			g.setColor(Palette.GRAY);
			int offset = 1;
			g.drawRect(x - offset, y - getHeightAbove() - offset, getStringWidth() + (offset * 2),
				getHeightAbove() + getHeightBelow() + (offset * 2));
		}
	}

	@Override
	public FieldElement substring(int start, int end) {
		AttributedString as = attributedString.substring(start, end);
		if (as == attributedString) {
			return this;
		}
		return new EvtFieldElement(token, as, column + start);
	}

	@Override
	public FieldElement replaceAll(char[] targets, char replacement) {
		return new EvtFieldElement(token, attributedString.replaceAll(targets, replacement),
			column);
	}
}
