package ghidraevt;

import java.awt.event.KeyEvent;
import java.util.Objects;

import docking.action.KeyBindingData;
import docking.action.MenuData;
import ghidra.app.util.AddEditDialog;
import ghidra.app.util.HelpTopics;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.HelpLocation;
import ghidra.util.UndefinedFunction;

public class RenameFunctionAction extends AbstractEvtAction {
	public RenameFunctionAction() {
		super("Rename Function");
		setKeyBindingData(new KeyBindingData(KeyEvent.VK_L, 0));
		setPopupMenuData(new MenuData(new String[] { "Rename Function" }, "Evt Disassembler"));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
		Function func = getFunction(context);
		return func != null && !(func instanceof UndefinedFunction);
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		Program program = context.getProgram();
		Function function = getFunction(context);
		AddEditDialog dialog = new AddEditDialog("Edit Function Name", context.getTool());
		Symbol symbol = function.getSymbol();
		String originalName = symbol.getName();
		dialog.editLabel(symbol, program);

		String currentName = symbol.getName();
		if (Objects.equals(originalName, currentName)) {
			return; // no change
		}

		EvtProvider provider = context.getComponentProvider();
		EvtToken tokenAtCursor = context.getTokenAtCursor();
		provider.tokenRenamed(tokenAtCursor, currentName);

	}
}
