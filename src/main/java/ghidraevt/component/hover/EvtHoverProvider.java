package ghidraevt.component.hover;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.app.plugin.core.hover.AbstractHoverProvider;
import ghidra.program.model.address.Address;
import ghidra.program.util.ProgramLocation;
import ghidraevt.component.EvtTextField;
import ghidraevt.token.EvtAddrToken;
import ghidraevt.token.EvtToken;

public class EvtHoverProvider extends AbstractHoverProvider {

	public EvtHoverProvider() {
		super("EvtHoverProvider");
	}

	public void addHoverService(EvtHoverService hoverService) {
		super.addHoverService(hoverService);
	}

	public void removeHoverService(EvtHoverService hoverService) {
		super.removeHoverService(hoverService);
	}

	@Override
	protected ProgramLocation getHoverLocation(FieldLocation fieldLocation, Field field,
			Rectangle fieldBounds, MouseEvent event) {

		if (!(field instanceof EvtTextField)) {
			return null;
		}

		EvtTextField decompilerField = (EvtTextField) field;
		EvtToken token = decompilerField.getToken(fieldLocation);

		if (token.getMinAddress() == null) {
			return null;
		}

        Address reference = null;
        if (token instanceof EvtAddrToken addr) {
            reference = addr.getTarget();
        }

		return new ProgramLocation(program, token.getMinAddress(), reference);
	}
}
