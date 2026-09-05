/* ###
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
 */
package ghidraevt.token;

import java.util.*;

public class EvtTokenIterator implements Iterator<EvtToken> {
	private EvtToken currentToken;
	private boolean forward;

	public EvtTokenIterator(EvtToken token, boolean forward) {
		this.currentToken = token;
		this.forward = forward;
	}

	@Override
	public boolean hasNext() {
		return (currentToken != null);
	}

	@Override
	public EvtToken next() {
		EvtToken res = currentToken;
		currentToken = advanceToken();
		return res;
	}

	private boolean inBounds(int idx, int size) {
		return idx >= 0 && idx < size;
	}

	private int offset() {
		return forward ? 1 : -1;
	}

	private EvtToken advanceToken() {
		// Try within current line
		EvtLine currentLine = currentToken.getLineParent();
		int nextIndex = currentLine.indexOfToken(currentToken) + offset();
		if (inBounds(nextIndex, currentLine.getLength())) {
			return currentLine.getToken(nextIndex);
		}

		// Try go to next line
		EvtLine nextLine;
		do {
			EvtDocument document = currentLine.getDocumentParent();
			int nextLineIndex = document.indexOfLine(currentLine) + offset();
			if (!(inBounds(nextLineIndex, document.getLineCount())))
				return null;
			nextLine = document.getLine(nextLineIndex);
		} while(nextLine.getLength() == 0);
		if (forward)
			return nextLine.getToken(0);
		else
			return nextLine.getToken(nextLine.getLength() - 1);
	}

}
