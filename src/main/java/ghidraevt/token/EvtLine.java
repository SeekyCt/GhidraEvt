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
 * Modified from ghidra/app/decompiler/ClangLine.java to work on evt scripts
 */
package ghidraevt.token;

import java.util.List;

import ghidra.program.model.address.Address;

public class EvtLine {
    private int lineNumber; // Line numbers <= 0 will not be rendered
    private int indent;
    private List<EvtToken> tokens;
    private Address addr;

    public EvtLine(List<EvtToken> tokens, Address addr, int lineNumber, int indent) {
        this.tokens = tokens;
        for (EvtToken token : tokens) {
            token.setLineParent(this);
        }
        this.addr = addr;
        this.lineNumber = lineNumber;
        this.indent = indent;
    }

    public List<EvtToken> getAllTokens() {
        return tokens;
    }

    public Address getAddr() {
        return addr;
    }

    public int getLineNumber() {
        return lineNumber;
    }

	public EvtToken getToken(int i) {
		return tokens.get(i);
	}

    public int indexOfToken(EvtToken token) {
		return tokens.indexOf(token);
	}

    public int getIndent() {
        return indent;
    }

}
