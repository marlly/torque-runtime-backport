package org.apache.torque.engine.database.transform;

/*
 * Copyright 2001,2004 The Apache Software Foundation.
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

import java.io.*;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.torque.BaseTestCase;
import org.apache.torque.engine.database.model.Database;
import org.apache.torque.engine.sql.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:andyhot@di.uoa.gr">Andreas Andreou</a>
 * @version $Id$
 */
public class SQLToAppDataTest extends BaseTestCase
{
    /** The path to the configuration file. */
    private static final String SQL_FOLDER = "target/test/rttest/sql";

    private Vector files;

    /**
     * Creates a new instance.
     */
    public SQLToAppDataTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        // this may not be needed
        //super.setUp();
        // init the vector
        files = new Vector();
        // find all sql files to test
        File sqlFolder = new File(SQL_FOLDER);
        if (sqlFolder != null && sqlFolder.isDirectory())
        {
            File allFiles[] = sqlFolder.listFiles();
            for (int i = 0; i < allFiles.length; i++)
            {
                File thisFile = allFiles[i];
                if (!thisFile.isDirectory() &&
                        thisFile.getName().toUpperCase().endsWith("SQL"))
                {
                    System.out.println("Adding file:" + thisFile.getName());
                    files.add(thisFile);
                }
            }
        }
        if (files.size() == 0)
        {
            System.out.println("No files where found to test the sql2xml task");
        }
    }

    public void testConvertToXml()
    {
        try
        {
            for (int i = 0; i < files.size(); i++)
            {
                File file = (File) files.elementAt(i);
                String filename = file.getAbsolutePath();
                // load the sql file
                SQLToAppData s2a = new SQLToAppData(filename);
                Database ad = s2a.execute();
                // write the output to a new xml file
                String xmlFilename = filename + ".xml";
                PrintWriter out = new PrintWriter(
                        new FileOutputStream(xmlFilename, false),true);
                out.println(ad);
                out.close();
                // compare result
                compareXmlFiles(filename + ".ref.xml", xmlFilename);
            }
        }
        catch (IOException expIo)
        {
            expIo.printStackTrace(System.out);
        }
        catch (ParseException expParse)
        {
            expParse.printStackTrace(System.out);
        }
    }

    private void renameDTD(String sFile)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(sFile));
            String line;
            StringBuffer sb = new StringBuffer(5000);
            while ((line=reader.readLine())!=null)
            {
                sb.append(line).append("\n");
            }
            reader.close();
            String data=sb.toString();
            if (data == null || data.length() == 0)
                return;
            int index=data.indexOf("<!DOCTYPE");
            if (index != -1)
            {
                int index2 = data.indexOf(">", index);
                if (index2 != -1)
                {
                    data = data.substring(0, index - 1)
                            + data.substring(index2 + 1);
                }
            }
            //data.replaceFirst("/database.dtd","/database.xxx");

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(sFile, false));
            writer.write(data);
            writer.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void compareXmlFiles(String refFile, String newFile)
    {
        System.out.println("Comparing " + newFile + " against " + refFile);
        //System.out.println("Rename DTD to disable checking for default values...");
        // The dom parser uses the DTD to add default values to the xml nodes.
        // This makes difficult comparing the xml files.
        // Since I couldn't find any way to disable this behavior,
        // I chose to delete the DTD declaration from the xml files.
        renameDTD(refFile);
        renameDTD(newFile);

        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setExpandEntityReferences(false);
            docFactory.setValidating(false);

            DocumentBuilder doc = docFactory.newDocumentBuilder();

            Document refDoc = doc.parse(new File(refFile));
            Document newDoc = doc.parse(new File(newFile));

            NodeList refList = refDoc.getElementsByTagName("database");
            NodeList newList = newDoc.getElementsByTagName("database");

            assertNotNull(refList);
            assertNotNull(newList);

            for (int i = 0; i < refList.getLength(); i++)
            {
                Node refNode = refList.item(i);
                Node newNode = newList.item(i);

                checkNodes(refNode, newNode);

                refNode = refNode.getFirstChild();
                newNode = newNode.getFirstChild();

            }
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace(System.out);
        }
        catch (FactoryConfigurationError factoryConfigurationError)
        {
            factoryConfigurationError.printStackTrace(System.out);
        }
        catch (SAXException e)
        {
            e.printStackTrace(System.out);
        }
        catch (IOException e)
        {
            e.printStackTrace(System.out);
        }
    }

    private void checkNodes(Node refNodeStart, Node newNodeStart)
    {
        Node refNode = refNodeStart;
        Node newNode = newNodeStart;
        while (refNode != null)
        {
            assertNotNull(newNode);
            if (refNode.getNodeType() != Node.TEXT_NODE)
            {
                // check matching names
                System.out.println(refNode.getNodeName() + " : "
                        + newNode.getNodeName());
                assertEquals(refNode.getNodeName(), newNode.getNodeName());
                // check matching attributes
                NamedNodeMap refNnm = refNode.getAttributes();
                NamedNodeMap newNnm = newNode.getAttributes();
                for (int j = 0; j < refNnm.getLength(); j++)
                {
                    Node refItem = refNnm.item(j);
                    String refName = refItem.getNodeName();

                    Node newItem = newNnm.getNamedItem(refName);
                    // check existance
                    assertNotNull(newItem);

                    // check matching value
                    System.out.println("    " + refName + " : "
                            + refItem.getNodeValue()+" -> "
                            + newItem.getNodeValue());
                    assertEquals(refItem.getNodeValue(), newItem.getNodeValue());
                }
            }

            Node refChild = refNode.getFirstChild();
            Node newChild = newNode.getFirstChild();
            if (refChild != null)
            {
                assertNotNull(newChild);
                checkNodes(refChild, newChild);
            }

            // check matching siblings
            refNode = refNode.getNextSibling();
            newNode = newNode.getNextSibling();
        }
    }

    // just for internal test
    public static void main(String args[])
    {
        SQLToAppDataTest test = new SQLToAppDataTest("inner test");
        //test.compareXmlFiles("c:/schema.sql.xml", "c:/schema.sql.xml.new");
        test.compareXmlFiles(
                "C:/java/projects/jakarta-turbine-torque/jakarta-turbine-torque/target/test/rttest/sql/schema.sql.ref.xml",
                "C:/java/projects/jakarta-turbine-torque/jakarta-turbine-torque/target/test/rttest/sql/schema.sql.xml");
    }
}
