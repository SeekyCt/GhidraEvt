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

import jevt.Instr;

public class LengthValidator implements EvtValidator {

    @Override
    public Result checkScript(List<Instr> script) {
        if (script.size() < 2)
            return Result.error("Too small");
        return Result.ok();
    }
}
