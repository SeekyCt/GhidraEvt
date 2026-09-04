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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class InstrTests {
    Instr readInstr(byte[] data) throws IOException, BadEvtException {
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
        return Instr.decode(Game.SPM, stream, false);
    } 
    
    byte[] writeInstr(Instr instr) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        instr.encode(Game.SPM, new DataOutputStream(stream));
        return stream.toByteArray();
    } 
    
    List<Instr> readDisassemble(byte[] data) throws IOException, BadEvtException {
        InputStream stream = new ByteArrayInputStream(data);
        return Instr.disassemble(Game.SPM, stream, false);
    }

    byte[] writeAssemble(List<Instr> script) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Instr.assemble(Game.SPM, script, stream);
        return stream.toByteArray();
    }

    byte[] bytes(int[] data) {
        byte[] ret = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            ret[i] = (byte) data[i];
        return ret;
    }

    @Test
    void test_reader() throws IOException, BadEvtException {
        assertEquals(
            new Instr(Opcode.END_SCRIPT, new ArrayList<>()),
            readInstr(bytes(new int[] {
                0x00, 0x00, 0x00, 0x01,
            }))
        );

        assertEquals(
            new Instr(Opcode.SET, Arrays.asList(new Arg.LW(0), new Arg.GSW(1))),
            readInstr(bytes(new int[] {
                0x00, 0x02, 0x00, 0x32,
                0xFE, 0x36, 0x3C, 0x80,
                0xF5, 0xDE, 0x01, 0x81,
            }))
        );
    }

    @Test
    void test_writer() throws IOException {
        assertArrayEquals(
            bytes(new int[] {
                0x00, 0x00, 0x00, 0x01,
            }),
            writeInstr(new Instr(Opcode.END_SCRIPT, new ArrayList<>()))
        );

        assertArrayEquals(
            bytes(new int[] {
                0x00, 0x02, 0x00, 0x32,
                0xFE, 0x36, 0x3C, 0x80,
                0xF5, 0xDE, 0x01, 0x81,
            }),
            writeInstr(new Instr(Opcode.SET, Arrays.asList(new Arg.LW(0), new Arg.GSW(1))))
        );
    }

    @Test
    void test_disassemble() throws IOException, BadEvtException {
        assertEquals(
            readDisassemble(bytes(new int[] {
                0x00, 0x02, 0x00, 0x32,
                0xFE, 0x36, 0x3C, 0x80,
                0xF5, 0xDE, 0x01, 0x81,
                0x00, 0x00, 0x00, 0x01,
            })),
            Arrays.asList(
                new Instr(Opcode.SET, Arrays.asList(new Arg.LW(0), new Arg.GSW(1))),
                new Instr(Opcode.END_SCRIPT, new ArrayList<>())
            )
        );
    }

    @Test
    void test_assemble() throws IOException {
        assertArrayEquals(
            writeAssemble(Arrays.asList(
                new Instr(Opcode.SET, Arrays.asList(new Arg.LW(0), new Arg.GSW(1))),
                new Instr(Opcode.END_SCRIPT, new ArrayList<>())
            )),
            bytes(new int[] {
                0x00, 0x02, 0x00, 0x32,
                0xFE, 0x36, 0x3C, 0x80,
                0xF5, 0xDE, 0x01, 0x81,
                0x00, 0x00, 0x00, 0x01,
            })
        );
    }

    // TODO: test strict mode
}
