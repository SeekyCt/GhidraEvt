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

