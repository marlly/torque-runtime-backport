package org.apache.torque.engine.database.model;

import junit.framework.TestCase;

/**
 * @author martin
 */
public class ColumnTest extends TestCase {

    public void testRequiresTransactionInPostgres() {
        Column col = new Column();
        col.setType("VARBINARY");
        assertTrue(col.requiresTransactionInPostgres());
        col = new Column();
        col.setType("INTEGER");
        assertFalse(col.requiresTransactionInPostgres());
        col = new Column();
        col.setType("BLOB");
        assertTrue(col.requiresTransactionInPostgres());
    }

}
