package org.apache.torque;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * This is a base class of runtime exeptions thrown by Torque. <p>
 *
 * This class represents a non-checked type exception (see
 * {@link java.lang.RuntimeException}).
 * It is intended to ease the debugging by carrying on the information about the
 * exception which was caught and provoked throwing the current exception.
 * Catching and rethrowing may occur multiple times, and provided that all
 * exceptions except the first one are descendands of
 * <code>TorqueRuntimeException</code>, when the exception is finally printed
 * out using any of the <code>printStackTrace()</code> methods, the stacktrace
 * will contain the information about all exceptions thrown and caught on the
 * way.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @version $Id$
 */
public class TorqueRuntimeException
    extends RuntimeException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2997617341459640541L;

    /**
     * Holds the reference to the exception or error that caused
     * this exception to be thrown.
     */
    private Throwable nested = null;

    /**
     * Constructs a new <code>TorqueRuntimeException</code> without specified
     * detail message.
     */
    public TorqueRuntimeException()
    {
        super();
    }

    /**
     * Constructs a new <code>TorqueRuntimeException</code> with specified
     * detail message.
     *
     * @param msg the error message.
     */
    public TorqueRuntimeException(String msg)
    {
        super(msg);
    }

    /**
     * Constructs a new <code>TorqueRuntimeException</code> with specified
     * nested <code>Throwable</code>.
     *
     * @param nested the exception or error that caused this exception
     *               to be thrown.
     */
    public TorqueRuntimeException(Throwable nested)
    {
        super();
        this.nested = nested;
    }

    /**
     * Constructs a new <code>TorqueRuntimeException</code> with specified
     * detail message and nested <code>Throwable</code>.
     *
     * @param msg the error message.
     * @param nested the exception or error that caused this exception
     *               to be thrown.
     */
    public TorqueRuntimeException(String msg, Throwable nested)
    {
        super(msg);
        this.nested = nested;
    }

    /**
     * Prints the stack trace of this exception the the standar error stream.
     */
    public void printStackTrace()
    {
        synchronized (System.err)
        {
            printStackTrace(System.err);
        }
    }

    /**
     * Prints the stack trace of this exception to the specified print stream.
     *
     * @param out <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream out)
    {
        synchronized (out)
        {
            PrintWriter pw = new PrintWriter(out, false);
            printStackTrace(pw);
            // flush the PrintWriter before it's GCed
            pw.flush();
        }
    }

    /**
     * Prints the stack trace of this exception to the specified print writer.
     *
     * @param out <code>PrintWriter</code> to use for output.
     */
    public void printStackTrace(PrintWriter out)
    {
        synchronized (out)
        {
            printStackTrace(out, 0);
        }
    }

    /**
     * Prints the stack trace of this exception skiping a specified number
     * of stack frames.
     *
     * @param out <code>PrintWriter</code> to use for output.
     * @param skip the numbere of stack frames to skip.
     */
    public void printStackTrace(PrintWriter out, int skip)
    {
        String[] st = captureStackTrace();
        if (nested != null)
        {
            if (nested instanceof TorqueRuntimeException)
            {
                ((TorqueRuntimeException) nested)
                        .printStackTrace(out, st.length - 2);
            }
            else if (nested instanceof TorqueException)
            {
                ((TorqueException) nested).printStackTrace(out);
            }
            else
            {
                String[] nst = captureStackTrace(nested);
                for (int i = 0; i < nst.length - st.length + 2; i++)
                {
                    out.println(nst[i]);
                }
            }
            out.print("rethrown as ");
        }
        for (int i = 0; i < st.length - skip; i++)
        {
            out.println(st[i]);
        }
    }

    /**
     * Captures the stack trace associated with this exception.
     *
     * @return an array of Strings describing stack frames.
     */
    private String[] captureStackTrace()
    {
        StringWriter sw = new StringWriter();
        super.printStackTrace(new PrintWriter(sw, true));
        return splitStackTrace(sw.getBuffer().toString());
    }

    /**
     * Captures the stack trace associated with a <code>Throwable</code>
     * object.
     *
     * @param t the <code>Throwable</code>.
     * @return an array of Strings describing stack frames.
     */
    private String[] captureStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return splitStackTrace(sw.getBuffer().toString());
    }

    /**
     * Splits the stack trace given as a newline separated string
     * into an array of stack frames.
     *
     * @param stackTrace the stack trace.
     * @return an array of Strings describing stack frames.
     */
    private String[] splitStackTrace(String stackTrace)
    {
        String linebreak = System.getProperty("line.separator");
        StringTokenizer st = new StringTokenizer(stackTrace, linebreak);
        LinkedList list = new LinkedList();
        while (st.hasMoreTokens())
        {
            list.add(st.nextToken());
        }
        return (String[]) list.toArray();
    }
}
