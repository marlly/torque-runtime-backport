package org.apache.torque.om;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.util.ArrayList;
import org.apache.commons.lang.ObjectUtils;

/**
 * This class can be used as an ObjectKey to uniquely identify an
 * object within an application where the key consists of multiple
 * entities (such a String[] representing a multi-column primary key).
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:drfish@cox.net">J. Russell Smyth</a>
 * @version $Id$
 */
public class ComboKey extends ObjectKey
{
    // might want to shift these to TR.props

    /** The single character used to separate key values in a string. */
    public static final char SEPARATOR = ':';

    /** The single character used to separate key values in a string. */
    public static final String SEPARATOR_STRING = ":";

    /** The array of the keys */
    private SimpleKey[] key;

    /**
     * Creates an ComboKey whose internal representation will be
     * set later, through a set method
     */
    public ComboKey()
    {
    }

    /**
     * Creates a ComboKey whose internal representation is an
     * array of SimpleKeys.
     *
     * @param keys the key values
     */
    public ComboKey(SimpleKey[] keys)
    {
        setValue(keys);
    }

    /**
     * Sets the internal representation to a String array.
     *
     * @param keys the key values
     * @see #toString()
     */
    public ComboKey(String keys)
    {
        setValue(keys);
    }

    /**
     * Sets the internal representation using a SimpleKey array.
     *
     * @param keys the key values
     */
    public void setValue(SimpleKey[] keys)
    {
        this.key = keys;
    }

    /**
     * Sets the internal representation using a String of the
     * form produced by the toString method.
     *
     * @param keys the key values
     */
    public void setValue(String keys)
    {
        int startPtr = 0;
        int indexOfSep = keys.indexOf(SEPARATOR);
        ArrayList tmpKeys = new ArrayList();
        while (indexOfSep != -1)
        {
            if (indexOfSep == startPtr)
            {
                tmpKeys.add(null);
            }
            else
            {
                char keyType = keys.charAt(startPtr);
                String keyString = keys.substring(startPtr + 1, indexOfSep);

                SimpleKey newKey = null;
                switch(keyType)
                {
                    case 'N':
                        newKey = new NumberKey(keyString);
                        break;
                    case 'S':
                        newKey = new StringKey(keyString);
                        break;
                    case 'D':
                        try
                        {
                            newKey = new DateKey(keyString);
                        }
                        catch (NumberFormatException nfe)
                        {
                            newKey = new DateKey();
                        }
                        break;
                    default:
                        // unextepcted key type
                }
                tmpKeys.add(newKey);
            }
            startPtr = indexOfSep + 1;
            indexOfSep = keys.indexOf(SEPARATOR, startPtr);
        }

        this.key = new SimpleKey[tmpKeys.size()];
        for (int i = 0; i < this.key.length; i++)
        {
            this.key[i] = (SimpleKey) tmpKeys.get(i);
        }
    }

    /**
     * Sets the internal representation using a ComboKey.
     *
     * @param keys the key values
     */
    public void setValue(ComboKey keys)
    {
        setValue((SimpleKey[]) keys.getValue());
    }

    /**
     * Get the underlying object.
     *
     * @return the underlying object
     */
    public Object getValue()
    {
        return key;
    }

    /**
     * This method will return true if the conditions for a looseEquals
     * are met and in addition no parts of the keys are null.
     *
     * @param keyObj the comparison value
     * @return whether the two objects are equal
     */
    public boolean equals(Object keyObj)
    {
        boolean isEqual = false;

        if (key != null)
        {
            // check that all keys are not null
            isEqual = true;
            SimpleKey[] keys = key;
            for (int i = 0; i < keys.length && isEqual; i++)
            {
                isEqual &= keys[i] != null && keys[i].getValue() != null;
            }

            isEqual &= looseEquals(keyObj);
        }

        return isEqual;
    }

    /**
     * keyObj is equal to this ComboKey if keyObj is a ComboKey, String,
     * ObjectKey[], or String[] that contains the same information this key
     * contains.
     * For example A String[] might be equal to this key, if this key was
     * instantiated with a String[] and the arrays contain equal Strings.
     * Another example, would be if keyObj is an ComboKey that was
     * instantiated with a ObjectKey[] and this ComboKey was instantiated with
     * a String[], but the ObjectKeys in the ObjectKey[] were instantiated
     * with Strings that equal the Strings in this KeyObject's String[]
     * This method is not as strict as the equals method which does not
     * allow any null keys parts, while the internal key may not be null
     * portions may be, and the two object will be considered equal if
     * their null portions match.
     *
     * @param keyObj the comparison value
     * @return whether the two objects are equal
     */
    public boolean looseEquals(Object keyObj)
    {
        boolean isEqual = false;

        if (key != null)
        {
            // Checks  a compound key (ObjectKey[] or String[]
            // based) with the delimited String created by the
            // toString() method.  Slightly expensive, but should be less
            // than parsing the String into its constituents.
            if (keyObj instanceof String)
            {
                isEqual = toString().equals(keyObj);
            }
            // check against a ObjectKey. Two keys are equal, if their
            // internal keys equivalent.
            else if (keyObj instanceof ComboKey)
            {
                SimpleKey[] obj = (SimpleKey[])
                    ((ComboKey) keyObj).getValue();

                SimpleKey[] keys1 = key;
                SimpleKey[] keys2 = obj;
                isEqual = keys1.length == keys2.length;
                for (int i = 0; i < keys1.length && isEqual; i++)
                {
                    isEqual &= ObjectUtils.equals(keys1[i], keys2[i]);
                }
            }
            else if (keyObj instanceof SimpleKey[])
            {
                SimpleKey[] keys1 = key;
                SimpleKey[] keys2 = (SimpleKey[]) keyObj;
                isEqual = keys1.length == keys2.length;
                for (int i = 0; i < keys1.length && isEqual; i++)
                {
                    isEqual &= ObjectUtils.equals(keys1[i], keys2[i]);
                }
            }
        }
        return isEqual;
    }

    /**
     *
     * @param sb the StringBuffer to append
     * @see #toString()
     */
    public void appendTo(StringBuffer sb)
    {
        if (key != null)
        {
            SimpleKey[] keys = key;
            for (int i = 0; i < keys.length; i++)
            {
                if (keys[i] != null)
                {
                    if (keys[i] instanceof StringKey)
                    {
                        sb.append("S");
                    }
                    else if (keys[i] instanceof NumberKey)
                    {
                        sb.append("N");
                    }
                    else if (keys[i] instanceof DateKey)
                    {
                        sb.append("D");
                    }
                    else
                    {
                        // unknown type
                        sb.append("U");
                    }
                    keys[i].appendTo(sb);
                }
                // MUST BE ADDED AFTER EACH KEY, IN CASE OF NULL KEY!
                sb.append(SEPARATOR);
            }
        }
    }

    /**
     * if the underlying key array is not null and the first element is
     * not null this method returns the hashcode of the first element
     * in the key.  Otherwise calls ObjectKey.hashCode()
     *
     * @return an <code>int</code> value
     */
    public int hashCode()
    {
        if (key == null)
        {
            return super.hashCode();
        }

        SimpleKey sk = key[0];
        if (sk == null)
        {
            return super.hashCode();
        }

        return sk.hashCode();
    }

    /**
     * A String that may consist of one section or multiple sections
     * separated by a colon. <br/>
     * Each Key is represented by <code>[type N|S|D][value][:]</code>. <p/>
     * Example: <br/>
     * the ComboKey(StringKey("key1"), NumberKey(2)) is represented as
     * <code><b>Skey1:N2:</b></code>
     *
     * @return a String representation
     */
    public String toString()
    {
        StringBuffer sbuf = new StringBuffer();
        appendTo(sbuf);
        return sbuf.toString();
    }
}
