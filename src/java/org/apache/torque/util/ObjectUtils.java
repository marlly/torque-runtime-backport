package org.apache.torque.util;

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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This is where common Object manipulation routines should go.
 *
 * @author <a href="mailto:nissim@nksystems.com">Nissim Karpenstein</a>
 * @version $Id$
 */
public class ObjectUtils
{
    /**
     * Returns a default value if the object passed is null.
     *
     * @param o The object to test.
     * @param dflt The default value to return.
     * @return The object o if it is not null, dflt otherwise.
     */
    public static Object isNull(Object o,
                                Object dflt)
    {
        if (o == null)
        {
            return dflt;
        }
        else
        {
            return o;
        }
    }

    /**
     * Adds an object to a vector, making sure the object is in the
     * vector only once.
     *
     * @param v The vector.
     * @param o The object.
     */
    public static void addOnce(Vector v,
                               Object o)
    {
        if (! v.contains( o ))
        {
            v.addElement( o );
        }
    }

    /**
     * Deserializes a single object from an array of bytes.
     *
     * @param objectData The serialized object.
     * @return The deserialized object, or <code>null</code> on failure.
     */
    public static Object deserialize( byte[] objectData )
    {
        Object object = null;
        if (objectData != null)
        {
            // These streams are closed in finally.
            ObjectInputStream in = null;
            ByteArrayInputStream bin =
                new ByteArrayInputStream(objectData);
            BufferedInputStream bufin =
                new BufferedInputStream(bin);
            try
            {
                in = new ObjectInputStream(bufin);

                // If objectData has not been initialized, an
                // exception will occur.
                object = in.readObject();
            }
            catch (Exception e)
            {
            }
            finally
            {
                try
                {
                    if (in != null) in.close();
                    if (bufin != null) bufin.close();
                    if (bin != null) bin.close();
                }
                catch(IOException e)
                {
                }
            }
        }
        return object;
    }

    /**
     * Compares two Objects, returns true if their values are the
     * same.  It checks for null values prior to an o1.equals(o2)
     * check
     *
     * @param o1 The first object.
     * @param o2 The second object.
     * @return True if the values of both xstrings are the same.
     */
    public static boolean equals( Object o1,
                                  Object o2 )
    {
        if (o1 == null)
        {
            return (o2 == null);
        }
        else if (o2 == null)
        {
            // o1 is not null
            return false;
        }
        else
        {
            return o1.equals(o2);
        }
    }

    /**
     * Nice method for adding data to a Hashtable in such a way
     * as to not get NPE's. The point being that if the
     * value is null, Hashtable.put() will throw an exception.
     * That blows in the case of this class cause you may want to
     * essentially treat put("Not Null", null ) == put("Not Null", "")
     * We will still throw a NPE if the key is null cause that should
     * never happen.
     */
    public static final void safeAddToHashtable(Hashtable hash, Object key, Object value)
        throws NullPointerException
    {
        if (value == null)
        {
            hash.put ( key, "" );
        }
        else
        {
           hash.put ( key, value );
        }
    }

}
