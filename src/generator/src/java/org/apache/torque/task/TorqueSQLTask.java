package org.apache.torque.task;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
