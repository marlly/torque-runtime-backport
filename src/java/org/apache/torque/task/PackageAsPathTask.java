package org.apache.torque.task;

import org.apache.tools.ant.Task;
import org.apache.velocity.util.StringUtils;

/**
 * Simple task to convert packages to paths.
 * 
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @version $Revision$
 */
public class PackageAsPathTask extends Task
{

    /** The package to convert. */
    protected String pckg;

    /** The value to store the conversion in. */
    protected String name;

    /**
     * Executes the package to patch converstion and stores it 
     * in the user property <code>value</code>.
     */
    public void execute()
    {
        super.getProject().setUserProperty(this.name, StringUtils.getPackageAsPath(this.pckg));
    }

    /**
     * @param pckg the package to convert
     */
    public void setPackage(String pckg)
    {
        this.pckg = pckg;
    }

    /**
     * @param name the Ant variable to store the path in
     */
    public void setName(String name)
    {
        this.name = name;
    }

}
