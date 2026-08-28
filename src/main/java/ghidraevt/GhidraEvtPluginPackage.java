package ghidraevt;

import ghidra.framework.plugintool.util.PluginPackage;
import resources.Icons;
import resources.ResourceManager;

public class GhidraEvtPluginPackage extends PluginPackage {
	public static final String NAME = "GhidraEvt";
	
	public GhidraEvtPluginPackage() {
		super(NAME, Icons.INFO_ICON, "TTYD/SPM evt script support", FEATURE_PRIORITY);
	}
}
