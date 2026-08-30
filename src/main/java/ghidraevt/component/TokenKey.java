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
package ghidraevt.component;

import java.util.Objects;

import ghidraevt.token.EvtLine;
import ghidraevt.token.EvtToken;

// a key that allows us to equate tokens that are not the same instance
class TokenKey {

	private EvtToken token;
	private int hash;
	private int lineNumber = -1;
	private int indexInParent = Integer.MAX_VALUE;

	TokenKey(EvtToken token) {
		this.token = Objects.requireNonNull(token);

		EvtLine lineParent = token.getLineParent();
		if (lineParent != null) {
			lineNumber = lineParent.getLineNumber();
		}

		// have the hash be more than just the token text, otherwise, the number of hash collisions
		// can become quite large if the user is matching on the same token text for multiple tokens
		hash = Objects.hash(token.getText());
		hash += lineNumber;
	}

	public TokenKey(HighlightToken t) {
		this(t.getToken());
	}

	private int getIndexInParent() {
		if (indexInParent == Integer.MAX_VALUE) {
			EvtLine lineParent = token.getLineParent();
			if (lineParent != null) {
				indexInParent = lineParent.indexOfToken(token);
			}
			else {
				indexInParent = -1;
			}
		}
		return indexInParent;
	}

	public EvtToken getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		TokenKey otherKey = (TokenKey) obj;
		EvtToken otherToken = otherKey.token;
		if (token.getClass() != otherToken.getClass()) {
			return false;
		}

		if (!Objects.equals(token.getText(), otherToken.getText())) {
			return false;
		}

		if (lineNumber != otherKey.lineNumber) {
			return false;
		}

		return getIndexInParent() == otherKey.getIndexInParent();
	}

	@Override
	public String toString() {
		return token.toString();
	}
}
