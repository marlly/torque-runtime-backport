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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import org.apache.velocity.context.Context;

import org.xml.sax.SAXException;

import org.apache.torque.engine.EngineException;
import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.transform.XmlToData;

/**
 * An extended Texen task used for generating SQL source from an XML data file
 *
 * @author <a href="mailto:jvanzyl@periapt.com"> Jason van Zyl </a>
 * @author <a href="mailto:jmcnally@collab.net"> John McNally </a>
 * @author <a href="mailto:fedor.karpelevitch@home.com"> Fedor Karpelevitch </a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class TorqueDataSQLTask extends TorqueDataModelTask
{
    /** the XML data file */
    private String dataXmlFile;
    /** the data dtd file */
    private String dataDTD;

    /**
     * The target database(s) we are generating SQL for. Right now we can only
     * deal with a single target, but we will support multiple targets soon.
     */
    private String targetDatabase;

    /**
     * Sets the DataXmlFile attribute of the TorqueDataSQLTask object
     *
     * @param  dataXmlFile The new DataXmlFile value
     */
    public void setDataXmlFile(String dataXmlFile)
    {
        this.dataXmlFile = project.resolveFile(dataXmlFile).toString();
    }

    /**
     * Gets the DataXmlFile attribute of the TorqueDataSQLTask object
     *
     * @return  The DataXmlFile value
     */
    public String getDataXmlFile()
    {
        return dataXmlFile;
    }

    /**
     * Get the current target database.
     *
     * @return  String target database(s)
     */
    public String getTargetDatabase()
    {
        return targetDatabase;
    }

    /**
     * Set the current target database.  This is where generated java classes
     * will live.
     *
     * @param  v The new TargetDatabase value
     */
    public void setTargetDatabase(String v)
    {
        targetDatabase = v;
    }

    /**
     * Gets the DataDTD attribute of the TorqueDataSQLTask object
     *
     * @return  The DataDTD value
     */
    public String getDataDTD()
    {
        return dataDTD;
    }

    /**
     * Sets the DataDTD attribute of the TorqueDataSQLTask object
     *
     * @param  dataDTD The new DataDTD value
     */
    public void setDataDTD(String dataDTD)
    {
        this.dataDTD = project.resolveFile(dataDTD).toString();
    }

    /**
     * Set up the initial context for generating the SQL from the XML schema.
     *
     * @return the context
     * @throws Exception If there is an error parsing the data xml.
     */
    public Context initControlContext() throws Exception
    {
        super.initControlContext();

        if (dataXmlFile == null && filesets.isEmpty())
        {
            throw new BuildException("You must specify an XML data file or "
                    + "a fileset of XML data files!");
        }

        try
        {
            Database db = (Database) getDataModels().get(0);
            
            List data;
            
            if (dataXmlFile != null)
            {
                XmlToData dataXmlParser = new XmlToData(db, dataDTD);
                data = dataXmlParser.parseFile(dataXmlFile);
            }
            else
            {
                data = new ArrayList();
                
                // Deal with the filesets.
                for (int i = 0; i < filesets.size(); i++)
                {
                    FileSet fs = (FileSet) filesets.get(i);
                    DirectoryScanner ds = fs.getDirectoryScanner(project);
                    File srcDir = fs.getDir(project);

                    String[] dataModelFiles = ds.getIncludedFiles();
                    
                    // Make a transaction for each file
                    for (int j = 0; j < dataModelFiles.length; j++)
                    {
                        File f = new File(srcDir, dataModelFiles[j]);
                        XmlToData dataXmlParser = new XmlToData(db, dataDTD);
                        List newData = dataXmlParser.parseFile(f.toString());

                        for (Iterator it = newData.iterator(); it.hasNext();)
                        {
                            data.add(it.next());
                        }
                    }
                }
            }
            context.put("data", data);

            // Place our model in the context.
            context.put("appData", db);

            // Place the target database in the context.
            context.put("targetDatabase", targetDatabase);

            Properties p = new Properties();
            FileInputStream fis = new FileInputStream(getSqlDbMap());
            p.load(fis);
            fis.close();
            
            p.setProperty(getOutputFile(), db.getName());
            p.store(new FileOutputStream(getSqlDbMap()), "Sqlfile -> Database map");
        }
        catch (EngineException ee)
        {
            throw new BuildException(ee);
        }
        catch (SAXException se)
        {
            throw new BuildException(se);
        }

        return context;
    }
}
