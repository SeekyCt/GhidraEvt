package ghidraevt;

import java.awt.Color;

public class EvtToken {
    private String text;
    private Color color;
    private EvtLine lineParent;

	public EvtToken(String txt, Color color) {
		this.text = txt;
		this.color = color;
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
}
