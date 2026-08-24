package ghidraevt;

import java.awt.Color;

import ghidra.program.model.address.Address;

public class EvtToken {
    private String text;
    private Color color;
    private EvtLine lineParent;
    Address minAddress;
    Address maxAddress;

	public EvtToken(String txt, Color color, Address minAddress, long size) {
		this.text = txt;
		this.color = color;
        this.minAddress = minAddress;
        if (minAddress != null)
            this.maxAddress = minAddress.add(size);
        else
            this.maxAddress = null;
	}

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    public void setLineParent(EvtLine lineParent) {
        this.lineParent = lineParent;
    }

    public EvtLine getLineParent() {
        return lineParent;
    }

	public Address getMinAddress() {
		return minAddress;
	}

    public Address getMaxAddress() {
		return maxAddress;
	}
}
