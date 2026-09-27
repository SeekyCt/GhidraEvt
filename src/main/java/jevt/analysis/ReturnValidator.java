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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jevt.Instr;
import jevt.Opcode;

public class ReturnValidator implements EvtValidator {
    private static final Set<Opcode> CASE_OPCODES = Set.of(
        Opcode.CASE_AND,
        Opcode.CASE_BETWEEN,
        Opcode.CASE_END,
        Opcode.CASE_EQUAL,
        Opcode.CASE_ETC,
        Opcode.CASE_FLAG,
        Opcode.CASE_LARGE,
        Opcode.CASE_LARGE_EQUAL,
        Opcode.CASE_NOT_EQUAL,
        Opcode.CASE_OR,
        Opcode.CASE_SMALL,
        Opcode.CASE_SMALL_EQUAL
    );

    private Result checkFlow(List<Instr> script, int start, Set<Integer> visited) {
        int i = start;
        while (i < script.size()) {
            if (visited.contains(i))
                return null;
            visited.add(i);

            Instr instr = script.get(i);
            Opcode opcode = instr.opcode();
            switch (opcode) {
                case END_EVT:
                    return null;

                case INLINE_EVT, INLINE_EVT_ID:
                    // TODO: validate the embedded script
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_INLINE)
                        i++;
                    break;

                case BROTHER_EVT, BROTHER_EVT_ID:
                    // TODO: validate the embedded script
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_BROTHER)
                        i++;
                    break;

                case SWITCH:
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_SWITCH)
                    {
                        if (CASE_OPCODES.contains(script.get(i).opcode()))
                            checkFlow(script, i+1, visited);
                        i++;
                    }
                    // TODO: case default should stop this fallthrough
                    break;

                case SWITCH_BREAK, CASE_AND, CASE_BETWEEN, CASE_END, CASE_EQUAL, CASE_ETC,
                    CASE_FLAG, CASE_LARGE, CASE_LARGE_EQUAL, CASE_NOT_EQUAL, CASE_OR, CASE_SMALL,
                    CASE_SMALL_EQUAL:
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_SWITCH)
                        i++;
                    break;

                case DO:
                    checkFlow(script, i+1, visited);
                    while (i < script.size() && script.get(i).opcode() != Opcode.WHILE)
                        i++;
                    break;

                case DO_CONTINUE, DO_BREAK:
                    while (i < script.size() && script.get(i).opcode() != Opcode.WHILE)
                        i++;
                    break;

                case IFF_EQUAL, IFF_LARGE, IFF_LARGE_EQUAL, IFF_NOT_EQUAL, IFF_SMALL,
                    IFF_SMALL_EQUAL, IF_EQUAL, IF_FLAG, IF_LARGE, IF_LARGE_EQUAL, IF_NOT_EQUAL,
                    IF_NOT_FLAG, IF_SMALL, IF_SMALL_EQUAL, IF_STR_EQUAL, IF_STR_LARGE,
                    IF_STR_LARGE_EQUAL, IF_STR_NOT_EQUAL, IF_STR_SMALL, IF_STR_SMALL_EQUAL:
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_IF) {
                        if (script.get(i).opcode() == Opcode.ELSE)
                            checkFlow(script, i+1, visited);
                        i++;
                    }
                    break;

                case ELSE:
                    while (i < script.size() && script.get(i).opcode() != Opcode.END_IF)
                        i++;
                    break;

                case GOTO:
                    if (instr.args().isEmpty())
                        return Result.error("Broken goto found");
                    int j;
                    for (j = 0; j < script.size(); j++) {
                        Instr instr2 = script.get(j);
                        if (instr2.opcode() != Opcode.LBL)
                            continue;
                        if (instr2.args().isEmpty())
                            return Result.error("Broken label found");
                        if (instr.args().get(0).equals(instr2.args().get(0)))
                            break;
                    }
                    if (j == script.size())
                        return Result.error("Missing label found");
                    break;

                default:
                    i++;
            }
        }

        return Result.error("Non-returning path found");
    }

    @Override
    public Result checkScript(List<Instr> script) {
        // TODO: check for dead code
        Set<Integer> visited = new HashSet<>();
        Result ret = checkFlow(script, 0, visited);
        if (ret == null)
            ret = Result.ok();
        return ret;
    }
}
