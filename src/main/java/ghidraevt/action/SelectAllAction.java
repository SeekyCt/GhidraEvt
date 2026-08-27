package ghidraevt.action;

import java.awt.event.KeyEvent;

import docking.ActionContext;
import docking.DockingUtils;
import docking.action.DockingAction;
import docking.action.KeyBindingData;
import docking.widgets.EventTrigger;
import ghidraevt.component.EvtPanel;

/**
 * Action for adding all fields to the current format.
 */
public class SelectAllAction extends DockingAction {
	EvtPanel panel;

	public SelectAllAction(String owner, EvtPanel panel) {
		super("Select All", owner);
		this.panel = panel;
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_A, DockingUtils.CONTROL_KEY_MODIFIER_MASK));
	}

	@Override
	public void actionPerformed(ActionContext context) {
		panel.selectAll(EventTrigger.GUI_ACTION);
	}
}
