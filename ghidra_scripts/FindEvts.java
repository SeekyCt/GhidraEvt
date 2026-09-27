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
//@category Seeky

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.util.Msg;
import ghidra.util.exception.CancelledException;
import jevt.Game;

public class FindEvts extends GhidraScript {
    public void run() throws CancelledException {
        File f = askFile("Output File", "Select output file");
        Game game = askChoice("Game", "Select game", Arrays.asList(Game.TTYD, Game.SPM), Game.SPM);
        var script = new ghidraevt.scripts.FindEvts();
        script.run(currentProgram, game, f);
    }
}
