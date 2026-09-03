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
 * Modified from ghidra/app/decompiler/DecompileOptions.java to work on evt scripts
 */
package ghidraevt.component;

import java.awt.Color;
import java.awt.Font;

import ghidra.app.decompiler.DecompileOptions;
import ghidra.framework.options.ToolOptions;
import ghidra.program.model.listing.Program;

public class EvtOptions {
    private DecompileOptions decompileOptions;

    public EvtOptions(DecompileOptions decompileOptions) {
        this.decompileOptions = decompileOptions;
    }

    public DecompileOptions getDecompileOptions() {
        return decompileOptions;
    }

    public void registerOptions(ToolOptions fieldOptions, ToolOptions opt, Program program) {

        decompileOptions.grabFromToolAndProgram(fieldOptions, opt, program);
    }

    public void grabFromToolAndProgram(ToolOptions fieldOptions, ToolOptions opt, Program program) {
        decompileOptions.grabFromToolAndProgram(fieldOptions, opt, program);
    }

    public Font getDefaultFont() {
        return decompileOptions.getDefaultFont();
    }

    public int getMaxWidth() {
        return decompileOptions.getMaxWidth();
    }

    public boolean isDisplayLineNumbers() {
        return decompileOptions.isDisplayLineNumbers();
    }

    public Color getGlobalColor() {
        return decompileOptions.getGlobalColor();
    }

    public Color getConstantColor() {
        return decompileOptions.getConstantColor();
    }

    public Color getDefaultColor() {
        return decompileOptions.getDefaultColor();
    }

    public Color getVariableColor() {
        return decompileOptions.getVariableColor();
    }

    public Color getBackgroundColor() {
        return decompileOptions.getBackgroundColor();
    }

    public Color getActiveSearchHighlightColor() {
        return decompileOptions.getActiveSearchHighlightColor();
    }
    public Color getSearchHighlightColor() {
        return decompileOptions.getSearchHighlightColor();
    }
    public Color getCurrentVariableHighlightColor() {
        return decompileOptions.getCurrentVariableHighlightColor();
    }
    public Color getMiddleMouseHighlightColor() {
        return decompileOptions.getMiddleMouseHighlightColor();
    }
    public int getMiddleMouseHighlightButton() {
        return decompileOptions.getMiddleMouseHighlightButton();
    }
}

