package org.apache.torque;

public class TorqueRunner
{
    public static void main(String[] args)
        throws TorqueException
    {
        String configurationFile = args[0];
        
        try
        {
            Torque.init2(configurationFile);
        }
        catch (Exception e)
        {
            throw new TorqueException("Can't initialize Torque!", e);
        }
    }
}
