package org.apache.torque.task;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

import org.apache.velocity.context.Context;

import org.apache.torque.engine.EngineException;
import org.apache.torque.engine.database.transform.XmlToAppData;
import org.apache.torque.engine.database.model.Database;


/**
 * An extended Texen task used for generating SQL source from
 * an XML schema describing a database structure.
 *
 * @author <a href="mailto:jvanzyl@periapt.com">Jason van Zyl</a>
 * @author <a href="mailto:jmcnally@collab.net>John McNally</a>
 * @version $Id$
 */
public class TorqueSQLTask extends TorqueDataModelTask
{
    // if the database is set than all generated sql files
    // will be placed in the specified database, the database
    // will not be taken from the data model schema file.

    private String database;
    private String suffix = "";

    private String idTableXMLFile = null;

    /**
     *
     * @param database
     */
    public void setDatabase(String database)
    {
        this.database = database;
    }

    /**
     *
     * @return
     */
    public String getDatabase()
    {
        return database;
    }

    /**
     *
     * @param suffix
     */
    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
    }

    /**
     *
     * @return
     */
    public String getSuffix()
    {
        return suffix;
    }

    /**
     * Set the path to the xml schema file that defines the id-table, used
     * by the idbroker method.
     *
     * @param idXmlFile xml schema file
     */
    public void setIdTableXMLFile(String idXmlFile)
    {
        idTableXMLFile = idXmlFile;
    }

    /**
     * Gets the id-table xml schema file path.
     *
     * @return Path to file.
     */
    public String getIdTableXMLFile()
    {
        return idTableXMLFile;
    }

    /**
     * create the sql -> database map.
     *
     * @throws Exception
     */
    private void createSqlDbMap() throws Exception
    {
        if (getSqlDbMap() == null)
        {
            return;
        }

        // Produce the sql -> database map
        Properties sqldbmap = new Properties();

        // Check to see if the sqldbmap has already been created.
        File file = new File(getSqlDbMap());

        if (file.exists())
        {
            FileInputStream fis = new FileInputStream(file);
            sqldbmap.load(fis);
            fis.close();
        }

        Iterator i = getDataModelDbMap().keySet().iterator();

        while (i.hasNext())
        {
            String dataModelName = (String) i.next();
            String sqlFile = dataModelName + suffix + ".sql";

            String databaseName;

            if (getDatabase() == null)
            {
                databaseName = (String) getDataModelDbMap().get(dataModelName);
            }
            else
            {
                databaseName = getDatabase();
            }

            sqldbmap.setProperty(sqlFile, databaseName);
        }

        sqldbmap.store(new FileOutputStream(getSqlDbMap()),
                "Sqlfile -> Database map");
    }

    /**
     * Create the database model necessary for the IDBroker tables.
     * We use the model to generate the necessary SQL to create
     * these tables.  This method adds an AppData object containing
     * the model to the context under the name "idmodel".
     */
    public void loadIdBrokerModel()
            throws EngineException
    {
        // Transform the XML database schema into
        // data model object.
        XmlToAppData xmlParser = new XmlToAppData(getTargetDatabase(), null);
        Database ad = xmlParser.parseFile(getIdTableXMLFile());

        ad.setName("idmodel");
        context.put("idmodel", ad);
    }

    /**
     * Place our target database and target platform
     * values into the context for use in the templates.
     *
     * @return the context
     * @throws Exception
     */
    public Context initControlContext() throws Exception
    {
        super.initControlContext();
        try
        {
            createSqlDbMap();

            // If the load path for the id broker table xml schema is
            // defined then load it.
            String f = getIdTableXMLFile();
            if (f != null && f.length() > 0)
            {
                loadIdBrokerModel();
            }
        }
        catch (EngineException ee)
        {
            throw new BuildException(ee);
        }

        return context;
    }
}
