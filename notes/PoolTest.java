package org.apache.torque.pool;

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

import org.apache.log4j.Logger;

import org.apache.torque.BaseTestCase;
import org.apache.torque.Torque;

/* TODO: Turn this into a JUnit test case.
import junit.framework.Test;
import junit.framework.TestSuite; */

/**
 * 25 concurrent
 * 10-100 ms holding connections before release
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Id$
 */
public class PoolTest implements Runnable
{
    /**
     * The number of threads to use for concurrent connections.
     */
    private static final int NBR_THREADS = 90;

    /**
     * The number of milliseconds to hold onto a database connection
     * for.
     */
    private static final int CONN_HOLD_TIME = 500;

    /**
     * The path to the configuration file.
     */
    private static final String CONFIG_FILE =
        "./TurbineResources.properties";

    private static Thread executionThread = null;

    private static ThreadGroup threadGroup = new ThreadGroup("run");

    private static int currentThreadCount = 0;

    private static Logger logger = Logger.getLogger(PoolTest.class);

    protected PoolTest()
    {
        Thread thread = new Thread(threadGroup, this,
                                   "Thread+" + currentThreadCount++);
        thread.setDaemon(false);
        thread.start();
    }

    public static void main(String[] args)
    {
        try
        {
            Torque.init(CONFIG_FILE);

            for (int i = 0; i < NBR_THREADS; i++)
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
                    dbCon = Torque.getConnection();
                    System.out.println ("Open Connection2: " + thread);
                    try
                    {
                        System.out.println("Start Holding: " +
                                           System.currentTimeMillis() +
                                           " : " + thread);
                        thread.sleep(CONN_HOLD_TIME);
                        System.out.println("Finish Holding: " +
                                           System.currentTimeMillis() +
                                           " : " + thread);
                    }
                    catch (InterruptedException ie)
                    {
                        // Yawn
                    }
                }
                finally
                {
                    System.out.println("Releasing connection: " + thread);
                    Torque.releaseConnection(dbCon);
                    System.out.println("Released connection: " + thread);
                }
            }
            catch (Exception e)
            {
                logger.error(e);
                e.printStackTrace();
            }
        }
    }
}
