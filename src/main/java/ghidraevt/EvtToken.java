package ghidraevt;

import java.awt.Color;

public class EvtToken {
    private String text;
    private Color color;	

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
}
