package ghidraevt;

import java.util.List;

import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.WrappingVerticalLayoutTextField;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;
import docking.widgets.fieldpanel.support.FieldLocation;

public class EvtTextField extends WrappingVerticalLayoutTextField {
    private EvtLine line;
    public EvtTextField(EvtLine line, FieldElement textElement, int startX, int width, int maxLines,
        FieldHighlightFactory hlFactory) {
			super(textElement, startX, width - startX, maxLines, hlFactory);
        // super(createSingleLineElement(fieldElements), x, width - x, 30, hlFactory, false, "");
        this.line = line;
    }

    public EvtToken getToken(FieldLocation loc) {
        if (loc == null) {
            return null;
        }

        int index = getTokenIndex(loc);
        return line.tokens().get(index);
    }

	FieldElement getClickedObject(FieldLocation fieldLocation) {
		return getFieldElement(fieldLocation.row, fieldLocation.col);
	}

	private int getTokenIndex(FieldLocation location) {
        List<EvtToken> tokens = line.tokens();

		int n = 0;
		for (int i = 0; i < tokens.size(); i++) {

			if (location.col == n) {
				return i;
			}

			EvtToken token = tokens.get(i);
		    n += token.getText().length();
			if (n > location.col) {
				return i;
			}
		}

		return tokens.size() - 1;
	}

}

