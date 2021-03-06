package core.languageHandler.compiler;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;

import argo.jdom.JsonNode;
import core.languageHandler.Language;
import core.userDefinedTask.UserDefinedAction;
import utilities.ILoggable;
import utilities.Pair;

public abstract class AbstractNativeCompiler implements ILoggable {

	public abstract Pair<DynamicCompilerOutput, UserDefinedAction> compile(String source);
	public abstract Pair<DynamicCompilerOutput, UserDefinedAction> compile(String source, File objectFile);
	public abstract Language getName();
	public abstract String getExtension();
	public abstract String getObjectExtension();

	public abstract File getPath();
	public abstract boolean canSetPath();
	public abstract boolean setPath(File path);

	public abstract boolean parseCompilerSpecificArgs(JsonNode node);
	public abstract JsonNode getCompilerSpecificArgs();

	protected abstract File getSourceFile(String compilingAction);
	protected abstract String getDummyPrefix();

	/*******************************************************************/
	/************************Swing components***************************/
	/*******************************************************************/
	public abstract void promptChangePath(JFrame parent);
	public abstract void changeCompilationButton(JButton bCompile);

	/**
	 * Show a swing component for the user to configure the parameters specific to this compiler.
	 */
	public abstract void configure();
}
