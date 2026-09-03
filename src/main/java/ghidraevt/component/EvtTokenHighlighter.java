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
 * Modified from ghidra/app/decompiler/component/ClangDecompilerHighlighter.java to work on evt
 * scripts
 */
package ghidraevt.component;

import java.awt.Color;
import java.util.*;
import java.util.function.Supplier;

import generic.json.Json;
import ghidraevt.token.EvtLine;
import ghidraevt.token.EvtToken;

/**
 * The implementation of {@link EvtHighlighter}.  This will get created by the
 * Disassembler and then handed to clients that use the {@link DisassemblerHighlightService}.  This
 * is also used internally for 'secondary highlights'.
 * 
 * <p>This class may be {@link #clone() cloned} or {@link #copy(DecompilerPanel) copied} as
 * needed when the user creates a snapshot.  Highlight service highlighters will be cloned;
 * secondary highlighters will be copied.  Cloning allows this class to delegate highlighting
 * and cleanup for clones.  Contrastingly, copying allows the secondary highlights to operate
 * independently.
 */
public class EvtTokenHighlighter implements EvtHighlighter {

    protected String id;
    private EvtPanel evtPanel;
    private EvtTokenHighlightMatcher matcher;
    private EvtScript script; // will be null for global highlights
    private Set<EvtTokenHighlighter> clones = new HashSet<>();

    EvtTokenHighlighter(String id, EvtPanel panel, EvtScript script,
            EvtTokenHighlightMatcher matcher) {
        this.id = id;
        this.evtPanel = panel;
        this.script = script;
        this.matcher = matcher;
    }

    private EvtTokenHighlighter(EvtPanel panel, EvtTokenHighlightMatcher matcher) {
        UUID uuId = UUID.randomUUID();
        this.id = uuId.toString();
        this.evtPanel = panel;
        this.matcher = matcher;
    }

    /**
     * Create a clone of this highlighter and tracks the clone
     * @param panel the panel
     * @return the highlighter
     */
    EvtTokenHighlighter clone(EvtPanel panel) {
        // note: we re-use the ID to make tracking easier
        EvtTokenHighlighter clone =
            new EvtTokenHighlighter(id, panel, script, matcher);
        clones.add(clone);
        return clone;
    }

    /**
     * Creates a copy of this highlighter that is not tracked by this highlighter
     * @param panel the panel
     * @return the highlighter
     */
    EvtTokenHighlighter copy(EvtPanel panel) {
        return new EvtTokenHighlighter(panel, matcher);
    }

    @Override
    public void applyHighlights() {

        if (evtPanel == null) {
            return; // disposed
        }

        EvtController controller = evtPanel.getController();
        EvtScript decompiledScript = controller.getScript();
        if (script != null && !script.equals(decompiledScript)) {
            return; // this is a function-specific highlighter and this is not the desired function 
        }

        // This is done by the caller of this method
        // clearHighlights();

        EvtLayoutModel layoutModel = evtPanel.getLayoutController();
        List<EvtLine> root = layoutModel.getLines();

        Map<EvtToken, Color> highlights = new HashMap<>();
        try {
            matcher.start(root);
            gatherHighlights(root, highlights);
        }
        finally {
            matcher.end();
        }

        Supplier<? extends Collection<EvtToken>> tokens = () -> highlights.keySet();
        EvtColorProvider colorProvider = new MappedTokenColorProvider(highlights);
        evtPanel.addHighlighterHighlights(this, tokens, colorProvider);

        clones.forEach(c -> c.applyHighlights());
    }

    private void gatherHighlights(List<EvtLine> root, Map<EvtToken, Color> results) {

        for (EvtLine line : root) {
            for (EvtToken token : line.getAllTokens())
                getHighlight(token, results);
        }
    }

    private void getHighlight(EvtToken token, Map<EvtToken, Color> results) {
        Color color = matcher.getTokenHighlight(token);
        if (color != null) {
            results.put(token, color);
        }
    }

    @Override
    public void clearHighlights() {
        if (evtPanel == null) {
            return; // disposed
        }

        evtPanel.removeHighlighterHighlights(this);
        clones.forEach(c -> c.clearHighlights());
    }

    @Override
    public void dispose() {
        if (evtPanel == null) {
            return; // disposed
        }

        clearHighlights();
        evtPanel.removeHighlighter(id);
        evtPanel = null;
        clones.forEach(c -> c.dispose());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return Json.toString(this, "matcher", "id");
    }

    private class MappedTokenColorProvider implements EvtColorProvider {

        private Map<EvtToken, Color> highlights;

        MappedTokenColorProvider(Map<EvtToken, Color> highlights) {
            this.highlights = highlights;
        }

        @Override
        public Color getColor(EvtToken token) {
            return highlights.get(token);
        }

        @Override
        public String toString() {
            return "Token Matcher Color " + matcher.toString();
        }
    }
}
