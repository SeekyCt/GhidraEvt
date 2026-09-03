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
 * Modified from ghidra/app/decompiler/component/HighlightToken.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.Color;

import ghidraevt.token.EvtToken;

/**
 * A class to used to track a disassembler token along with its highlight color
 */
public class EvtHighlightToken {

	private EvtToken token;
	private Color color;

	public EvtHighlightToken(EvtToken token, Color color) {
		this.token = token;
		this.color = color;
	}

	public EvtToken getToken() {
		return token;
	}

	public Color getColor() {
		return color;
	}

	@Override
	public String toString() {
		return token.toString() + "; highlight=" + color;
	}
}
