package org.apache.torque.engine.platform;

import junit.framework.TestCase;

import org.apache.torque.engine.database.model.Domain;
import org.apache.torque.engine.database.model.SchemaType;

/**
 * @author martin
 */
public class PlatformMysqlImplTest extends TestCase {

    Platform platform;
    
    public void setUp()
    {
        platform = PlatformFactory.getPlatformFor("mysql");
    }
    
    public void testGetMaxColumnNameLength() {
        assertEquals(64, platform.getMaxColumnNameLength());
    }

    public void testGetNativeIdMethod() {
        assertEquals("identity", platform.getNativeIdMethod());
    }

    public void testGetDomainForJdbcType() {
        Domain numeric = platform.getDomainForSchemaType(SchemaType.NUMERIC);
        assertEquals(SchemaType.NUMERIC, numeric.getType());
        assertEquals("DECIMAL", numeric.getSqlType());
    }

}
