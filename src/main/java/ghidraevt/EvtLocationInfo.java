package ghidraevt;

import java.util.Objects;

import ghidra.framework.options.SaveState;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;

public class EvtLocationInfo {

	private Address entryPoint;
	private EvtResults results;
	private EvtToken token;
	private String tokenName;
	private int lineNumber;
	private int charPos;

	public EvtLocationInfo(Address entryPoint, EvtResults results,
			EvtToken token, int lineNumber, int charPos) {
		this.entryPoint = entryPoint;
		this.results = results;
		this.token = token;
		this.tokenName = token.getText();
		this.lineNumber = lineNumber;
		this.charPos = charPos;
	}

	/**
	 * Default constructor required for restoring a program location from XML.
	 */
	public EvtLocationInfo() {
	}

	public Address getFunctionEntryPoint() {
		return entryPoint;
	}

	/**
	 * Results from the decompilation
	 * 
	 * @return C-AST, DFG, and CFG object. null if there are no results attached to this location
	 */
	public EvtResults getDisassembly() {
		return results;
	}

	/**
	 * C text token at the current cursor location
	 * 
	 * @return token at this location, could be null if there are no decompiler results
	 */
	public EvtToken getToken() {
		return token;
	}

	public String getTokenName() {
		return tokenName;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getCharPos() {
		return charPos;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + charPos;
		result =
			prime * result + ((entryPoint == null) ? 0 : entryPoint.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + ((tokenName == null) ? 0 : tokenName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		EvtLocationInfo other = (EvtLocationInfo) obj;
		if (charPos != other.charPos) {
			return false;
		}

		if (lineNumber != other.lineNumber) {
			return false;
		}

		if (!Objects.equals(entryPoint, other.entryPoint)) {
			return false;
		}

		if (!Objects.equals(tokenName, other.tokenName)) {
			return false;
		}
		return true;
	}

	public void saveState(SaveState saveState) {
		saveState.putString("_FUNCTION_ENTRY", entryPoint.toString());
		saveState.putString("_TOKEN_TEXT", tokenName);
		saveState.putInt("_LINE_NUM", lineNumber);
		saveState.putInt("_CHAR_POS", charPos);
	}

	public void restoreState(Program program1, SaveState obj) {
		String addrStr = obj.getString("_FUNCTION_ENTRY", "0");
		entryPoint = program1.parseAddress(addrStr)[0];
		tokenName = obj.getString("_TOKEN_TEXT", "");
		lineNumber = obj.getInt("_LINE_NUM", 0);
		charPos = obj.getInt("_CHAR_POS", 0);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName());
		buf.append(", line=");
		buf.append(lineNumber);
		buf.append(", character=");
		buf.append(charPos);
		buf.append(", token=");
		buf.append(tokenName);
		return buf.toString();
	}
}
