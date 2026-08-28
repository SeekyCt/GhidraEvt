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
package ghidraevt.location;

import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.DisassembleResults;
import ghidraevt.token.EvtToken;

/**
 * Represents a location in the disassembler. This interface allows the disassembler to subclass more
 * general {@link ProgramLocation}s while adding more detailed disassembler information.
 */
public interface EvtLocation {
    public Address getScriptEntryPoint();

    /**
     * Results from the disassembly
     * 
     * @return Results object. null if there are no results attached to this location
     */
    public DisassembleResults getDisassembly();

    /**
     * C text token at the current cursor location
     * 
     * @return token at this location, could be null if there are no disassembly results
     */
    public EvtToken getToken();

    /**
     * {@return the name of the token for the current location}
     */
    public String getTokenName();

    /**
     * {@return the line number}
     */
    public int getLineNumber();

    /**
     * {@return the character position}
     */
    public int getCharPos();
}
