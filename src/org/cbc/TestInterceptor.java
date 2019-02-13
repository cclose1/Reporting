package org.cbc;

import org.cbc.application.reporting.Interceptor;
import org.cbc.application.reporting.InterceptorException;

public class TestInterceptor implements Interceptor
{
	public Object open(String Stream, String OpenInfo) throws InterceptorException
	{
		if (OpenInfo == null) throw new InterceptorException(100, "Stream " + Stream + " empty OpenInfo ");
		return null;
	}
        
        /**
         * Gives the interface the option to handle the report defined by
         * Details. Returns true if the report has been handled.
         */
        
	public boolean output(Object Control, boolean Reentered, String text, String duplicateKey) throws InterceptorException
	{
		return true;
	}
	public void close(Object Control)
	{
	}
        
}