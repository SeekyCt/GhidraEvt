package ghidraevt;

import docking.widgets.fieldpanel.field.FieldElement;
import docking.widgets.fieldpanel.field.WrappingVerticalLayoutTextField;
import docking.widgets.fieldpanel.support.FieldHighlightFactory;

public class EvtTextField extends WrappingVerticalLayoutTextField {
    public EvtTextField(FieldElement textElement, int startX, int width, int maxLines,
        FieldHighlightFactory hlFactory) {
        // super(createSingleLineElement(fieldElements), x, width - x, 30, hlFactory, false, "");

        super(textElement, startX, width - startX, maxLines, hlFactory);
    }
}

