package org.apache.torque.engine;

/*
 * Copyright 2002,2004 The Apache Software Foundation.
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
 */

import org.apache.commons.lang.exception.NestableException;

/**
 * The base class of all exceptions thrown by the engine.
 *
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:jvz@apache.org">Jason van Zyl</a>
 * @version $Id$
 */
public class EngineException extends NestableException
{

    /**
     * Constructs a new <code>EngineException</code> without specified detail
     * message.
     */
    public EngineException()
    {
    }

    /**
     * Constructs a new <code>EngineException</code> with specified detail
     * message.
     *
     * @param msg the error message.
     */
    public EngineException(String msg)
    {
        super(msg);
    }

    /**
     * Constructs a new <code>EngineException</code> with specified nested
     * <code>Throwable</code>.
     *
     * @param nested the exception or error that caused this exception
     *               to be thrown.
     */
    public EngineException(Throwable nested)
    {
        super(nested);
    }

    /**
     * Constructs a new <code>EngineException</code> with specified detail
     * message and nested <code>Throwable</code>.
     *
     * @param msg the error message.
     * @param nested the exception or error that caused this exception
     *               to be thrown.
     */
    public EngineException(String msg, Throwable nested)
    {
        super(msg, nested);
    }
}
