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

import ghidra.util.Msg;
import jevt.Instr;
import jevt.Opcode;

public class ScopeValidator implements EvtValidator {
    private static final List<Opcode> IF_OPCODES = Arrays.asList(
        Opcode.IF_STR_EQUAL,
        Opcode.IF_STR_NOT_EQUAL,
        Opcode.IF_STR_SMALL,
        Opcode.IF_STR_LARGE,
        Opcode.IF_STR_SMALL_EQUAL,
        Opcode.IF_STR_LARGE_EQUAL,
        Opcode.IFF_EQUAL,
        Opcode.IFF_NOT_EQUAL,
        Opcode.IFF_SMALL,
        Opcode.IFF_LARGE,
        Opcode.IFF_SMALL_EQUAL,
        Opcode.IFF_LARGE_EQUAL,
        Opcode.IF_EQUAL,
        Opcode.IF_NOT_EQUAL,
        Opcode.IF_SMALL,
        Opcode.IF_LARGE,
        Opcode.IF_SMALL_EQUAL,
        Opcode.IF_LARGE_EQUAL,
        Opcode.IF_FLAG,
        Opcode.IF_NOT_FLAG
    );

    private static final  List<Opcode> CASE_OPCODES = Arrays.asList(
        Opcode.CASE_EQUAL,
        Opcode.CASE_NOT_EQUAL,
        Opcode.CASE_SMALL,
        Opcode.CASE_LARGE,
        Opcode.CASE_SMALL_EQUAL,
        Opcode.CASE_LARGE_EQUAL,
        Opcode.CASE_ETC,
        Opcode.CASE_OR,
        Opcode.CASE_AND,
        Opcode.CASE_FLAG,
        Opcode.CASE_BETWEEN
    );

    private boolean checkUnindent(Opcode opening, Opcode closing) {
        return switch (opening) {
            case Opcode.SWITCH, Opcode.SWITCHI
                -> closing == Opcode.END_SWITCH || CASE_OPCODES.contains(closing);

            case Opcode.CASE_EQUAL, Opcode.CASE_NOT_EQUAL, Opcode.CASE_SMALL, Opcode.CASE_LARGE,
                Opcode.CASE_SMALL_EQUAL, Opcode.CASE_LARGE_EQUAL, Opcode.CASE_ETC, Opcode.CASE_OR,
                Opcode.CASE_AND, Opcode.CASE_FLAG, Opcode.CASE_BETWEEN
                -> CASE_OPCODES.contains(closing) || closing == Opcode.END_SWITCH;

            case Opcode.DO -> closing == Opcode.WHILE;

            case Opcode.INLINE_EVT, Opcode.INLINE_EVT_ID -> closing == Opcode.END_INLINE;

            case Opcode.BROTHER_EVT, Opcode.BROTHER_EVT_ID -> closing == Opcode.END_BROTHER;

            case Opcode.IF_STR_EQUAL, Opcode.IF_STR_NOT_EQUAL, Opcode.IF_STR_SMALL,
                Opcode.IF_STR_LARGE, Opcode.IF_STR_SMALL_EQUAL, Opcode.IF_STR_LARGE_EQUAL,
                Opcode.IFF_EQUAL, Opcode.IFF_NOT_EQUAL, Opcode.IFF_SMALL, Opcode.IFF_LARGE,
                Opcode.IFF_SMALL_EQUAL, Opcode.IFF_LARGE_EQUAL, Opcode.IF_EQUAL,
                Opcode.IF_NOT_EQUAL, Opcode.IF_SMALL, Opcode.IF_LARGE, Opcode.IF_SMALL_EQUAL,
                Opcode.IF_LARGE_EQUAL, Opcode.IF_FLAG, Opcode.IF_NOT_FLAG
                -> closing == Opcode.ELSE || closing == Opcode.END_IF;

            case Opcode.ELSE -> closing == Opcode.END_IF;

            default -> {
                Msg.warn(this, "Uh oh " + opening + " " + closing);
                throw new IllegalArgumentException();
            }
        };
    }

    @Override
    public Result checkScript(List<Instr> script) {
        List<Opcode> scopeStack = new ArrayList<>();

        for (int i = 0; i < script.size(); i++) {
            Instr instr = script.get(i);
            Opcode opcode = instr.opcode();

            for (int j = opcode.unindent(); j > 0; j--) {
                if (scopeStack.isEmpty())
                    return Result.error("Mismatched scope: " + opcode + " at " + i + " without opening");
                Opcode opening = scopeStack.removeLast();

                if (!checkUnindent(opening, opcode))
                    return Result.error("Mismatched scope: " + opcode + " at " + i + " tried closing " + opening);
            }

            for (int j = opcode.indent(); j > 0; j--) {
                scopeStack.add(opcode);
            }
        }
        if (!scopeStack.isEmpty())
            return Result.error("Unclosed scope: " + scopeStack.getLast());

        return Result.ok();
    }
}
