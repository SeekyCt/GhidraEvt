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
 * Modified from Ghidra's decompiler UI source code to work on evt scripts
 */
package ghidraevt.component;

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
