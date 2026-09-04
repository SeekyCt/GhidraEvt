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
package jevt;

import java.util.List;

public class Printer {
    public String print_evt(List<Instr> script) {
        StringBuilder ret = new StringBuilder();
        int indent = 0;
        for (Instr instr : script) {
            Opcode opcode = instr.opcode();

            // Unindent for this line
            indent -= opcode.unindent();

            for (int i = 0; i < indent; i++)
                ret.append("    ");

            ret.append(opcode.name());

            for (Arg arg : instr.args()) {
                ret.append(" " + arg.toString());
            }

            ret.append("\n");

            // Indent for next line
            indent += opcode.indent();
        }
        return ret.toString();
    }
}
