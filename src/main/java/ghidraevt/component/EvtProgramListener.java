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

import ghidra.framework.model.*;
import ghidra.program.model.listing.Program;
import ghidra.util.task.SwingUpdateManager;

/**
 * Listener of {@link Program} events for disassembler panels. Program events are buffered using 
 * a {@link SwingUpdateManager} before triggering a new disassemble process.
*/
public class EvtProgramListener implements DomainObjectListener {

    private SwingUpdateManager updater;

    /**
     * Construct a listener with a SwingUpdateManger that should be kicked for every
     * program change.
     * @param controller the EvtController
     * @param updater A SwingUpdateManger to be kicked as program events are received which will
     * eventually trigger a disassemble refresh.
     */
    public EvtProgramListener(SwingUpdateManager updater) {
        this.updater = updater;
    }

    @Override
    public void domainObjectChanged(DomainObjectChangedEvent ev) {
        updater.update();
    }

    public void dispose() {
        updater.dispose();
    }
}
