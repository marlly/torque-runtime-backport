package org.apache.torque.engine.database.transform;

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.List;

import org.apache.torque.Torque;
import org.apache.torque.engine.database.model.AppData;
import org.apache.torque.engine.database.model.Column;
import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.database.model.ForeignKey;
import org.apache.torque.engine.database.model.IdMethodParameter;
import org.apache.torque.engine.database.model.Index;
import org.apache.torque.engine.database.model.Inheritance;
import org.apache.torque.engine.database.model.Table;
import org.apache.torque.engine.database.model.Unique;

import org.apache.xerces.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A Class that is used to parse an input
 * xml schema file and creates and AppData java structure.
 * It uses apache Xerces to do the xml parsing.
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @version $Id$
 */
public class XmlToAppData extends DefaultHandler
{
    private static final boolean DEBUG = false;

    private AppData app;
    private Database currDB;
    private Table currTable;
    private Column currColumn;
    private ForeignKey currFK;
    private Index currIndex;
    private Unique currUnique;

    private boolean firstPass;
    private Table foreignTable;
    private String errorMessage;

    /**
     * Creates a new instance.
     */
    public XmlToAppData()
    {
        this(null, null);
    }

    /**
     * Creates a new instance for the specified database type.
     *
     * @param databaseType The type of database for the application.
     * @param basePropsFilePath The base of the path to the properties
     * file, including trailing slash.
     */
    public XmlToAppData(String databaseType, String basePropsFilePath)
    {
        app = (databaseType == null ? new AppData() :
               new AppData(databaseType, basePropsFilePath));
        firstPass = true;
        errorMessage = "";
    }

    /**
     * Parses a XML input file and returns a newly created and
     * populated AppData structure.
     *
     * @param xmlFile The input file to parse.
     * @return AppData populated by <code>xmlFile</code>.
     */
    public AppData parseFile(String xmlFile)
    {
        try
        {
            SAXParser parser = createParser();
            FileReader fr = null;
            try
            {
                fr = new FileReader (xmlFile);
            }
            catch (FileNotFoundException fnfe)
            {
                throw new FileNotFoundException
                    (new File(xmlFile).getAbsolutePath());
            }
            BufferedReader br = new BufferedReader (fr);
            try
            {
                InputSource is = new InputSource (br);
                parser.parse(is);
            }
            finally
            {
                br.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        firstPass = false;
        if ( errorMessage.length() > 0 )
        {
            System.out.println("Error in XML schema: " + errorMessage);
        }

        return app;
    }

    /**
     * Sets up the XML parser used by this SAX callback.
     *
     * @return A validating XML parser.
     */
    protected SAXParser createParser()
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        SAXParser parser = new SAXParser();

        // We don't use an external handlers, instead implementing
        // handler interfaces ourself.
        parser.setContentHandler(this);
        parser.setErrorHandler(this);

        // Set the Resolver for the Torque's DTD.
        DTDResolver dtdResolver = new DTDResolver();
        parser.setEntityResolver(dtdResolver);

        // Validate the input file
        parser.setFeature
            ("http://apache.org/xml/features/validation/dynamic", true);
        parser.setFeature("http://xml.org/sax/features/validation", true);

        return parser;
    }

    /**
     * Handles opening elements of the xml file.
     */
    public void startElement(String uri, String localName, String rawName,
                             Attributes attributes)
    {
        try
        {
            if (!firstPass)
            {
                if (rawName.equals("database"))
                {
                    String s = attributes.getValue("name");
                    if (s == null)
                    {
                        s = Torque.getDefaultDB();
                    }
                    currDB = app.getDatabase(s);
                }
                if (rawName.equals("table"))
                {
                    currTable = currDB.getTable(attributes.getValue("name"));

                    // check schema integrity
                    // if idMethod="autoincrement", make sure a column is
                    // specified as autoIncrement="true"
                    // FIXME: Handle idMethod="native" via DB adapter.
                    if ( currTable.getIdMethod().equals("autoincrement") )
                    {
                        Column[] columns = currTable.getColumns();
                        boolean foundOne = false;
                        for (int i = 0; i < columns.length && !foundOne; i++)
                        {
                            foundOne = columns[i].isAutoIncrement();
                        }

                        if ( !foundOne )
                        {
                            errorMessage += "Table '" + currTable.getName() +
                            "' is marked as autoincrement, but it does not " +
                            "have a column which declared as the one to " +
                            "auto increment (i.e. autoIncrement=\"true\")\n";
                        }
                    }
                }
                else if (rawName.equals("foreign-key"))
                {
                    String foreignTableName =
                        attributes.getValue("foreignTable");
                    foreignTable = currDB
                        .getTable(foreignTableName);
                    if ( foreignTable == null )
                    {
                        System.out.println("ERROR!! Attempt to set foreign"
                            + " key to nonexistent table, " +
                            attributes.getValue("foreignTable") + "!");
                    }
                }
                else if (rawName.equals("reference"))
                {
                    ForeignKey fk = currTable
                        .getForeignKey(attributes.getValue("local"));
                    List referrers = foreignTable.getReferrers();
                    if (referrers == null || !referrers.contains(fk))
                    {
                        foreignTable.addReferrer(fk);
                    }

                    Column local = currTable
                        .getColumn(attributes.getValue("local"));
                    // give notice of a schema inconsistency.
                    // note we do not prevent the npe as there is nothing
                    // that we can do, if it is to occur.
                    if ( local == null )
                    {
                        System.out.println("ERROR!! Attempt to define foreign"
                            + " key with nonexistent column, " +
                            attributes.getValue("local") + ", in table, " +
                            currTable.getName() + "!" );
                    }
                    //check for foreign pk's
                    if (local.isPrimaryKey())
                    {
                        currTable.setContainsForeignPK(true);
                    }

                    Column foreign = foreignTable
                        .getColumn(attributes.getValue("foreign"));
                    // if the foreign column does not exist, we may have an
                    // external reference or a misspelling
                    if ( foreign == null )
                    {
                        System.out.println("ERROR!! Attempt to set foreign"
                            + " key to nonexistent column, " +
                            attributes.getValue("foreign") + ", in table, "
                            + foreignTable.getName() + "!" );
                    }
                    foreign.addReferrer(fk);
                }
            }
            else
            {
                if (rawName.equals("database"))
                {
                    currDB = app.addDatabase(attributes);
                }
                else if (rawName.equals("table"))
                {
                    currTable = currDB.addTable(attributes);
                }
                else if (rawName.equals("column"))
                {
                    currColumn = currTable.addColumn(attributes);
                }
                else if (rawName.equals("inheritance"))
                {
                    currColumn.addInheritance(attributes);
                }
                else if (rawName.equals("foreign-key"))
                {
                    currFK = currTable.addForeignKey(attributes);
                }
                else if (rawName.equals("reference"))
                {
                    currFK.addReference(attributes);
                }
                else if (rawName.equals("index"))
                {
                    currIndex = currTable.addIndex(attributes);
                }
                else if (rawName.equals("index-column"))
                {
                    currIndex.addColumn(attributes);
                }
                else if (rawName.equals("unique"))
                {
                    currUnique = currTable.addUnique(attributes);
                }
                else if (rawName.equals("unique-column"))
                {
                    currUnique.addColumn(attributes);
                }
                else if (rawName.equals("id-method-parameter"))
                {
                    currTable.addIdMethodParameter(attributes);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Handles closing elements of the xml file.
     */
    public void endElement(String uri, String localName, String rawName)
    {
        if (DEBUG)
        {
            System.out.println("endElement(" + uri + ", " + localName + ", " +
                               rawName + ") called");
        }

        try
        {
            if (firstPass)
            {
                if ("table".equals(rawName) && currTable != null)
                {
                    currTable.loadedFromXML();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Warning callback.
     *
     * @exception spe The parse exception that caused the callback to be
     *                invoked.
     */
    public void warning(SAXParseException spe)
    {
        printParseError("Warning", spe);
    }

    /**
     * Error callback.
     *
     * @exception spe The parse exception that caused the callback to be
     *                invoked.
     */
    public void error(SAXParseException spe)
    {
        printParseError("Error", spe);
    }

    /**
     * Fatal error callback.
     *
     * @exception spe The parse exception that caused the callback to be
     *                invoked.
     */
    public void fatalError(SAXParseException spe)
    {
        printParseError("Fatal Error", spe);
    }

    private final void printParseError(String type, SAXParseException spe)
    {
        System.err.println(type + " [line " + spe.getLineNumber() +
                           ", row " + spe.getColumnNumber() + "]: " +
                           spe.getMessage());
    }
}
