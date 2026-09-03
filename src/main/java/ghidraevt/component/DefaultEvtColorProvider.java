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
 * Modified from ghidra/app/decompiler/component/DefaultColorProvider.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.Color;

import ghidraevt.token.EvtToken;

/**
 * A color provider that returns a specific color.
 */
public class DefaultEvtColorProvider implements EvtColorProvider {

	private Color color;
	private String prefix;

	/**
	 * Constructor
	 * @param prefix a descriptive prefix used in the {@link #toString()} method
	 * @param color the color
	 */
	public DefaultEvtColorProvider(String prefix, Color color) {
		this.prefix = prefix;
		this.color = color;
	}

	@Override
	public Color getColor(EvtToken token) {
		return color;
	}

	@Override
	public String toString() {
		return prefix + ' ' + color;
	}
}
