package ghidraevt;

import java.awt.event.KeyEvent;

import docking.action.KeyBindingData;
import docking.action.MenuData;
import ghidra.app.util.AddEditDialog;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.symbol.Symbol;
import ghidra.util.*;

public class RenameSymbolAction extends AbstractEvtAction {
	public RenameSymbolAction() {
		super("Rename Symbol");
		setPopupMenuData(new MenuData(new String[] { "Rename Symbol" }, "Evt Disassembler"));
		setKeyBindingData(new KeyBindingData(KeyEvent.VK_L, 0));
	}

	@Override
	protected boolean isEnabledForEvtContext(EvtActionContext context) {
        return getSymbolHighlighted(context) != null;
	}

	@Override
	protected void evtActionPerformed(EvtActionContext context) {
		PluginTool tool = context.getTool();
		Symbol symbol = getSymbolHighlighted(context);
		if (symbol == null) {
			Msg.showError(this, tool.getToolFrame(), "Rename Failed",
				"Memory storage not found for symbol");
			return;
		}
		AddEditDialog dialog = new AddEditDialog("Rename Symbol", context.getTool());
		dialog.editLabel(symbol, context.getProgram());
	}
}
