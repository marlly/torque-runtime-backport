package org.apache.torque.engine.database.model;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 *    "Apache Turbine" must not be used to endorse or promote products 
 *    derived from this software without prior written permission. For 
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without 
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

import java.util.List;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * A <code>NameGenerator</code> implementation for names used globaly
 * throughout the system (such as constraint and index names).
 * Conforms to the maximum column name length, as specified by the
 * configured <code>DB</code> adapter.
 *
 * @author <a href="mailto:dlr@finemaltcoding.com>Daniel Rall</a>
 * @version $Id$
 */
public class GlobalNameGenerator implements NameGenerator
{
    private static final boolean DEBUG = true;

    /**
     * First element of <code>inputs</code> should be {@link
     * org.apache.torque.engine.database.model.AppData}.  Remaining
     * elements are concatenated together, separated by underscores.
     * Assumes the last element is the type identifier (spared if
     * length constraint trimming is necessary).  Returned name has
     * already been added to the set of global names maintained by the
     * provided <code>AppData</code>.
     *
     * @see org.apache.torque.engine.database.model.NameGenerator
     */
    public String generateName(List inputs)
        throws TorqueException
    {
        String name;
        StringBuffer buf = new StringBuffer();
        AppData appData = (AppData) inputs.get(0);

        int typeIndex = inputs.size() - 1;
        for (int i = 1; i < typeIndex; i++)
        {
            if (i > 1)
            {
                buf.append(STD_SEPARATOR_CHAR);
            }
            buf.append(inputs.get(i));
        }

        // Grab last element.
        String globalNameType = (String) inputs.get(typeIndex);

        // Enforce maximum column character limit for RDBMS
        // corresponding to the DB adatper in use.
        int maxBodyLength;
        try
        {
            maxBodyLength = (Torque.getDB().getMaxColumnNameLength() -
                             globalNameType.length());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        if (buf.length() >= maxBodyLength)
        {
            buf.setLength(maxBodyLength);
        }

        // Save off trimmable section in case of name collision.
        String trimmable = buf.toString();

        // Append last element, assumed to be a name type indicator.
        buf.append(STD_SEPARATOR_CHAR).append(globalNameType);
        name = buf.toString();

        if (DEBUG)
        {
            System.out.println("[1] name=" + name + " used=" +
                               appData.isUsedName(name));
        }

        if (appData.isUsedName(name))
        {
            // Remove one character from trimmable section to make
            // room for value to add to ensure uniqueness.
            buf.setLength(--maxBodyLength);
            buf.replace(0, maxBodyLength, trimmable);
            buf.append(STD_SEPARATOR_CHAR).append(globalNameType);

            String baseName = buf.toString();
            for (int i = 0; appData.isUsedName(name); i++)
            {
                switch (i)
                {
                case 0:
                    buf.append(i);
                    break;
                case 10:
                    throw new Error("Artificial limit for duplicate " +
                                    "global names exceeded: " + i +
                                    " duplicate global names");
                default:
                    buf.setCharAt(buf.length() - 1, Character.forDigit(i, 10));
                }
                name = buf.toString();

                if (DEBUG)
                {
                    System.out.println("[" + (i + 2) +"] name=" + name +
                                       " used=" + appData.isUsedName(name));
                }
            }
        }
        appData.markNameAsUsed(name);

        return name;
    }
}
