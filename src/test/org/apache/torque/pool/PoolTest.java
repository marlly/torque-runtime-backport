package org.apache.torque.pool;

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

import java.util.Hashtable;
import java.util.Properties;
import java.io.FileInputStream;

import org.apache.velocity.runtime.configuration.Configuration;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import org.apache.torque.Torque;

/**
 * 25 concurrent
 * 10-100 ms holding connections before release
 */
public class PoolTest implements Runnable
{
    private static int threadCount = 90;   // number
    private static int holdCount = 500;     // ms
//    private static final int sleepPeriod = 1 * 60000;
    private static final String dbName = "default";
    private static final String configFile =
        "./TurbineResources.properties";

    private static Thread executionThread = null;

    private static ThreadGroup threadGroup = new ThreadGroup("run");

    private static int currentThreadCount = 0;

    private Hashtable localHash = new Hashtable();

    private static Category category;

    protected PoolTest()
    {
        Thread thread = new Thread(threadGroup, this, "Thread+" + currentThreadCount++);
        thread.setDaemon(false);
        thread.start();
    }

    public static void main(String[] args)
    {
        try
        {
            Configuration config = new Configuration(configFile);
            config = config.subset ("services.DatabaseService");
            System.out.println ("CONFIG: " + config.getString("database.default"));

            Properties p = new Properties();
            p.load(new FileInputStream(configFile));
            PropertyConfigurator.configure(p);

            Torque.setConfiguration(config);
            category = Category.getInstance("ALL");
            Torque.setCategory(category);
            Torque.init();

            for (int i=0; i<threadCount; i++)
            {
                PoolTest pt = new PoolTest();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void run()
    {
        Thread thread = Thread.currentThread();
        while (true)
        {
            try
            {
                DBConnection dbCon = null;
                try
                {
                    System.out.println ("Open Connection1: " + thread);
                    dbCon = Torque.getConnection(dbName);
                    System.out.println ("Open Connection2: " + thread);
                    try
                    {
                        System.out.println("Start Holding: " + System.currentTimeMillis() + " : " + thread);
                        thread.sleep(holdCount);
                        System.out.println("Finish Holding: " + System.currentTimeMillis() + " : " + thread);
                    }
                    catch (InterruptedException ie)
                    {
                        // Yawn
                    }
                }
                finally
                {
                    System.out.println ("Release Connection1: " + thread);
                    Torque.releaseConnection(dbCon);
                    System.out.println ("Release Connection2: " + thread);
                }
            }
            catch (ConnectionWaitTimeoutException cwte)
            {
                System.out.println ("Caught CWTE: " + thread);
            }
            catch (Exception e)
            {
                category.error(e);
                e.printStackTrace();
            }
        }
    }
}