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
package ghidraevt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;

import ghidra.app.events.ProgramActivatedPluginEvent;
import ghidra.app.events.ProgramOpenedPluginEvent;
import ghidra.app.events.ProgramLocationPluginEvent;
import ghidra.app.events.ProgramSelectionPluginEvent;
import ghidra.app.events.ProgramClosedPluginEvent;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.app.services.ClipboardService;
import ghidra.app.services.DataTypeManagerService;
import ghidra.app.services.GoToService;
import ghidra.app.services.NavigationHistoryService;
import ghidra.app.services.ProgramManager;
import ghidra.framework.model.DomainFile;
import ghidra.framework.options.SaveState;
import ghidra.framework.plugintool.Plugin;
import ghidra.framework.plugintool.PluginEvent;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.program.util.ProgramSelection;
import ghidra.util.Msg;
import ghidra.util.task.SwingUpdateManager;
import ghidraevt.component.EvtProvider;
import ghidraevt.component.hover.EvtHoverService;
import ghidraevt.component.hover.FunctionSignatureEvtHover;
import ghidraevt.component.hover.ReferenceEvtHover;
import ghidraevt.component.hover.ScalarValueEvtHover;
import ghidraevt.token.EvtToken;

/**
 * Provide class-level documentation that describes what this plugin does.
 */
//@formatter:off
@PluginInfo(
    status = PluginStatus.STABLE,
    packageName = GhidraEvtPluginPackage.NAME,
    category = PluginCategoryNames.COMMON,
    shortDescription = "Script disassembler window.",
    description = "Super Paper Mario evt script disassembly integration.",

    servicesRequired = {
        GoToService.class, NavigationHistoryService.class, ClipboardService.class,
        DataTypeManagerService.class /*, ProgramManager.class */
    },
    servicesProvided = {
        EvtHoverService.class,
    },
    eventsConsumed = {
        ProgramActivatedPluginEvent.class, ProgramOpenedPluginEvent.class,
        ProgramLocationPluginEvent.class, ProgramSelectionPluginEvent.class,
        ProgramClosedPluginEvent.class
    }
)

//@formatter:on
// TODO: ProgramPlugin can clean up
public class GhidraEvtPlugin extends Plugin {
    public static final String OPTIONS_TITLE = "Evt Disassembler";

    private EvtProvider connectedProvider;
    private List<EvtProvider> disconnectedProviders;

    private Program currentProgram;
    private ProgramLocation currentLocation;
    private ProgramSelection currentSelection;

    private FunctionSignatureEvtHover functionNameHoverService;
    private ScalarValueEvtHover scalarValueHoverService;
    private ReferenceEvtHover referenceHoverService;

    /**
     * Delay location changes to allow location events to settle down. This happens when a
     * readDataState occurs when a tool is restored or when switching program tabs.
     */
    SwingUpdateManager delayedLocationUpdateMgr = new SwingUpdateManager(200, 200, () -> {
        if (currentLocation == null) {
            return;
        }

        Program locationProgram = currentLocation.getProgram();
        if (locationProgram.isClosed()) {
            return; // not sure if this can happen
        }
        connectedProvider.setLocation(currentLocation, null);
    });

    /**
     * Plugin constructor.
     * 
     * @param tool The plugin tool that this plugin is added to.
     * @throws IOException 
     */
    public GhidraEvtPlugin(PluginTool tool) throws IOException {
        super(tool);

        disconnectedProviders = new ArrayList<>();
        connectedProvider = new EvtProvider(this, true);

		functionNameHoverService = new FunctionSignatureEvtHover(tool);
		registerServiceProvided(EvtHoverService.class, functionNameHoverService);
        scalarValueHoverService = new ScalarValueEvtHover(tool);
        registerServiceProvided(EvtHoverService.class, scalarValueHoverService);
        referenceHoverService = new ReferenceEvtHover(tool);
        registerServiceProvided(EvtHoverService.class, referenceHoverService);
    }

    @Override
    protected void init() {
        super.init();

        ClipboardService clipboardService = tool.getService(ClipboardService.class);
        if (clipboardService != null) {
            connectedProvider.setClipboardService(clipboardService);
            for (EvtProvider provider : disconnectedProviders) {
                provider.setClipboardService(clipboardService);
            }
        }
    }

    @Override
    public void writeDataState(SaveState saveState) {
        if (connectedProvider != null) {
            connectedProvider.writeDataState(saveState);
        }
        saveState.putInt("Num Disconnected", disconnectedProviders.size());
        int i = 0;
        for (EvtProvider provider : disconnectedProviders) {
            SaveState providerSaveState = new SaveState();
            DomainFile df = provider.getProgram().getDomainFile();
            if (df.getParent() == null) {
                continue; // not contained within project
            }
            String programPathname = df.getPathname();
            providerSaveState.putString("Program Path", programPathname);
            provider.writeDataState(providerSaveState);
            String elementName = "Provider" + i;
            saveState.putXmlElement(elementName, providerSaveState.saveToXml());
            i++;
        }
    }

    @Override
    public void readDataState(SaveState saveState) {
        ProgramManager programManagerService = tool.getService(ProgramManager.class);

        if (connectedProvider != null) {
            connectedProvider.readDataState(saveState);
        }
        int numDisconnected = saveState.getInt("Num Disconnected", 0);
        for (int i = 0; i < numDisconnected; i++) {
            Element xmlElement = saveState.getXmlElement("Provider" + i);
            SaveState providerSaveState = new SaveState(xmlElement);
            String programPath = providerSaveState.getString("Program Path", "");
            DomainFile file = tool.getProject().getProjectData().getFile(programPath);
            if (file == null) {
                continue;
            }
            Program program = programManagerService.openProgram(file);
            if (program != null) {
                EvtProvider provider = createNewDisconnectedProvider();
                provider.doSetProgram(program);
                provider.readDataState(providerSaveState);
            }
        }
    }

    public EvtProvider createNewDisconnectedProvider() {
        EvtProvider provider = new EvtProvider(this, false);
        provider.setClipboardService(tool.getService(ClipboardService.class));
        disconnectedProviders.add(provider);
        tool.showComponentProvider(provider, true);
        return provider;
    }

    @Override
    public void dispose() {

        currentProgram = null;

        if (connectedProvider != null) {
            removeProvider(connectedProvider);
        }
        for (EvtProvider provider : disconnectedProviders) {
            removeProvider(provider);
        }
        disconnectedProviders.clear();

    }

    public void exportLocation(Program program, ProgramLocation location) {
        GoToService service = tool.getService(GoToService.class);
        if (service != null) {
            service.goTo(location, program);
        }
    }

    public void updateSelection(EvtProvider provider, Program selProgram,
            ProgramSelection selection) {
        if (provider == connectedProvider) {
            firePluginEvent(new ProgramSelectionPluginEvent(name, selection, selProgram));
        }
    }

    public void closeProvider(EvtProvider provider) {
        if (provider == connectedProvider) {
            tool.showComponentProvider(provider, false);
        }
        else {
            disconnectedProviders.remove(provider);
            removeProvider(provider);
        }
    }

    public void locationChanged(EvtProvider provider, ProgramLocation location) {
        if (provider.shouldSendEvents()) {
            firePluginEvent(new ProgramLocationPluginEvent(name, location, location.getProgram()));
        }
    }

    public void selectionChanged(EvtProvider provider, ProgramSelection selection) {
        if (provider.shouldSendEvents()) {
            firePluginEvent(new ProgramSelectionPluginEvent(name, selection, currentProgram));
        }
    }

    public void handleTokenRenamed(EvtToken tokenAtCursor, String newName) {
        connectedProvider.handleTokenRenamed(tokenAtCursor, newName);
        for (EvtProvider provider : disconnectedProviders) {
            provider.handleTokenRenamed(tokenAtCursor, newName);
        }
    }

    private void removeProvider(EvtProvider provider) {
        tool.removeComponentProvider(provider);
        provider.dispose();
    }

    @Override
    public void processEvent(PluginEvent event) {
        if (event instanceof ProgramClosedPluginEvent) {
            Program program = ((ProgramClosedPluginEvent) event).getProgram();
            programClosed(program);
            return;
        }
        if (connectedProvider == null) {
            return;
        }

        if (event instanceof ProgramActivatedPluginEvent) {
            currentProgram = ((ProgramActivatedPluginEvent) event).getActiveProgram();
            connectedProvider.doSetProgram(currentProgram);
        }
        else if (event instanceof ProgramLocationPluginEvent) {
            ProgramLocation location = ((ProgramLocationPluginEvent) event).getLocation();
            Address address = location.getAddress();
            if (address.isExternalAddress()) {
                return;
            }
            if (currentProgram != null) {
                Listing listing = currentProgram.getListing();
                CodeUnit codeUnit = listing.getCodeUnitContaining(address);
                if (codeUnit instanceof Instruction) {
                    return;
                }
            }
            currentLocation = location;
            // Delay location change to allow immediate location changes to settle down.  This 
            // happens when switching program tabs in code browser which produces multiple location
            // changes
            delayedLocationUpdateMgr.updateLater();
        }
        else if (event instanceof ProgramSelectionPluginEvent) {
            currentSelection = ((ProgramSelectionPluginEvent) event).getSelection();
            connectedProvider.setSelection(currentSelection);
        }

    }

    private void programClosed(Program closedProgram) {
        Iterator<EvtProvider> iterator = disconnectedProviders.iterator();
        while (iterator.hasNext()) {
            EvtProvider provider = iterator.next();
            if (provider.getProgram() == closedProgram) {
                iterator.remove();
                removeProvider(provider);
            }
        }
    }

    public ProgramLocation getCurrentLocation() {
        return currentLocation;
    }

	@Override
	public void serviceAdded(Class<?> interfaceClass, Object service) {
		if (interfaceClass == EvtHoverService.class) {
			EvtHoverService hoverService = (EvtHoverService) service;
			connectedProvider.getEvtPanel().addHoverService(hoverService);
			for (EvtProvider provider : disconnectedProviders) {
				provider.getEvtPanel().addHoverService(hoverService);
			}
		}
	}

	@Override
	public void serviceRemoved(Class<?> interfaceClass, Object service) {
		if (interfaceClass == EvtHoverService.class) {
			EvtHoverService hoverService = (EvtHoverService) service;
			connectedProvider.getEvtPanel().removeHoverService(hoverService);
			for (EvtProvider provider : disconnectedProviders) {
				provider.getEvtPanel().removeHoverService(hoverService);
			}
		}
	}
}
