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
package ghidraevt.token;

import java.awt.Color;

import ghidra.program.model.address.Address;
import ghidraevt.component.EvtScript;
import jevt.Arg;
import jevt.Instr;

public class EvtToken {
    private EvtScript script;
    private String text;
    private Color color;
    private EvtLine lineParent;
    private Address minAddress;
    private Address maxAddress;

    private Color highlight; // Color to highlight with or null if no highlight
	private boolean matchingToken;

    public EvtToken(EvtScript script, String txt, Color color, Address minAddress, long size) {
        this.script = script;
        this.text = txt;
        this.color = color;
        this.minAddress = minAddress;
        this.highlight = null;
        if (minAddress != null && size > 0)
            this.maxAddress = minAddress.add(size);
        else
            this.maxAddress = null;
    }

    public static EvtToken instr(EvtScript script, String txt, Color color, Address minAddress) {
        return new EvtToken(script, txt, color, minAddress, Instr.HEADER_SIZE);
    }

    public static EvtToken arg(EvtScript script, String txt, Color color, Address minAddress) {
        return new EvtToken(script, txt, color, minAddress, Arg.bytesSize());
    }

    public static EvtToken argScalar(EvtScript script, String txt, Color color, Address minAddress, long value, boolean signed) {
        return new EvtScalarToken(script, txt, color, minAddress, value, signed);
    }

    public static EvtToken syntax(EvtScript script, String txt, Color color, Address minAddress) {
        return new EvtToken(script, txt, color, minAddress, 0);
    }

    public EvtScript getScript() {
        return script;
    }

	public void setHighlight(Color val) {
		highlight = val;
	}

	/**
	 * Get the background highlight color used to render this token, or null if not highlighted
	 * @return the Color or null
	 */
	public Color getHighlight() {
		return highlight;
	}

	/**
	 * Set whether or not additional "matching" highlighting is applied to this token.
	 * Currently this means a bounding box is drawn around the token.
	 * @param matchingToken is true to enable highlighting, false to disable
	 */
	public void setMatchingToken(boolean matchingToken) {
		this.matchingToken = matchingToken;
	}

	/**
	 * @return true if this token should be displayed with "matching" highlighting
	 */
	public boolean isMatchingToken() {
		return matchingToken;
	}

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    public void setLineParent(EvtLine lineParent) {
        this.lineParent = lineParent;
    }

    public EvtLine getLineParent() {
        return lineParent;
    }

    public Address getMinAddress() {
        return minAddress;
    }

    public Address getMaxAddress() {
        return maxAddress;
    }
}
