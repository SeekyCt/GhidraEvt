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

import java.util.List;
import java.util.Map;

import jevt.Instr;
import jevt.Opcode;

public class CmdnValidator implements EvtValidator {
    public static Map<Opcode, Integer> expectedCmdns = Map.ofEntries(
        Map.entry(Opcode.END_SCRIPT, 0),
        Map.entry(Opcode.END_EVT, 0),
        Map.entry(Opcode.LBL, 1),
        Map.entry(Opcode.GOTO, 1),
        Map.entry(Opcode.DO, 1),
        Map.entry(Opcode.WHILE, 0),
        Map.entry(Opcode.DO_BREAK, 0),
        Map.entry(Opcode.DO_CONTINUE, 0),
        Map.entry(Opcode.WAIT_FRM, 1),
        Map.entry(Opcode.WAIT_MSEC, 1),
        Map.entry(Opcode.HALT, 1),
        Map.entry(Opcode.IF_STR_EQUAL, 2),
        Map.entry(Opcode.IF_STR_NOT_EQUAL, 2),
        Map.entry(Opcode.IF_STR_SMALL, 2),
        Map.entry(Opcode.IF_STR_LARGE, 2),
        Map.entry(Opcode.IF_STR_SMALL_EQUAL, 2),
        Map.entry(Opcode.IF_STR_LARGE_EQUAL, 2),
        Map.entry(Opcode.IFF_EQUAL, 2),
        Map.entry(Opcode.IFF_NOT_EQUAL, 2),
        Map.entry(Opcode.IFF_SMALL, 2),
        Map.entry(Opcode.IFF_LARGE, 2),
        Map.entry(Opcode.IFF_SMALL_EQUAL, 2),
        Map.entry(Opcode.IFF_LARGE_EQUAL, 2),
        Map.entry(Opcode.IF_EQUAL, 2),
        Map.entry(Opcode.IF_NOT_EQUAL, 2),
        Map.entry(Opcode.IF_SMALL, 2),
        Map.entry(Opcode.IF_LARGE, 2),
        Map.entry(Opcode.IF_SMALL_EQUAL, 2),
        Map.entry(Opcode.IF_LARGE_EQUAL, 2),
        Map.entry(Opcode.IF_FLAG, 2),
        Map.entry(Opcode.IF_NOT_FLAG, 2),
        Map.entry(Opcode.ELSE, 0),
        Map.entry(Opcode.END_IF, 0),
        Map.entry(Opcode.SWITCH, 1),
        Map.entry(Opcode.SWITCHI, 1),
        Map.entry(Opcode.CASE_EQUAL, 1),
        Map.entry(Opcode.CASE_NOT_EQUAL, 1),
        Map.entry(Opcode.CASE_SMALL, 1),
        Map.entry(Opcode.CASE_LARGE, 1),
        Map.entry(Opcode.CASE_SMALL_EQUAL, 1),
        Map.entry(Opcode.CASE_LARGE_EQUAL, 1),
        Map.entry(Opcode.CASE_ETC, 0),
        Map.entry(Opcode.CASE_OR, 1),
        Map.entry(Opcode.CASE_AND, 1),
        Map.entry(Opcode.CASE_FLAG, 1),
        Map.entry(Opcode.CASE_END, 0),
        Map.entry(Opcode.CASE_BETWEEN, 2),
        Map.entry(Opcode.SWITCH_BREAK, 0),
        Map.entry(Opcode.END_SWITCH, 0),
        Map.entry(Opcode.SET, 2),
        Map.entry(Opcode.SETI, 2),
        Map.entry(Opcode.SETF, 2),
        Map.entry(Opcode.ADD, 2),
        Map.entry(Opcode.SUB, 2),
        Map.entry(Opcode.MUL, 2),
        Map.entry(Opcode.DIV, 2),
        Map.entry(Opcode.MOD, 2),
        Map.entry(Opcode.ADDF, 2),
        Map.entry(Opcode.SUBF, 2),
        Map.entry(Opcode.MULF, 2),
        Map.entry(Opcode.DIVF, 2),
        Map.entry(Opcode.SET_READ, 1),
        Map.entry(Opcode.READ, 1),
        Map.entry(Opcode.READ2, 2),
        Map.entry(Opcode.READ3, 3),
        Map.entry(Opcode.READ4, 4),
        Map.entry(Opcode.READ_N, 2),
        Map.entry(Opcode.SET_READF, 1),
        Map.entry(Opcode.READF, 1),
        Map.entry(Opcode.READF2, 2),
        Map.entry(Opcode.READF3, 3),
        Map.entry(Opcode.READF4, 4),
        Map.entry(Opcode.READF_N, 2),
        Map.entry(Opcode.SET_USER_WRK, 1),
        Map.entry(Opcode.SET_USER_FLG, 1),
        Map.entry(Opcode.ALLOC_USER_WRK, 2),
        Map.entry(Opcode.AND, 2),
        Map.entry(Opcode.ANDI, 2),
        Map.entry(Opcode.OR, 2),
        Map.entry(Opcode.ORI, 2),
        Map.entry(Opcode.SET_FRAME_FROM_MSEC, 2),
        Map.entry(Opcode.SET_MSEC_FROM_FRAME, 2),
        Map.entry(Opcode.SET_RAM, 2),
        Map.entry(Opcode.SET_RAMF, 2),
        Map.entry(Opcode.GET_RAM, 2),
        Map.entry(Opcode.GET_RAMF, 2),
        Map.entry(Opcode.SETR, 2),
        Map.entry(Opcode.SETRF, 2),
        Map.entry(Opcode.GETR, 2),
        Map.entry(Opcode.GETRF, 2),
        // user_func
        Map.entry(Opcode.RUN_EVT, 1),
        Map.entry(Opcode.RUN_EVT_ID, 2),
        Map.entry(Opcode.RUN_CHILD_EVT, 1),
        Map.entry(Opcode.DELETE_EVT, 1),
        Map.entry(Opcode.RESTART_EVT, 1),
        Map.entry(Opcode.SET_PRI, 1),
        Map.entry(Opcode.SET_SPD, 1),
        Map.entry(Opcode.SET_TYPE, 1),
        Map.entry(Opcode.STOP_ALL, 1),
        Map.entry(Opcode.START_ALL, 1),
        Map.entry(Opcode.STOP_OTHER, 1),
        Map.entry(Opcode.START_OTHER, 1),
        Map.entry(Opcode.STOP_ID, 1),
        Map.entry(Opcode.START_ID, 1),
        Map.entry(Opcode.CHK_EVT, 2),
        Map.entry(Opcode.INLINE_EVT, 0),
        Map.entry(Opcode.INLINE_EVT_ID, 1),
        Map.entry(Opcode.END_INLINE, 0),
        Map.entry(Opcode.BROTHER_EVT, 0),
        Map.entry(Opcode.BROTHER_EVT_ID, 1),
        Map.entry(Opcode.END_BROTHER, 0),
        Map.entry(Opcode.DEBUG_PUT_MSG, 1),
        Map.entry(Opcode.DEBUG_MSG_CLEAR, 5), // conservative guess
        Map.entry(Opcode.DEBUG_PUT_REG, 1),
        Map.entry(Opcode.DEBUG_NAME, 1),
        Map.entry(Opcode.DEBUG_REM, 5), // conservative guess
        Map.entry(Opcode.DEBUG_BP, 5) // conservative guess
    );

    @Override
    public Result checkScript(List<Instr> script) {
        for (int i = 0; i < script.size(); i++) {
            Instr instr = script.get(i);
            Integer expected = expectedCmdns.get(instr.opcode());
            if (expected != null && instr.args().size() != expected)
                return Result.error("Unexpected cmdn for instruction");
        }

        return Result.ok();
    }
    
}
