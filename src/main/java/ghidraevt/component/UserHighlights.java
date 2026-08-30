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

import org.apache.commons.collections4.map.LazyMap;

import ghidraevt.token.EvtToken;

/**
 * A class to manage and track Disassembler highlights created by the user via the UI or from a 
 * script.  This class manages secondary highlights and highlights created from the 
 * {@link EvtHighlightService}, which has both global and per-function highlights.  For a 
 * description of these terms, see {@link EvtHighlightController}.
 * <p>
 * These highlights will remain until cleared explicitly by the user or a client API call.  
 * Contrastingly, context highlights are cleared as the user moves the cursor around the Disassembler 
 * display.
 */
public class UserHighlights {

	private Map<EvtScript, List<EvtHighlighter>> secondaryHighlightersByScript =
		LazyMap.lazyMap(new HashMap<>(), f -> new ArrayList<>());

	// store the secondary highlighters here in addition to the map below so that we may discern
	// between secondary highlights and highlight service highlights
	private Set<EvtHighlighter> secondaryHighlighters = new HashSet<>();

	// all highlighters, including secondary and global highlight service highlighters and per 
	// function highlight service highlighters
	private Map<EvtHighlighter, TokenHighlights> allHighlighterHighlights = new HashMap<>();

	// color supplier for secondary highlights
	private EvtTokenHighlightColors secondaryHighlightColors = new EvtTokenHighlightColors();

	Color getSecondaryColor(String text) {
		// Note: this call is used to generate colors for secondary highlighters that this API
		// creates.  Client highlighters will create their own colors.
		return secondaryHighlightColors.getColor(text);
	}

	String getAppliedColorsString() {
		return secondaryHighlightColors.getAppliedColorsString();
	}

	boolean hasSecondaryHighlights(EvtScript script) {
		return !secondaryHighlightersByScript.get(script).isEmpty();
	}

	Color getSecondaryHighlight(EvtToken token) {
		EvtHighlighter highlighter = getSecondaryHighlighter(token);
		if (highlighter != null) {
			TokenHighlights highlights = allHighlighterHighlights.get(highlighter);
			HighlightToken hlToken = highlights.get(token);
			return hlToken.getColor();
		}

		return null;
	}

	EvtTokenHighlightColors getSecondaryHighlightColors() {
		return secondaryHighlightColors;
	}

	Set<EvtHighlighter> getSecondaryHighlighters(EvtScript script) {
		return new HashSet<>(secondaryHighlightersByScript.get(script));
	}

	Set<EvtHighlighter> getServiceHighlighters() {
		Set<EvtHighlighter> allHighlighters = allHighlighterHighlights.keySet();
		Set<EvtHighlighter> results = new HashSet<>(allHighlighters);
		results.removeAll(secondaryHighlighters);
		return results;
	}

	List<EvtHighlighter> getSecondaryHighlightersByFunction(EvtScript f) {
		return secondaryHighlightersByScript.get(f);
	}

	TokenHighlights getHighlights(EvtHighlighter highlighter) {
		return allHighlighterHighlights.get(highlighter);
	}

	EvtHighlighter getSecondaryHighlighter(EvtToken token) {
		for (EvtHighlighter highlighter : secondaryHighlighters) {
			TokenHighlights highlights = allHighlighterHighlights.get(highlighter);
			HighlightToken hlToken = highlights.get(token);
			if (hlToken != null) {
				return highlighter;
			}
		}

		return null;
	}

	void addSecondaryHighlighter(EvtScript script, EvtHighlighter highlighter) {

		// Note: this highlighter has likely already been added to this class, but has not
		//       yet been bound to the given function.
		secondaryHighlightersByScript.get(script).add(highlighter);
		secondaryHighlighters.add(highlighter);
		allHighlighterHighlights.putIfAbsent(highlighter, new TokenHighlights());
	}

	// This adds the given highlighter.  This is for global and secondary highlights.  Secondary
	// highlights will be later registered to this class for the function they apply to.
	TokenHighlights add(EvtHighlighter highlighter) {
		allHighlighterHighlights.putIfAbsent(highlighter, new TokenHighlights());
		return allHighlighterHighlights.get(highlighter);
	}

	void remove(EvtHighlighter highlighter) {
		allHighlighterHighlights.remove(highlighter);
		secondaryHighlighters.remove(highlighter);

		Collection<List<EvtHighlighter>> lists = secondaryHighlightersByScript.values();
		for (List<EvtHighlighter> highlighters : lists) {
			if (highlighters.remove(highlighter)) {
				break;
			}
		}
	}

	TokenHighlights get(EvtHighlighter highlighter) {
		return allHighlighterHighlights.get(highlighter);
	}

	void dispose() {
		secondaryHighlighters.clear();
		secondaryHighlightersByScript.clear();
		allHighlighterHighlights.clear();
	}
}
