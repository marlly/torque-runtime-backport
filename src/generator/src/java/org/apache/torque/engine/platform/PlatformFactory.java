package org.apache.torque.engine.platform;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Torque" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Torque", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory class responsible to create Platform objects that
 * define RDBMS platform specific behaviour.
 *
 * @author Thomas Mahler
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class PlatformFactory
{
    private static HashMap platforms = new HashMap();
    private static Log log = LogFactory.getLog(PlatformFactory.class);

    /**
     * Returns the Platform for a platform name.
     *
     * @param dbms name of the platform
     */
    public static Platform getPlatformFor(String dbms)
    {
        Platform result = null;
        String platformName = null;

        result = (Platform) getPlatforms().get(dbms);
        if (result == null)
        {
            try
            {
                platformName = getClassnameFor(dbms);
                Class platformClass = Class.forName(platformName);
                result = (Platform) platformClass.newInstance();

            }
            catch (Throwable t)
            {
                log.warn("problems with platform " + platformName 
                        + ": " + t.getMessage());
                log.warn("Torque will use PlatformDefaultImpl instead");

                result = new PlatformDefaultImpl();
            }
            getPlatforms().put(dbms, result); // cache the Platform
        }
        return result;
    }

    /**
     * compute the name of the concrete Class representing the Platform
     * specified by <code>platform</code>
     * 
     * @param platform the name of the platform as specified in the repository
     */
    private static String getClassnameFor(String platform)
    {
        String pf = "Default";
        if (platform != null)
        {
            pf = platform;
        }
        return "org.apache.torque.engine.platform.Platform" + pf.substring(0, 1).toUpperCase() + pf.substring(1) + "Impl";
    }

    /**
     * Gets the platforms.
     * 
     * @return Returns a HashMap
     */
    private static HashMap getPlatforms()
    {
        return platforms;
    }
}
