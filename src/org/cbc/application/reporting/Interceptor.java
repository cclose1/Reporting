package org.cbc.application.reporting;
/**
 *  @version <b>v1.0, 11/Jun/01, C.B. Close:</b>  Initial version.
 */
public interface Interceptor
{
	/**
	 * Opens an external interface for Stream. OpenInfo is a string the
	 * contents of which are defined by the implementation of the interface.
	 *
	 * The returned object is defined by the implementation and is passed
	 * in the remaining calls for the interface.
	 */
	public Object open(String stream, String openInfo) throws InterceptorException;
	/**
	 * Gives the interface the option to handle the report defined by
	 * Details. Returns true if the report has been handled.
	 */
	public boolean output(Object control, boolean reentered, String text, String duplicateKey) throws InterceptorException;
	/**
	 * Closes the external interface.
	 */
	
	public void close(Object Control);
}
