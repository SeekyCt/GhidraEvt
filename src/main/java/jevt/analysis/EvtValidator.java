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

public interface EvtValidator {
    public static class Result {
        boolean ok;
        String error;

        private Result(boolean ok, String errMessage) {
            this.ok = ok;
            this.error = errMessage;
        }

        public static Result ok()  {
            return new Result(true, null);
        }

        public static Result error(String error)  {
            return new Result(false, error);
        }

        public String getError() {
            return error;
        }

        public boolean isOk() {
            return ok;
        }
    }

    public Result checkScript(List<Instr> script);

    public static final EvtValidator[] VALIDATORS = {
        new BreakContinueValidator(),
        new CmdnValidator(),
        new LengthValidator(),
        new NextValidator(),
        new ReturnValidator(),
        new ScopeValidator(),
    };
}
