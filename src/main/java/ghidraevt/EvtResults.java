package ghidraevt;

import java.util.List;

import ghidra.app.decompiler.ClangTokenGroup;
import ghidra.program.model.listing.Data;
import jevt.Instr;

/**
 * Class for getting at the various structures returned
 * by the decompiler.  Depending on how the DecompInterface
 * was called, you can get C code (with markup), the
 * function' syntax tree, the prototype, etc.
 * 
 * To check if the decompileFunction call completed normally
 * use the decompileCompleted method.  If this returns false,
 * the getErrorMessage method may contain a useful error
 * message.  Its also possible that getErrorMessage will
 * return warning messages, even if decompileFunction did
 * complete.
 * 
 * To get the resulting C code, marked up with XML in terms
 * of the lines and tokens, use the getCCodeMarkup method.
 * 
 * To get the resulting C code just as a straight String,
 * use the getDecompiledFunction method which returns a
 * DecompiledFunction.  Off of this, you can use the getC
 * method to get the raw C code as a String or use the
 * getSignature method to get the functions prototype as
 * a String.
 * 
 * To get the syntax tree use the getHighFunction method.
 * 
 * 
 *
 */
public class EvtResults {
	private Data data; // Function to which results pertain
	private String errMsg; // Error message from decompiler

	private List<Instr> script;


	public EvtResults(Data d, List<Instr> script, String e) {
		this.data = d;
		this.script = script;
		this.errMsg = e;
	}

	/**
	 * Returns true if the decompilation producing these
	 * results completed without aborting.  If it was
	 * aborted, there will be no real results in this
	 * object, and an error message should be available via
	 * getErrorMessage.
	 * @return true if the decompilation completed.
	 */
	public boolean decompileCompleted() {
		return script != null;
	}

	public Data getData() {
		return data;
	}

	/** 
	 * Returns true if the decompile completed normally
	 * @return true if the decompile completed normally
	 */
	public boolean isValid() {
		return errMsg == null;
	}

	/**
	 * Return any error message associated with the
	 * decompilation producing these results.  Generally,
	 * there will only be an error if the decompilation was
	 * aborted for some reason, but there could conceivably
	 * be warnings obtainable via this method, even if the
	 * decompilation did complete.
	 * @return any error message associated with these results
	 */
	public String getErrorMessage() {
		return errMsg;
	}

	/**
	 * Get the marked up C code associated with these
	 * decompilation results. If there was an error, or
	 * code generation was turned off, return null
	 * @return the resulting root of C markup
	 */
	public List<Instr> getCCodeMarkup() {
		return script;
	}

	/**
	 * Converts the C code results into an unadorned string.
	 * The returned object contains both the whole function
	 * and just the prototype as separate strings containing
	 * raw C code
	 * @return a DecompiledFunction object
	 */
	// public DecompiledFunction getDecompiledFunction() {
	// 	if (docroot == null) {
	// 		return null;
	// 	}
	// 	PrettyPrinter printer =
	// 		new PrettyPrinter(function, docroot, new IllegalCharCppTransformer());
	// 	return printer.print();
	// }
}
