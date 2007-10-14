package org.apache.torque;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests the class TorqueRuntimeException
 */
public class TorqueRuntimeExceptionTest extends BaseTestCase
{
    /**
     * Creates a new instance.
     *
     * @param name the name of the test case.
     */
    public TorqueRuntimeExceptionTest(String name)
    {
        super(name);
    }

    /**
     * Tests whether printstackTrace works.
     */
    public void testPrintStackTrace()
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        try
        {
            throw new TorqueRuntimeException();
        }
        catch (TorqueRuntimeException e)
        {
            e.printStackTrace(writer);
            assertTrue(stringWriter.toString().startsWith(
                    "org.apache.torque.TorqueRuntimeException"));
            assertTrue(stringWriter.toString().indexOf(
                    "org.apache.torque.TorqueRuntimeExceptionTest.testPrintStackTrace")
                        > 0);
        }
        
    }
}
