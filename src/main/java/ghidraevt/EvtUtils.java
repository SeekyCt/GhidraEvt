package ghidraevt;

import java.util.ArrayList;
import java.util.List;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import docking.widgets.fieldpanel.support.FieldRange;
import docking.widgets.fieldpanel.support.FieldSelection;
import ghidra.app.decompiler.ClangLine;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.component.ClangTextField;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;

public class EvtUtils {
	private static boolean intersects(EvtToken token, AddressSetView addressSet) {
		Address minAddress = token.getMinAddress();
		if (minAddress == null) {
			return false;
		}
		Address maxAddress = token.getMaxAddress();
		maxAddress = maxAddress == null ? minAddress : maxAddress;
		return addressSet.intersects(minAddress, maxAddress);
	}

	public static Address getClosestAddress(Program program, EvtToken token) {

		Address address = token.getMinAddress();
		if (address != null) {
			return address;
		}
		EvtToken addressedToken = findClosestAddressedToken(token);
		if (addressedToken == null) {
			return null;
		}
		return addressedToken.getMinAddress();
	}


	public static AddressSet findClosestAddressSet(Program program, AddressSpace functionSpace,
			List<EvtToken> tokenList) {
		AddressSet addressSet = new AddressSet();
		for (EvtToken tok : tokenList) {
			addTokenAddressRangeToSet(addressSet, tok, functionSpace);
		}

		// If no tokens are addressed - look for something on the same line
		if (addressSet.isEmpty()) {
			EvtLine lastLine = null;
			for (EvtToken token : tokenList) {
				// Only check each line once
				if (token.getLineParent() != lastLine) {
					lastLine = token.getLineParent();
					token = findClosestAddressedToken(token);
					addTokenAddressRangeToSet(addressSet, token, functionSpace);
				}
			}
		}
		return addressSet;

	}

	private static void addTokenAddressRangeToSet(AddressSet addrs, EvtToken token,
			AddressSpace space) {
		if (token == null || token.getMinAddress() == null) {
			return;
		}
		Address minAddress = token.getMinAddress();
		Address maxAddress = token.getMaxAddress();
		maxAddress = maxAddress == null ? minAddress : maxAddress;
		addrs.addRange(minAddress, maxAddress);
	}

	private static EvtToken findClosestAddressedToken(EvtToken token) {
		if (token == null) {
			return null;
		}
		if (token.getMinAddress() != null) {
			return token;
		}

		List<EvtToken> lineTokens = token.getLineParent().getAllTokens();
		int tokIndex = -1;
		int lastIndex = lineTokens.size() - 1;
		for (int i = 0; i <= lastIndex; i++) {
			if (lineTokens.get(i) == token) {
				tokIndex = i;
				break;
			}
		}

		if (tokIndex != -1) {
			// look to the right
			for (int i = tokIndex + 1; i <= lastIndex; i++) {
				EvtToken tok = lineTokens.get(i);
				if (tok.getMinAddress() != null) {
					return tok;
				}
			}
			// look to the left
			for (int i = tokIndex - 1; i >= 0; i--) {
				EvtToken tok = lineTokens.get(i);
				if (tok.getMinAddress() != null) {
					return tok;
				}
			}
		}
		return null;
	}

	public static FieldSelection getFieldSelection(List<EvtToken> tokens) {
		FieldSelection fieldSelection = new FieldSelection();
		for (EvtToken clangToken : tokens) {
			EvtLine lineParent = clangToken.getLineParent();
			if (lineParent == null) {
				continue;
			}
			int lineNumber = lineParent.getLineNumber();
			// lineNumber is one-based, we need zero-based
			fieldSelection.addRange(lineNumber - 1, lineNumber);
		}
		return fieldSelection;
	}

    public static List<EvtToken> getTokensInSelection(FieldSelection selection, Field[] lines) {
		List<EvtToken> tokenList = new ArrayList<>();
		int numRanges = selection.getNumRanges();
		for (int i = 0; i < numRanges; i++) {
			FieldRange subSelectionRange = selection.getFieldRange(i);
			addTokensInSelectionRange(tokenList, subSelectionRange, lines);
		}
		return tokenList;
	}

	private static void addTokensInSelectionRange(List<EvtToken> tokenList,
			FieldRange selectionRange, Field[] lines) {

		FieldLocation start = selectionRange.getStart();
		FieldLocation end = selectionRange.getEnd();
		if (start.equals(end)) {
			return;
		}
		if (start.getIndex().intValue() == end.getIndex().intValue()) {
			// single row
			addTokens(tokenList, lines, start.getIndex().intValue(), start, end);
		}
		else {
			// add Tokens For First Line
			addTokens(tokenList, lines, start.getIndex().intValue(), start, null);

			// add Tokens for in between lines
			for (int i = start.getIndex().intValue() + 1; i < end.getIndex().intValue(); i++) {
				addTokens(tokenList, lines, i, null, null);
			}

			// add Tokens for last line
			addTokens(tokenList, lines, end.getIndex().intValue(), null, end);
		}

	}

	private static void addTokens(List<EvtToken> tokenList, Field[] lines, int lineNumber,
			FieldLocation start, FieldLocation end) {
		if (lineNumber >= lines.length) {
			return;
		}

		EvtTextField textLine = (EvtTextField) lines[lineNumber];
		int startIndex = getStartIndex(textLine, start);
		int endIndex = getEndIndex(textLine, end);
		if (startIndex >= endIndex) {
			// There is a bug in how the start and end field location get created when a line
			// wraps.  This is likely something we can fix if we can get an example that shows this
			// state.  For now, we are adding this error checking to prevent an exception in the
			// call below.
			return;
		}

		tokenList.addAll(textLine.getTokens().subList(startIndex, endIndex));
	}

	private static int getStartIndex(EvtTextField textLine, FieldLocation location) {
		if (location == null) {
			return 0;
		}

		int tokenIndex = textLine.getTokenIndex(location);
		return tokenIndex;
	}

	private static int getEndIndex(EvtTextField textLine, FieldLocation location) {
		if (location == null) {
			return textLine.getTokens().size();
		}
		if (location.row == 0 && location.col == 0) {
			return 0;
		}

		int nextTokenIndex = textLine.getNextTokenIndexStartingAfter(location);
		return nextTokenIndex;
	}

	public static Address findAddressBefore(Field[] lines, EvtToken token) {
		EvtLine lineParent = token.getLineParent();
		int lineNumber = lineParent.getLineNumber();
		for (int i = lineNumber - 1; i >= 0; i--) {
			EvtTextField textLine = (EvtTextField) lines[i];
			List<EvtToken> tokens = textLine.getTokens();
			EvtToken addressedToken = findClosestAddressedToken(tokens.get(0));
			if (addressedToken != null) {
				return addressedToken.getMinAddress();
			}
		}
		return null;
	}

    public static List<EvtToken> getTokensFromView(Field[] fields, Address address) {

		AddressSetView set = new AddressSet(address);
		List<EvtToken> result = new ArrayList<>();
		for (Field f : fields) {
			EvtTextField tf = (EvtTextField) f;
			List<EvtToken> fieldTokens = tf.getTokens();
			for (EvtToken token : fieldTokens) {
				if (intersects(token, set)) {
					result.add(token);
				}
			}
		}
		return result;
	}

	public static int findIndexOfFirstField(List<EvtToken> queryTokens, Field[] fields) {
		if (queryTokens.isEmpty()) {
			return -1;
		}

		for (int i = 0; i < fields.length; i++) {
			EvtTextField f = (EvtTextField) fields[i];
			List<EvtToken> fieldTokens = f.getTokens();
			for (int j = 0; j < fieldTokens.size(); j++) {
				EvtToken fieldToken = fieldTokens.get(j);
				if (queryTokens.contains(fieldToken)) {
					return i;
				}
			}
		}
		return -1;
	}

	public static List<EvtToken> getTokens(List<EvtLine> lines, AddressSetView addressSet) {
		List<EvtToken> tokenList = new ArrayList<>();
		collectTokens(tokenList, lines, addressSet);
		return tokenList;
	}

	private static void collectTokens(List<EvtToken> tokenList, List<EvtLine> lines, AddressSetView addressSet) {
		for (EvtLine line : lines) {
			for (EvtToken token : line.getAllTokens()) {
				if (intersects(token, addressSet)) {
					tokenList.add(token);
				}
			}
		}
	}
}
