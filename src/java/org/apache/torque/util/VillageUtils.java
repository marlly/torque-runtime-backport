package org.apache.torque.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.om.SimpleKey;

import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.TableDataSet;

/**
 * Some Village related code factored out of the BasePeer.
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public final class VillageUtils
{
    /** The log. */
    private static Log log = LogFactory.getLog(VillageUtils.class);

    /**
     * Private constructor to prevent instantiation.
     *
     * Class contains only static method ans should therefore not be
     * instantiated.
     */
    private VillageUtils()
    {
    }

    /**
     * Convenience Method to close a Table Data Set without
     * Exception check.
     *
     * @param tds A TableDataSet
     */
    public static final void close(final TableDataSet tds)
    {
        if (tds != null)
        {
            try
            {
                tds.close();
            }
            catch (Exception ignored)
            {
                log.debug("Caught exception when closing a TableDataSet",
                        ignored);
            }
        }
    }

    /**
     * Convenience Method to close a Table Data Set without
     * Exception check.
     *
     * @param qds A TableDataSet
     */
    public static final void close(final QueryDataSet qds)
    {
        if (qds != null)
        {
            try
            {
                qds.close();
            }
            catch (Exception ignored)
            {
                log.debug("Caught exception when closing a QueryDataSet",
                        ignored);
            }
        }
    }

    /**
     * Convenience Method to close an Output Stream without
     * Exception check.
     *
     * @param os An OutputStream
     */
    public static final void close(final OutputStream os)
    {
        try
        {
            if (os != null)
            {
                os.close();
            }
        }
        catch (Exception ignored)
        {
            log.debug("Caught exception when closing an OutputStream",
                    ignored);
        }
    }

    /**
     * Converts a hashtable to a byte array for storage/serialization.
     *
     * @param hash The Hashtable to convert.
     * @return A byte[] with the converted Hashtable.
     * @throws Exception If an error occurs.
     */
    public static final byte[] hashtableToByteArray(final Hashtable hash)
        throws Exception
    {
        Hashtable saveData = new Hashtable(hash.size());
        byte[] byteArray = null;

        Iterator keys = hash.entrySet().iterator();
        while (keys.hasNext())
        {
            Map.Entry entry = (Map.Entry) keys.next();
            if (entry.getValue() instanceof Serializable)
            {
                saveData.put(entry.getKey(), entry.getValue());
            }
        }

        ByteArrayOutputStream baos = null;
        BufferedOutputStream bos = null;
        ObjectOutputStream out = null;
        try
        {
            // These objects are closed in the finally.
            baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos);
            out = new ObjectOutputStream(bos);

            out.writeObject(saveData);

            out.flush();
            bos.flush();
            baos.flush();
            byteArray = baos.toByteArray();
        }
        finally
        {
            close(out);
            close(bos);
            close(baos);
        }
        return byteArray;
    }

    /**
     * Factored out setting of a Village Record column from a Criteria Key
     *
     * @param crit The Criteria
     * @param key The Criterion Key
     * @param rec The Village Record
     * @param colName The name of the Column in the Record
     */
    public static final void setVillageValue(final Criteria crit,
            final String key,
            final Record rec,
            final String colName)
            throws Exception
    {
        // A village Record.setValue( String, Object ) would
        // be nice here.
        Object obj = crit.getValue(key);
        if (obj instanceof SimpleKey)
        {
            obj = ((SimpleKey) obj).getValue();
        }
        if (obj == null)
        {
            rec.setValueNull(colName);
        }
        else if (obj instanceof String)
        {
            rec.setValue(colName, (String) obj);
        }
        else if (obj instanceof Integer)
        {
            rec.setValue(colName,
                    crit.getInt(key));
        }
        else if (obj instanceof BigDecimal)
        {
            rec.setValue(colName, (BigDecimal) obj);
        }
        else if (obj instanceof Boolean)
        {
            rec.setValue(colName,
                    ((Boolean) obj).booleanValue());
        }
        else if (obj instanceof java.util.Date)
        {
            rec.setValue(colName,
                    (java.util.Date) obj);
        }
        else if (obj instanceof Float)
        {
            rec.setValue(colName,
                    crit.getFloat(key));
        }
        else if (obj instanceof Double)
        {
            rec.setValue(colName,
                    crit.getDouble(key));
        }
        else if (obj instanceof Byte)
        {
            rec.setValue(colName,
                    ((Byte) obj).byteValue());
        }
        else if (obj instanceof Long)
        {
            rec.setValue(colName,
                    crit.getLong(key));
        }
        else if (obj instanceof Short)
        {
            rec.setValue(colName,
                    ((Short) obj).shortValue());
        }
        else if (obj instanceof Hashtable)
        {
            rec.setValue(colName,
                    hashtableToByteArray((Hashtable) obj));
        }
        else if (obj instanceof byte[])
        {
            rec.setValue(colName, (byte[]) obj);
        }
    }
}
