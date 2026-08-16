package ghidraevt;


import javax.swing.*;
import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.ToolBarData;
import resources.Icons;

public class DockingToggle extends DockingAction {
    private boolean enabled;
    private Runnable callback;

    private Icon enabledIcon;
    private Icon disabledIcon;

    public DockingToggle(String name, String owner, boolean enabled, Runnable callback) {
        super(name, owner);
        this.enabled = enabled;
        this.callback = callback;

        this.enabledIcon = Icons.ADD_ICON;
        this.disabledIcon = Icons.STOP_ICON;

        update();
    }

    public boolean enabled() {
        return enabled;
    }

    @Override
    public void actionPerformed(ActionContext context) {
        enabled = !enabled;
        update();
    }

    private void update() {
        String desc;
        Icon icon;
        if (enabled) {
            icon = enabledIcon;
            desc = getName() + " (enabled)";
        }
        else {
            icon = disabledIcon;
            desc = getName() + " (disabled)";
        }

        setDescription(desc);
        setToolBarData(new ToolBarData(icon, null));

        callback.run();
    }
}
