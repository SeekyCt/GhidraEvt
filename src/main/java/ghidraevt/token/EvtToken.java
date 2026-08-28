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
package ghidraevt.token;

import java.awt.Color;

import ghidra.program.model.address.Address;
import jevt.Arg;
import jevt.Instr;

public class EvtToken {
    private String text;
    private Color color;
    private EvtLine lineParent;
    Address minAddress;
    Address maxAddress;

    public EvtToken(String txt, Color color, Address minAddress, long size) {
        this.text = txt;
        this.color = color;
        this.minAddress = minAddress;
        if (minAddress != null && size > 0)
            this.maxAddress = minAddress.add(size);
        else
            this.maxAddress = null;
    }

    public static EvtToken instr(String txt, Color color, Address minAddress) {
        return new EvtToken(txt, color, minAddress, Instr.HEADER_SIZE);
    }

    public static EvtToken arg(String txt, Color color, Address minAddress) {
        return new EvtToken(txt, color, minAddress, Arg.bytesSize());
    }

    public static EvtToken argScalar(String txt, Color color, Address minAddress, long value, boolean signed) {
        return new EvtScalarToken(txt, color, minAddress, value, signed);
    }

    public static EvtToken syntax(String txt, Color color, Address minAddress) {
        return new EvtToken(txt, color, minAddress, 0);
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    public void setLineParent(EvtLine lineParent) {
        this.lineParent = lineParent;
    }

    public EvtLine getLineParent() {
        return lineParent;
    }

    public Address getMinAddress() {
        return minAddress;
    }

    public Address getMaxAddress() {
        return maxAddress;
    }
}
