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

    private PluginTool tool;
    private Program program;

    public EvtOptions(DecompileOptions decompileOptions) {
        this.decompileOptions = decompileOptions;
    }

    public void registerOptions(PluginTool tool, Program program) {
        registerToolOptions(tool);
        registerProgramOptions(program);
        // No need to re-register decompiler options

        grabFromToolAndProgram(tool, program);
    }

    public void registerListener(PluginTool tool, OptionsChangeListener listener) {
        for (String title : listenedCategories) {
            tool.getOptions(title).addOptionsChangeListener(listener);
        }
    }

    public boolean isCategoryListened(String title) {
        return listenedCategories.contains(title);
    }

    public void grabFromToolAndProgram(PluginTool tool, Program program) {
        grabFromTool(tool);
        this.tool = tool;
        grabFromProgram(program);
        this.program = program;

        // Update decompiler options
        ToolOptions fieldOptions = tool.getOptions(GhidraOptions.CATEGORY_BROWSER_FIELDS);
        ToolOptions decompilerOptions = tool.getOptions(DecompilePlugin.OPTIONS_TITLE);
        decompileOptions.grabFromToolAndProgram(fieldOptions, decompilerOptions, program);
    }

    /****************
     * Tool Options *
     ****************/

    private static final String TOPT_C_MACRO = "C Macro Mode";
    private static final String TOPT_C_MACRO_DESC = "Render scripts in the evt_cmd.h C macro format";
    private boolean cMacroMode;

    public boolean isCMacroMode() {
        return cMacroMode;
    }

    public void setCMacroMode(boolean cMacroMode) {
        this.cMacroMode = cMacroMode;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_C_MACRO, cMacroMode);
    }

    // TODO: another mode with type-based opt-in
    private static final String TOPT_STRICT = "Strict Script Detection";
    private static final String TOPT_STRICT_DESC = "Stricter script detection rules (may risk false-negatives)";
    private boolean strictMode;

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_STRICT, strictMode);
    }

    private static final String TOPT_LINE_NUMBERS = "Show Line Numbers";
    private static final String TOPT_LINE_NUMBERS_DESC = "Display instruction-based line numbers";
    private boolean showLineNumbers;

    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_LINE_NUMBERS, showLineNumbers);
    }

    private static final String TOPT_SYM_SNAP = "Snap to Last Symbol";
    private static final String TOPT_SYM_SNAP_DESC = "Start disassembly from the address of the last defined symbol";
    private boolean snapToSymbol;

    public boolean isSnapToSymbol() {
        return snapToSymbol;
    }

    public void setSnapToSymbol(boolean snapToSymbol) {
        this.snapToSymbol = snapToSymbol;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_SYM_SNAP, snapToSymbol);
    }

    private static final String TOPT_SYM_STOP = "Stop on Next Symbol";
    private static final String TOPT_SYM_STOP_DESC = "Cancel disassembly if the next defined symbol is reached";
    private boolean stopOnNextSymbol;

    public boolean isStopOnNextSymbol() {
        return stopOnNextSymbol;
    }

    public void setStopOnNextSymbol(boolean stopOnNextSymbol) {
        this.stopOnNextSymbol = stopOnNextSymbol;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_SYM_STOP, stopOnNextSymbol);
    }

    private static final String TOPT_LOCAL_XREFS = "Allow Local Variable Search";
    private static final String TOPT_LOCAL_XREFS_DESC = "Enable global reference searching for script-local variables.";
    private boolean allowLocalVarXrefs;

    public boolean isAllowLocalVarXrefs() {
        return allowLocalVarXrefs;
    }

    private static final String TOPT_NAMESPACES = "Display Namespaces";
    private static final String TOPT_NAMESPACES_DESC = "Display symbol namespaces in disassembly.";
    private boolean enableNamespaces;

    public boolean isEnableNamespaces() {
        return enableNamespaces;
    }

    public void setEnableNamespaces(boolean enableNamespaces) {
        this.enableNamespaces = enableNamespaces;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_NAMESPACES, enableNamespaces);
    }

    private static final String TOPT_MAX_WIDTH = "Maximum Line Width";
    private static final String TOPT_MAX_WIDTH_DESC = "Maximum characters within a line before wrapping.";
    private int maxWidth;

    public int getMaxWidth() {
        return maxWidth;
    }

    private static final String TOPT_STAY_ON_ERROR = "Stay on Error";
    private static final String TOPT_STAY_ON_ERROR_DESC = "Keep previous output when the new location is not a valid script.";
    private boolean stayOnError;

    public boolean isStayOnError() {
        return stayOnError;
    }

    public void setStayOnError(boolean stayOnError) {
        this.stayOnError = stayOnError;
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.setBoolean(TOPT_STAY_ON_ERROR, stayOnError);
    }

    public void registerToolOptions(PluginTool tool) {
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        toolOptions.registerOption(TOPT_C_MACRO,       false, null, TOPT_C_MACRO_DESC);
        toolOptions.registerOption(TOPT_STRICT,        true,  null, TOPT_STRICT_DESC);
        toolOptions.registerOption(TOPT_LINE_NUMBERS,  true,  null, TOPT_LINE_NUMBERS_DESC);
        toolOptions.registerOption(TOPT_SYM_SNAP,      true,  null, TOPT_SYM_SNAP_DESC);
        toolOptions.registerOption(TOPT_SYM_STOP,      true,  null, TOPT_SYM_STOP_DESC);
        toolOptions.registerOption(TOPT_LOCAL_XREFS,   false, null, TOPT_LOCAL_XREFS_DESC);
        toolOptions.registerOption(TOPT_NAMESPACES,    true,  null, TOPT_NAMESPACES_DESC);
        toolOptions.registerOption(TOPT_MAX_WIDTH,     100,   null, TOPT_MAX_WIDTH_DESC);
        toolOptions.registerOption(TOPT_STAY_ON_ERROR, true,  null, TOPT_STAY_ON_ERROR_DESC);
    }

    public void grabFromTool(PluginTool tool) {
        ToolOptions toolOptions = tool.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        this.cMacroMode         = toolOptions.getBoolean(TOPT_C_MACRO,       false);
        this.strictMode         = toolOptions.getBoolean(TOPT_STRICT,        true );
        this.showLineNumbers    = toolOptions.getBoolean(TOPT_LINE_NUMBERS,  true );
        this.snapToSymbol       = toolOptions.getBoolean(TOPT_SYM_SNAP,      true );
        this.stopOnNextSymbol   = toolOptions.getBoolean(TOPT_SYM_STOP,      true );
        this.allowLocalVarXrefs = toolOptions.getBoolean(TOPT_LOCAL_XREFS,   false);
        this.enableNamespaces   = toolOptions.getBoolean(TOPT_NAMESPACES,    true );
        this.maxWidth           =     toolOptions.getInt(TOPT_MAX_WIDTH,     100  );
        this.stayOnError        = toolOptions.getBoolean(TOPT_STAY_ON_ERROR, true );
    }

    /*******************
     * Program Options *
     *******************/

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

    public Game getGame() {
        return game;
    }

    public String getGameNamespace() {
        return switch (game) {
            case Game.SPM -> "spm";
            case Game.TTYD -> "ttyd";
            case null -> null;
        };
    }

    private void registerProgramOptions(Program program) {
        if (program == null)
            return;

        Options programOptions = program.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        programOptions.registerOption(POPT_GAME, GameChoice.AUTO, null, POPT_GAME_DESC);
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
    
    private void grabFromProgram(Program program) {
        if (program == null) {
            this.game = null;
            return;
        }

        Options programOptions = program.getOptions(GhidraEvtPlugin.OPTIONS_TITLE);
        this.game = decideGame(program, programOptions);
    }

    /**********************
     * Decompiler Options *
     **********************/

    private DecompileOptions decompileOptions;

    public DecompileOptions getDecompileOptions() {
        return decompileOptions;
    }

    public Font getDefaultFont() {
        return decompileOptions.getDefaultFont();
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

