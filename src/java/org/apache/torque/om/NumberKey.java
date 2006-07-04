package org.apache.torque.om;

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

import java.math.BigDecimal;

/**
 * This class can be used as an ObjectKey to uniquely identify an
 * object within an application where the id  consists
 * of a single entity such a GUID or the value of a db row's primary key.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @author <a href="mailto:rg@onepercentsoftware.com">Runako Godfrey</a>
 * @version $Id$
 */
public class NumberKey extends SimpleKey
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5566819786708264162L;

    /**
     * Creates a NumberKey whose internal representation will be
     * set later, through a set method
     */
    public NumberKey()
    {
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     *
     * @param key the key value
     */
    public NumberKey(String key)
    {
        this.key = new BigDecimal(key);
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     *
     * @param key the key value
     */
    public NumberKey(BigDecimal key)
    {
        this.key = key;
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     *
     * @param key the key value
     */
    public NumberKey(NumberKey key)
    {
        if (key != null)
        {
            this.key = key.getValue();
        }
        else
        {
            this.key = null;
        }
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     *
     * @param key the key value
     */
    public NumberKey(long key)
    {
        this.key = BigDecimal.valueOf(key);
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     *
     * @param key the key value
     */
    public NumberKey(double key)
    {
        this.key = new BigDecimal(key);
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     * Convenience only.
     *
     * @param key the key value
     */
    public NumberKey(int key)
    {
        this((long) key);
    }

    /**
     * Creates a NumberKey equivalent to <code>key</code>.
     * Convenience only.
     *
     * @param key the key value
     */
    public NumberKey(Number key)
    {
        if (key != null)
        {
            this.key = new BigDecimal(key.toString());
        }
        else
        {
            this.key = null;
        }
    }

    /**
     * Sets the internal representation using a String representation
     * of a number
     *
     * @param key the key value
     * @throws NumberFormatException if key is not a valid number
     */
    public void setValue(String key) throws NumberFormatException
    {
        this.key = new BigDecimal(key);
    }

    /**
     * Sets the underlying object
     *
     * @param key the key value
     */
    public void setValue(BigDecimal key)
    {
        this.key = key;
    }

    /**
     * Sets the internal representation to the same object used by key.
     *
     * @param key the key value
     */
    public void setValue(NumberKey key)
    {
        this.key = (key == null ? null : key.getValue());
    }

    /**
     * Access the underlying BigDecimal object.
     *
     * @return a <code>BigDecimal</code> value
     */
    public BigDecimal getBigDecimal()
    {
        return (BigDecimal) key;
    }

    /**
     * Two ObjectKeys that both contain null values <strong>are not</strong>
     * considered equal.
     *
     * @param keyObj the key to compare values to
     * @return whether the two objects are equal
     */
    public boolean equals(Object keyObj)
    {
        if (keyObj == this)
        {
            return true;
        }

        if (!(keyObj instanceof NumberKey))
        {
            // NumberKeys used to be comparable to Strings.  This behavior has
            // been changed, I don't think it is a good idea to fail silently
            // as code may be dependent on the old behavior.
            if (keyObj instanceof String)
            {
                throw new IllegalArgumentException(
                    "NumberKeys are not comparable to Strings");
            }

            return false;
        }

        if (getValue() != null)
        {
            return getValue().equals(((NumberKey) keyObj).getValue());
        }
        else
        {
            // Even if they are both null...still return false.
            return false;
        }
    }

    /**
     * @return a hash code based on the value
     */
    public int hashCode()
    {
        if (getValue() == null)
        {
            return super.hashCode();
        }
        else
        {
            return getValue().hashCode();
        }
    }

    /**
     * @param o the comparison value
     * @return a numeric comparison of the two values
     */
    public int compareTo(Object o)
    {
        return getBigDecimal().compareTo(((NumberKey) o).getBigDecimal());
    }

    /**
     * Invokes the toString() method on the object.  An empty string
     * is returned is the value is null.
     *
     * @return a String representation of the key value
     */
    public String toString()
    {
        if (key != null)
        {
            return key.toString();
        }
        return "";
    }

    /**
     * Returns the value of this NumberKey as a byte. This value is subject
     * to the conversion rules set out in
     * {@link java.math.BigDecimal#byteValue()}
     *
     * @return the NumberKey converted to a byte
     */
    public byte byteValue()
    {
        return getBigDecimal().byteValue();
    }

    /**
     * Returns the value of this NumberKey as an int. This value is subject
     * to the conversion rules set out in
     * {@link java.math.BigDecimal#intValue()}, importantly any fractional part
     * will be discarded and if the underlying value is too big to fit in an
     * int, only the low-order 32 bits are returned. Note that this
     * conversion can lose information about the overall magnitude and
     * precision of the NumberKey value as well as return a result with the
     * opposite sign.
     *
     * @return the NumberKey converted to an int
     */
    public int intValue()
    {
        return getBigDecimal().intValue();
    }

    /**
     * Returns the value of this NumberKey as a short. This value is subject
     * to the conversion rules set out in
     * {@link java.math.BigDecimal#intValue()}, importantly any fractional part
     *  will be discarded and if the underlying value is too big to fit
     * in a long, only the low-order 64 bits are returned. Note that this
     * conversion can lose information about the overall magnitude and
     * precision of the NumberKey value as well as return a result with the
     * opposite sign.
     *
     * @return the NumberKey converted to a short
     */
    public short shortValue()
    {
        return getBigDecimal().shortValue();
    }

    /**
     * Returns the value of this NumberKey as a long. This value is subject
     * to the conversion rules set out in
     * {@link java.math.BigDecimal#intValue()}
     *
     * @return the NumberKey converted to a long
     */
    public long longValue()
    {
        return getBigDecimal().longValue();
    }

    /**
     * Returns the value of this NumberKey as a float. This value is subject to
     * the conversion rules set out in
     * {@link java.math.BigDecimal#floatValue()}, most importantly if the
     * underlying value has too great a magnitude to represent as a
     * float, it will be converted to Float.NEGATIVE_INFINITY
     * or Float.POSITIVE_INFINITY as appropriate.
     *
     * @return the NumberKey converted to a float
     */
    public float floatValue()
    {
        return getBigDecimal().floatValue();
    }

    /**
     * Returns the value of this NumberKey as a double. This value is subject
     * to the conversion rules set out in
     * {@link java.math.BigDecimal#doubleValue()}, most importantly if the
     * underlying value has too great a magnitude to represent as a
     * double, it will be converted to Double.NEGATIVE_INFINITY
     * or Double.POSITIVE_INFINITY as appropriate.
     *
     * @return the NumberKey converted to a double
     */
    public double doubleValue()
    {
        return getBigDecimal().doubleValue();
    }
}
