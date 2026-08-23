package ghidraevt;

import generic.theme.GColor;

public class EvtToken {
	public final static int KEYWORD_COLOR = 0; // Constants must match EvtLayoutModel syntaxColor
	public final static int COMMENT_COLOR = 1;
	public final static int TYPE_COLOR = 2;
	public final static int FUNCTION_COLOR = 3;
	public final static int VARIABLE_COLOR = 4;
	public final static int CONST_COLOR = 5;
	public final static int PARAMETER_COLOR = 6;
	public final static int GLOBAL_COLOR = 7;
	public final static int DEFAULT_COLOR = 8;
	public final static int ERROR_COLOR = 9;
	public final static int SPECIAL_COLOR = 10;
	public final static int MAX_COLOR = 11;

    private String text;
    private int color;		

	public EvtToken(String txt) {
		this.text = txt;
		this.color = DEFAULT_COLOR;
	}

	public EvtToken(String txt, int color) {
		this.text = txt;
		this.color = color;
	}

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }
}
