package org.apache.torque.pool;

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