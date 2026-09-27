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
package jevt.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jevt.Instr;
import jevt.Opcode;

public class BreakContinueValidator implements EvtValidator {
    @Override
    public Result checkScript(List<Instr> script) {
        List<Opcode> scopeStack = new ArrayList<>();

        int whileDepth = 0;
        int switchDepth = 0;
        for (int i = 0; i < script.size(); i++) {
            Instr instr = script.get(i);
            Opcode opcode = instr.opcode();

            switch (opcode) {
                case Opcode.DO:
                    whileDepth++;
                    break;
                case Opcode.WHILE:
                    whileDepth--;
                    break;
                case Opcode.DO_CONTINUE:
                    if (whileDepth <= 0)
                        return Result.error("DO_CONTINUE outside of do-while at " + i);
                    break;
                case Opcode.DO_BREAK:
                    if (whileDepth <= 0)
                        return Result.error("DO_BREAK outside of do-while at " + i);
                    break;

                case Opcode.SWITCH:
                    switchDepth++;
                    break;
                case Opcode.END_SWITCH:
                    switchDepth--;
                    break;
                case Opcode.SWITCH_BREAK:
                    if (switchDepth <= 0)
                        return Result.error("SWITCH_BREAK outside of switch at " + i);
                    break;
                
                default:
                    break;
            }

        }

        return Result.ok();
    }
}
