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

import java.awt.Color;
import java.util.*;

import ghidraevt.token.EvtToken;

/**
 * A simple class to manage {@link EvtHighlightToken}s used to create highlights in the Decompiler.
 * This class allows clients to access highlights either by a {@link ClangToken} or a
 * {@link EvtHighlightToken}.
 */
public class TokenHighlights implements Iterable<EvtHighlightToken> {

	private Map<TokenKey, EvtHighlightToken> highlightsByToken = new HashMap<>();

	public Map<String, Color> copyHighlightsByName() {
		Map<String, Color> results = new HashMap<>();

		Collection<EvtHighlightToken> values = highlightsByToken.values();
		for (EvtHighlightToken hl : values) {
			String name = hl.getToken().getText();
			results.put(name, hl.getColor());
		}

		return results;
	}

	private TokenKey getKey(EvtHighlightToken ht) {
		return new TokenKey(ht);
	}

	private TokenKey getKey(EvtToken t) {
		return new TokenKey(t);
	}

	/**
	 * Returns true if there are not highlights
	 * @return true if there are not highlights
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns the number of highlights
	 * @return the number of highlights
	 */
	public int size() {
		return highlightsByToken.size();
	}

	/**
	 * Adds the given highlight to this container
	 * @param t the highlight
	 */
	public void add(EvtHighlightToken t) {
		highlightsByToken.put(getKey(t), t);
	}

	/**
	 * Gets the current highlight for the given token
	 * @param t the token
	 * @return the highlight
	 */
	public EvtHighlightToken get(EvtToken t) {
		return highlightsByToken.get(getKey(t));
	}

	/**
	 * Returns true if this class has a highlight for the given token
	 * @param t the token
	 * @return true if this class has a highlight for the given token
	 */
	public boolean contains(EvtToken t) {
		return highlightsByToken.containsKey(getKey(t));
	}

	/**
	 * Removes all highlights from this container
	 */
	public void clear() {
		highlightsByToken.clear();
	}

	/**
	 * Removes the highlight for the given token
	 * @param t the token
	 */
	public void remove(EvtToken t) {
		highlightsByToken.remove(getKey(t));
	}

	@Override
	public Iterator<EvtHighlightToken> iterator() {
		return highlightsByToken.values().iterator();
	}

	@Override
	public String toString() {
		return highlightsByToken.values().toString();
	}
}
