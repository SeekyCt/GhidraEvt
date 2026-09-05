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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EvtDocument implements Iterable<EvtLine> {
    private List<EvtLine> lines;

    public EvtDocument() {
        this.lines = new ArrayList<>();
    }

    public void addLine(int index, EvtLine line) {
        line.setDocumentParent(this);
        lines.add(index, line);
    }

    public void addLine(EvtLine line) {
        line.setDocumentParent(this);
        lines.add(line);
    }

    public int indexOfLine(EvtLine line) {
        return lines.indexOf(line);
    }

    public EvtLine getLine(int index) {
        return lines.get(index);
    }

    public int getLineCount() {
        return lines.size();
    }

    @Override
    public Iterator<EvtLine> iterator() {
        return lines.iterator();
    }
}
