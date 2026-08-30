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

import java.util.List;

import docking.widgets.EventTrigger;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidraevt.token.EvtLine;

/**
 * A stub implementation of the highlight controller that allows clients to avoid null checks
 */
public class NullEvtHighlightController extends EvtHighlightController {

	@Override
	public void fieldLocationChanged(FieldLocation location, Field field, EventTrigger trigger) {
		// stub
	}

	@Override
	public void addPrimaryHighlights(List<EvtLine> parentNode, EvtColorProvider colorProvider) {
		// stub
	}

	// @Override
	// public void addPrimaryHighlightToTokensForBrace(EvtToken token, Color highlightColor) {
	// 	// stub
	// }

	@Override
	public void addListener(EvtHighlightListener listener) {
		// stub
	}

	@Override
	public void removeListener(EvtHighlightListener listener) {
		// stub
	}
}
