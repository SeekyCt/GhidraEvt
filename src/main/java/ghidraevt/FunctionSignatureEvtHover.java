package ghidraevt;

import javax.swing.JComponent;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.GhidraOptions;
import ghidra.app.plugin.core.hover.AbstractConfigurableHover;
import ghidra.app.util.ToolTipUtils;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;

/**
 * A hover service to show tool tip text for hovering over a function name in the decompiler.
 * The tooltip shows the function signature per the listing.
*/
public class FunctionSignatureEvtHover extends AbstractConfigurableHover
		implements EvtHoverService {
	private static final String NAME = "Function Signature Display (evt)";
	private static final String DESCRIPTION =
		"Show function signatures when hovering over a function name.";

	// note: this is relative to other EvtHovers; a higher priority gets called first
	private static final int PRIORITY = 20;

	protected FunctionSignatureEvtHover(PluginTool tool) {
		super(tool, PRIORITY);
	}

	@Override
	protected String getName() {
		return NAME;
	}

	@Override
	protected String getDescription() {
		return DESCRIPTION;
	}

	@Override
	protected String getOptionsCategory() {
		return GhidraOptions.CATEGORY_DECOMPILER_POPUPS; // TODO
	}

	@Override
	public JComponent getHoverComponent(Program program, ProgramLocation programLocation,
			FieldLocation fieldLocation, Field field) {

		if (!enabled) {
			return null;
		}

		if (!(field instanceof EvtTextField)) {
			return null;
		}

		EvtToken token = ((EvtTextField) field).getToken(fieldLocation);
		if (token instanceof EvtAddrToken addr) {
			Function function = program.getFunctionManager().getFunctionAt(addr.getTarget());
			if (function == null) {
				return null; // no function in program; maybe bad address
			}
			String content = ToolTipUtils.getToolTipText(function, false);
			return createTooltipComponent(content);
		}

		return null;
	}
}
