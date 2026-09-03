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
 * Modified from ghidra/app/decompiler/component/DecompilerCallbackHandler.java to work on evt
 * scripts
 */
package ghidraevt.component;

import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.bean.field.AnnotatedTextFieldElement;
import utility.function.Callback;

public interface EvtCallbackHandler {
    void disassembleDataChanged(DisassembleData data);

    void contextChanged();

    void locationChanged(ProgramLocation programLocation);

    void selectionChanged(ProgramSelection programSelection);

    void annotationClicked(AnnotatedTextFieldElement annotation, boolean newWindow);

    void goToAddress(Address addr, boolean newWindow);

    void exportLocation();

    void doWhenNotBusy(Callback c);
}
