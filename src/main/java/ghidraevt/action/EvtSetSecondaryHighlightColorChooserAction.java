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
 * Modified from ghidra/app/plugin/core/decompile/actions/SetSecondaryHighlightColorChooserAction.java
 * to work on evt scripts
 */
package ghidraevt.action;

import java.awt.Color;
import java.util.List;

import docking.action.MenuData;
import docking.options.editor.GhidraColorChooser;
import ghidraevt.component.EvtPanel;
import ghidraevt.highlight.EvtTokenHighlightColors;
import ghidraevt.token.EvtToken;

public class EvtSetSecondaryHighlightColorChooserAction extends EvtAbstractSetSecondaryHighlightAction {
	public static String NAME = "Set Secondary Highlight With Color";

	public EvtSetSecondaryHighlightColorChooserAction() {
		super(NAME);

		setPopupMenuData(
			new MenuData(new String[] { "Secondary Highlight", "Set Highlight..." }, "Evt Disassembler"));
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		EvtToken token = context.getTokenAtCursor();
		EvtPanel panel = context.getEvtPanel();
		EvtTokenHighlightColors colors = panel.getSecondaryHighlightColors();
		List<Color> recentColors = colors.getRecentColors();

		String name = token.getText();
		Color currentColor = colors.getColor(name);
		GhidraColorChooser chooser = new GhidraColorChooser(currentColor);
		chooser.setColorHistory(recentColors);
		chooser.setActiveTab("RGB");

		Color colorChoice = chooser.showDialog(null);
		if (colorChoice == null) {
			return; // cancelled
		}

		colors.setColor(name, colorChoice);
		panel.addSecondaryHighlight(token, colorChoice);
	}
}
