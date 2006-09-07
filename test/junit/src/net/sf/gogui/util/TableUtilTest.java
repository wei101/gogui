//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.util.ArrayList;

//----------------------------------------------------------------------------

public class TableUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TableUtilTest.class);
    }

    public void testAllEmpty()
    {
        ArrayList columnTitles = new ArrayList();
        columnTitles.add("column1");
        columnTitles.add("column2");        
        Table table = new Table(columnTitles);
        table.startRow();
        table.set("column2", "value");
        table.startRow();
        table.set("column1", " \t");
        assertTrue(TableUtil.allEmpty(table, "column1"));
        assertFalse(TableUtil.allEmpty(table, "column2"));
    }

    public void testAppendRow()
    {
        ArrayList columnTitles = new ArrayList();
        columnTitles.add("column1");
        columnTitles.add("column2");        
        Table from = new Table(columnTitles);
        from.startRow();
        from.set("column1", "11");
        from.set("column2", "12");
        from.startRow();
        from.set("column1", "21");
        from.set("column2", null);
        from.startRow();
        from.set("column1", "31");
        from.set("column2", "32");
        Table to = new Table(columnTitles);
        to.startRow();
        TableUtil.appendRow(to, from, 1);
        assertEquals(2, to.getNumberRows());
        assertEquals("21", to.get(0, 1));
        assertEquals(null, to.get(1, 1));

    }

    public void testFromHistogramSingleValue() throws Exception
    {
        Histogram histo = new Histogram(-1, 1, 1);
        histo.add(0);
        Table table = TableUtil.fromHistogram(histo, "");
        assertEquals(table.getNumberColumns(), 2);
        assertEquals(table.getNumberRows(), 1);
    }
}

//----------------------------------------------------------------------------