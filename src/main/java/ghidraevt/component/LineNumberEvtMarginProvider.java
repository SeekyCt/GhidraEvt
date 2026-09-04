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
 * Modified from ghidra/app/decompiler/component/margin/DecompilerMarginProvider.java to work on evt
 * scripts
 */
package ghidraevt.component;

import java.awt.*;
import java.math.BigInteger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import docking.util.GraphicsUtils;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import ghidra.app.decompiler.component.margin.LayoutPixelIndexMap;
import ghidra.program.model.listing.Program;
import ghidraevt.token.EvtLine;

public class LineNumberEvtMarginProvider extends JPanel
		implements EvtMarginProvider, LayoutModelListener {

	private LayoutPixelIndexMap pixmap;
	private EvtLayoutModel model;

	public LineNumberEvtMarginProvider() {
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
	}

	@Override
	public void setProgram(Program program, EvtLayoutModel model, LayoutPixelIndexMap pixmap) {
		setLayoutManager(model);
		this.pixmap = pixmap;
		repaint();
	}

	private void setLayoutManager(EvtLayoutModel model) {
		if (this.model == model) {
			return;
		}
		if (this.model != null) {
			this.model.removeLayoutModelListener(this);
		}
		this.model = model;
		setWidthForLastLine();
		if (this.model != null) {
			this.model.addLayoutModelListener(this);
		}
	}

	@Override
	public void setOptions(EvtOptions options) {
		this.setFont(options.getDefaultFont());
		setWidthForLastLine();
		repaint();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public void modelSizeChanged(IndexMapper indexMapper) {
		setWidthForLastLine();
		repaint();
	}

	@Override
	public void dataChanged(BigInteger start, BigInteger end) {
		repaint();
	}

	private void setWidthForLastLine() {
		if (model == null) {
			return;
		}
		int lastLine = model.getNumIndexes().intValueExact();
		int width = getFontMetrics(getFont()).stringWidth(Integer.toString(lastLine));
		Insets insets = getInsets();
		width += insets.left + insets.right;
		setPreferredSize(new Dimension(Math.max(16, width), 0));
		invalidate();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Insets insets = getInsets();
		int rightEdge = getWidth() - insets.right;
		Rectangle visible = getVisibleRect();
		BigInteger startIdx = pixmap.getIndex(visible.y);
		BigInteger endIdx = pixmap.getIndex(visible.y + visible.height);
		int ascent = g.getFontMetrics().getMaxAscent();
		for (BigInteger i = startIdx; i.compareTo(endIdx) <= 0; i = i.add(BigInteger.ONE)) {
			EvtLine line = model.getLines().get(i.intValueExact());
			
			// Only render real line numbers
			if (line.getLineNumber() <= 0)
				continue;

			String text = Integer.toString(line.getLineNumber());
			int width = g.getFontMetrics().stringWidth(text);
			GraphicsUtils.drawString(this, g, text, rightEdge - width, pixmap.getPixel(i) + ascent);
		}
	}
}
