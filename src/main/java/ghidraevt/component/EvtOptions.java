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
import java.util.Arrays;
import java.util.List;

import ghidra.GhidraOptions;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.plugin.core.decompile.DecompilePlugin;
import ghidra.framework.options.Options;
import ghidra.framework.options.OptionsChangeListener;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.util.Msg;
import ghidraevt.GhidraEvtPlugin;
import jevt.Game;

public class EvtOptions {
    private static List<String> listenedCategories = Arrays.asList(
        GhidraEvtPlugin.OPTIONS_TITLE,
        DecompilePlugin.OPTIONS_TITLE,
        GhidraOptions.CATEGORY_BROWSER_FIELDS
    );

    private static final String TOPT_C_MACRO = "C Macro Mode";
    private static final String TOPT_C_MACRO_DESC = "Render scripts in the evt_cmd.h C macro format";
    private boolean cMacroMode;

    private static final String POPT_GAME = "Game";
    private static final String POPT_GAME_DESC = "Choose which game's constants to base disassembly on";
    public static enum GameChoice {
        AUTO("AUTO", "Auto-detect"),
        TTYD("TTYD", "Paper Mario: The Thousand-Year Door"),
        SPM("SPM", "Super Paper Mario");

		private String label;
		private String optionString;

		private GameChoice(String optString, String label) {
			this.label = label;
			this.optionString = optString;
		}

		public String getOptionString() {
			return optionString;
		}

		@Override
		public String toString() {
			return label;
		}
	}
    private Game game;

    private DecompileOptions decompileOptions;

    public EvtOptions(DecompileOptions decompileOptions) {
        this.decompileOptions = decompileOptions;
    }

    public DecompileOptions getDecompileOptions() {
        return decompileOptions;
    }

    public void registerOptions(PluginTool tool, Program program) {
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.registerOption(TOPT_C_MACRO, false, null, TOPT_C_MACRO_DESC);
        
        if (program != null) {
            Options programOptions = program.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
            programOptions.registerOption(POPT_GAME, GameChoice.AUTO, null, POPT_GAME_DESC);
        }

        // No need to re-register decompiler options
        ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        ToolOptions decompilerOptions = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
        decompileOptions.grabFromToolAndProgram(fieldOptions, decompilerOptions, program);
    }

    public void registerListener(PluginTool tool, OptionsChangeListener listener) {
        for (String title : listenedCategories) {
            tool.getOptions(title).addOptionsChangeListener(listener);
        }
    }

    public boolean isCategoryListened(String title) {
        return listenedCategories.contains(title);
    }

    private Game decideGame(Program program, Options programOptions) {
        switch (programOptions.getEnum(POPT_GAME, GameChoice.AUTO)) {
            case GameChoice.SPM:
                return Game.SPM;

            case GameChoice.TTYD:
                return Game.TTYD;

            default:
                // SPM has memcpy here, TTYD does not
                Address maybeMemcpy = program.getAddressFactory().getAddress("0x80004000");
                try {
                    return switch (program.getMemory().getByte(maybeMemcpy)) {
                        case 0x00 -> Game.TTYD;
                        case 0x7c -> Game.SPM;
                        default -> null;
                    };
                }
                catch (MemoryAccessException e) {
                    Msg.warn(this, "Couldn't do memcpy check: " + e.getMessage());
                    return null;
                }
        }
    }

    public void grabFromToolAndProgram(PluginTool tool, Program program) {
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        this.cMacroMode = toolOptions.getBoolean(TOPT_C_MACRO, false);

        grabFromProgram(program);

        // Update decompiler options
        ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        ToolOptions decompilerOptions = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
        decompileOptions.grabFromToolAndProgram(fieldOptions, decompilerOptions, program);
    }

    private void grabFromProgram(Program program) {
        if (program == null) {
            this.game = null;
            return;
        }

        Options programOptions = program.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        this.game = decideGame(program, programOptions);
    }


    /****************
     * Tool Options *
     ****************/

    public boolean getCMacroMode() {
        return cMacroMode;
    }


    /*******************
     * Program Options *
     *******************/

    public Game getGame() {
        return game;
    }

    /**********************
     * Decompiler Options *
     **********************/

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

