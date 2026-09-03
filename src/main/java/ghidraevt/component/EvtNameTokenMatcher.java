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
 * Modified from ghidra/app/decompiler/component/NameTokenMatcher.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.Color;

import generic.json.Json;
import ghidraevt.highlight.EvtTokenHighlightMatcher;
import ghidraevt.token.EvtToken;

/**
 * Matcher used for secondary highlights in the Disassembler.
 */
class EvtNameTokenMatcher implements EvtTokenHighlightMatcher {

	private EvtColorProvider colorProvider;
	private String name;

	EvtNameTokenMatcher(String name, EvtColorProvider colorProvider) {
		this.name = name;
		this.colorProvider = colorProvider;
	}

	@Override
	public Color getTokenHighlight(EvtToken token) {
		if (name.equals(token.getText())) {
			return colorProvider.getColor(token);
		}
		return null;
	}

	@Override
	public String toString() {
		return Json.toString(this);
	}
}
