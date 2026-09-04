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

public class EvtAddrToken extends EvtToken {
    private Address target;

    public EvtAddrToken(EvtScript script, String txt, Color color, Address minAddress, Address target, long size) {
        super(script, txt, color, minAddress, size);
        this.target = target;
    }

    public Address getTarget() {
        return target;
    }
}
