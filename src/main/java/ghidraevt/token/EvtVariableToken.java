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

import java.awt.Color;

import ghidra.program.model.address.Address;
import ghidraevt.component.EvtScript;
import jevt.Arg;

public final class EvtVariableToken extends EvtToken {
    private Arg.Variable var;

    public EvtVariableToken(EvtScript script, String txt, Color color, Address minAddress,
            long size, Arg.Variable ref) {
        super(script, txt, color, minAddress, size);
        this.var = ref;
    }

    public Arg.Variable getVar() {
        return var;
    }

}
