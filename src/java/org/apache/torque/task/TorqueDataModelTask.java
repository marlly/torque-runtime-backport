package org.apache.torque.task;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.torque.engine.database.model.AppData;
import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.transform.XmlToAppData;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.texen.ant.TexenTask;

/**
 * A base torque task that uses either a single XML schema
 * representing a data model, or a &lt;fileset&gt; of XML schemas.
 * We are making the assumption that an XML schema representing
 * a data model contains tables for a <strong>single</strong>
 * database.
 *
 * @author <a href="mailto:jvanzyl@zenplex.com">Jason van Zyl</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 */
public class TorqueDataModelTask extends TexenTask
{
    /**
     *  XML that describes the database model, this is transformed
     *  into the application model object.
     */
    protected String xmlFile;

    /**
     * Fileset of XML schemas which represent our data models.
     */
    protected List filesets = new ArrayList();

    /**
     * Data models that we collect. One from each XML schema file.
     */
    protected List dataModels = new ArrayList();

    /**
     * Velocity context which exposes our objects
     * in the templates.
     */
    protected Context context;

    /**
     * Map of data model name to database name.
     * Should probably stick to the convention
     * of them being the same but I know right now
     * in a lot of cases they won't be.
     */
    protected Hashtable dataModelDbMap;

    /**
     * Hashtable containing the names of all the databases
     * in our collection of schemas.
     */
    protected Hashtable databaseNames;

    //!! This is probably a crappy idea having the sql file -> db map
    // here. I can't remember why I put it here at the moment ...
    // maybe I was going to map something else. It can probably
    // move into the SQL task.

    /**
     * Name of the properties file that maps an SQL file
     * to a particular database.
     */
    protected String sqldbmap;

    /**
     * The path to properties file containing db idiosyncrasies is
     * constructed by appending the "getTargetDatabase()/db.props
     * to this path.
     */
    private String basePathToDbProps;

    /**
     * The target database(s) we are generating SQL
     * for. Right now we can only deal with a single
     * target, but we will support multiple targets
     * soon.
     */
    private String targetDatabase;

    /**
     * Target Java package to place the generated files in.
     */
    private String targetPackage;


    /**
     * Set the sqldbmap.
     *
     * @param sqldbmap th db map
     */
    public void setSqlDbMap(String sqldbmap)
    {
        //!! Make all these references files not strings.
        this.sqldbmap = project.resolveFile(sqldbmap).toString();
    }

    /**
     * Get the sqldbmap.
     *
     * @return String sqldbmap.
     */
    public String getSqlDbMap()
    {
        return sqldbmap;
    }

    /**
     * Return the data models that have been
     * processed.
     *
     * @return List data models
     */
    public List getDataModels()
    {
        return dataModels;
    }

    /**
     * Return the data model to database name map.
     *
     * @return Hashtable data model name to database name map.
     */
    public Hashtable getDataModelDbMap()
    {
        return dataModelDbMap;
    }

    /**
     * Get the xml schema describing the application model.
     *
     * @return  String xml schema file.
     */
    public String getXmlFile()
    {
        return xmlFile;
    }

    /**
     * Set the xml schema describing the application model.
     *
     * @param xmlFile The new XmlFile value
     */
    public void setXmlFile(String xmlFile)
    {
        this.xmlFile = project.resolveFile(xmlFile).toString();
    }

    /**
     * Adds a set of xml schema files (nested fileset attribute).
     *
     * @param set a Set of xml schema files
     */
    public void addFileset(FileSet set)
    {
        filesets.add(set);
    }

    /**
     * Get the current target database.
     *
     * @return String target database(s)
     */
    public String getTargetDatabase()
    {
        return targetDatabase;
    }

    /**
     * Set the current target database. (e.g. mysql, oracle, ..)
     *
     * @param v target database(s)
     */
    public void setTargetDatabase(String v)
    {
        targetDatabase = v;
    }

    /**
     * Get the current target package.
     *
     * @return return target java package.
     */
    public String getTargetPackage()
    {
        return targetPackage;
    }

    /**
     * Set the current target package. This is where generated java classes will
     * live.
     *
     * @param v target java package.
     */
    public void setTargetPackage(String v)
    {
        targetPackage = v;
    }

    /**
     * The path to properties file containing db idiosyncrasies is
     * constructed by appending the "getTargetDatabase()/db.props to this path.
     *
     * @return basepath to db.props
     */
    public String getBasePathToDbProps()
    {
        return basePathToDbProps;
    }

    /**
     * The path to properties file containing db idiosyncrasies is
     * constructed by appending the "getTargetDatabase()/db.props
     * to this path.
     *
     * @param v basepath to db.props
     */
    public void setBasePathToDbProps(String  v)
    {
        this.basePathToDbProps = v;
    }

    /**
     * Set up the initial context for generating the SQL from the XML schema.
     *
     * @return the context
     * @throws Exception
     */
    public Context initControlContext() throws Exception
    {
        XmlToAppData xmlParser;

        if (xmlFile == null && filesets.isEmpty())
        {
            throw new BuildException("You must specify an XML schema or " +
                "fileset of XML schemas!");
        }

        if (xmlFile != null)
        {
            // Transform the XML database schema into
            // data model object.
            xmlParser =
                new XmlToAppData(getTargetDatabase(), getTargetPackage(), 
                                 getBasePathToDbProps());
            AppData ad = xmlParser.parseFile(xmlFile);
            ad.setName(grokName(xmlFile));
            dataModels.add(ad);
        }
        else
        {
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
                    xmlParser = new XmlToAppData(getTargetDatabase(),
                                                 getTargetPackage(), 
                                                 getBasePathToDbProps());
                    AppData ad = xmlParser.parseFile(f.toString());
                    ad.setName(grokName(f.toString()));
                    dataModels.add(ad);
                }
            }
        }

        Iterator i = dataModels.iterator();
        databaseNames = new Hashtable();
        dataModelDbMap = new Hashtable();

        // Different datamodels may state the same database
        // names, we just want the unique names of databases.
        while (i.hasNext())
        {
            AppData ad = (AppData) i.next();
            Database database = ad.getDatabase();
            databaseNames.put(database.getName(), database.getName());
            dataModelDbMap.put(ad.getName(), database.getName());
        }

        context = new VelocityContext();

        // Place our set of data models into the context along
        // with the names of the databases as a convenience for now.
        context.put("dataModels", dataModels);
        context.put("databaseNames", databaseNames);
        context.put("targetDatabase", targetDatabase);
        context.put("targetPackage", targetPackage);

        return context;
    }

    /**
     * Gets a name to use for the application's data model.
     *
     * @param xmlFile The path to the XML file housing the data model.
     * @return The name to use for the <code>AppData</code>.
     */
    private String grokName(String xmlFile)
    {
        // This can't be set from the file name as it is an unreliable
        // method of naming the descriptor. Not everyone uses the same
        // method as I do in the TDK. jvz.

        String name = "data-model";
        int i = xmlFile.lastIndexOf(System.getProperty("file.separator"));
        if (i != -1)
        {
            // Creep forward to the start of the file name.
            i++;

            int j = xmlFile.lastIndexOf('.');
            if (i < j)
            {
                name = xmlFile.substring(i, j);
            }
            else
            {
                // Weirdo
                name = xmlFile.substring(i);
            }
        }
        return name;
    }
    
    /**
     * Override Texen's context properties to map the
     * torque.xxx properties (including defaults set by the
     * org/apache/torque/defaults.properties) to just xxx.
     *
     * <p>
     * Also, move xxx.yyy properties to xxxYyy as Velocity
     * doesn't like the xxx.yyy syntax.
     * </p>
     *
     * @param file the file to read the properties from
     */
    public void setContextProperties(String file)
    {
        super.setContextProperties(file);
        
        // Map the torque.xxx elements from the env to the contextProperties
        Hashtable env = super.getProject().getProperties();
        for (Iterator i = env.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            if (key.startsWith("torque."))
            {
                String newKey = key.substring("torque.".length());
                int j = newKey.indexOf(".");
                while (j != -1)
                {
                    newKey =
                        newKey.substring(0, j)
                        +  StringUtils.capitalise(newKey.substring(j + 1));
                    j = newKey.indexOf(".");
                }

                contextProperties.setProperty(newKey, env.get(key));
            }
        }
    }        
}
