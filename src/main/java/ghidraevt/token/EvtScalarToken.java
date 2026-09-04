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
import ghidra.program.model.scalar.Scalar;
import ghidraevt.component.EvtScript;
import jevt.Arg;

public final class EvtScalarToken extends EvtToken {
    private Scalar scalar;

    public EvtScalarToken(EvtScript script, String txt, Color color, Address minAddress, long value, boolean signed) {
        super(script, txt, color, minAddress, Arg.bytesSize());
        this.scalar = new Scalar(32, value, signed);
    }

    public Scalar getScalar() {
        return scalar;
    }
}
