//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ArrayList;

/** Table of string elements. */
public class Table
{
    public static class InvalidLocation
        extends ErrorMessage
    {
        public InvalidLocation(String message)
        {
            super(message);
        }        

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    public static class InvalidFormat
        extends ErrorMessage
    {
        public InvalidFormat(String message)
        {
            super(message);
        }        

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    public static class InvalidElement
        extends ErrorMessage
    {
        public InvalidElement(String message)
        {
            super(message);
        }        

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    public Table()
    {
        m_columnTitles = new ArrayList();
        m_numberColumns = 0;
    }

    public Table(ArrayList columnTitles)
    {        
        m_columnTitles = columnTitles;
        m_numberColumns = columnTitles.size();
    }

    public String get(int column, int row)
    {
        return (String)getRow(row).get(column);
    }

    public String get(String columnTitle, int row) throws InvalidLocation
    {
        return get(getColumnIndex(columnTitle), row);
    }

    public double getDouble(int column, int row)
        throws InvalidLocation, InvalidElement
    {
        try
        {
            String s = get(column, row);
            if (s == null)
                s = "";
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidElement("Expected floating point number in "
                                     + "table (column=" + column + ", row="
                                     + row + ")");
        }
    }

    public double getDouble(String columnTitle, int row)
        throws InvalidLocation, InvalidElement
    {
        return getDouble(getColumnIndex(columnTitle), row);
    }

    public int getInt(int column, int row) throws InvalidElement
    {
        try
        {
            String s = get(column, row);
            if (s == null)
                s = "";
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidElement("Expected integer in table (column="
                                     + column + ", row=" + row + ")");
        }
    }

    public int getInt(String columnTitle, int row)
        throws InvalidLocation, InvalidElement
    {
        return getInt(getColumnIndex(columnTitle), row);
    }

    public int getColumnIndex(String column) throws InvalidLocation
    {
        for (int i = 0; i < m_numberColumns; ++i)
        {
            String title = getColumnTitle(i);
            if (title.equals(column))
                return i;
        }
        throw new InvalidLocation("No such column in table: " + column);
    }

    public String getColumnTitle(int index)
    {
        return (String)m_columnTitles.get(index);
    }

    public ArrayList getColumnTitles()
    {
        return (ArrayList)m_columnTitles.clone();
    }

    public int getNumberColumns()
    {
        return m_columnTitles.size();
    }

    public int getNumberRows()
    {
        return m_rows.size();
    }

    /** Get meta information.
        @param key the property key
        @param def the default value, if this property does not exist
    */
    public String getProperty(String key, String def)
    {
        return m_properties.getProperty(key, def);
    }

    public boolean hasProperty(String key)
    {
        return (m_properties.get(key) != null);
    }

    public void read(File file) throws FileNotFoundException, IOException,
                                       InvalidFormat
    {
        read(new FileReader(file));
    }

    public void read(Reader reader) throws IOException, InvalidFormat
    {
        BufferedReader bufferedReader = new BufferedReader(reader);
        m_lineNumber = 0;
        m_propertiesRead = false;
        String line = null;
        while ((line = bufferedReader.readLine()) != null)
        {
            ++m_lineNumber;
            handleLine(line);
        }
        bufferedReader.close();
    }

    public void save(File file) throws IOException
    {
        FileWriter writer = new FileWriter(file);
        try
        {
            save(writer);
        }
        finally
        {
            writer.close();
        }
    }

    public void save(Writer out) throws IOException
    {
        save(out, true);
    }

    public void save(Writer out, boolean withHeader) throws IOException
    {
        if (withHeader)
        {
            Enumeration propertyNames = m_properties.propertyNames();
            for (Enumeration e = propertyNames; e.hasMoreElements(); )
            {
                String key = (String)e.nextElement();
                out.write("# " + key + ": " + m_properties.get(key) + "\n");
            }
            out.write("#\n#");
            for (int i = 0; i < m_numberColumns; ++i)
            {
                out.write(getColumnTitle(i));
                if (i < m_numberColumns - 1)
                    out.write('\t');
                else
                    out.write('\n');
            }
        }
        for (int i = 0; i < m_rows.size(); ++i)
        {
            ArrayList row = (ArrayList)m_rows.get(i);
            for (int j = 0; j < m_numberColumns; ++j)
            {
                String value = (String)row.get(j);
                if (value != null)
                    out.write(value);
                if (j < m_numberColumns - 1)
                    out.write('\t');
                else
                    out.write('\n');
            }
        }
    }

    /** Set element in last row.
        @param column Column in last row.
        @param value The value (must not contain newlines or tabs).
    */
    public void set(int column, String value)
    {
        assert(m_lastRow.get(column) == null);
        // Values containing newlines and tabs are not supported by save()
        // yet
        assert(value.indexOf("\n") < 0);
        assert(value.indexOf("\t") < 0);
        m_lastRow.set(column, value);
    }

    public void set(String column, int value) throws InvalidLocation
    {
        set(column, Integer.toString(value));
    }

    public void set(String column, double value) throws InvalidLocation
    {
        set(column, Double.toString(value));
    }

    public void set(String column, String value) throws InvalidLocation
    {
        set(getColumnIndex(column), value);
    }

    /** Set meta information.
        @param key the property key
        @param value the property value
    */
    public Object setProperty(String key, String value)
    {
        return m_properties.setProperty(key, value);
    }

    public void startRow()
    {
        ArrayList row = new ArrayList(m_numberColumns);
        for (int i = 0; i < m_numberColumns; ++i)
            row.add(null);
        m_rows.add(row);
        m_lastRow = row;
    }
    
    private boolean m_propertiesRead;

    private int m_lineNumber;

    private int m_numberColumns;

    private final Properties m_properties = new Properties();

    private final ArrayList m_columnTitles;

    private ArrayList m_lastRow;

    private final ArrayList m_rows = new ArrayList();

    private void addColumnTitle(String columnTitle)
    {
        m_columnTitles.add(columnTitle);
        ++m_numberColumns;
    }

    private ArrayList getRow(int index)
    {
        return (ArrayList)m_rows.get(index);
    }

    private void handleComment(String comment)
    {
        comment = comment.trim();
        if (m_propertiesRead)
        {
            String[] array = comment.split("\\t");
            for (int i = 0; i < array.length; ++i)
                addColumnTitle(array[i]);
            return;
        }
        if (comment.equals(""))
        {
            m_propertiesRead = true;
            return;
        }
        int pos = comment.indexOf(':');
        if (pos < 0)
        {
            System.err.println("Invalid line " + m_lineNumber + ": "
                               + comment);
            return;
        }
        String key = comment.substring(0, pos).trim();
        String value = comment.substring(pos + 1).trim();
        setProperty(key, value);
    }

    private void handleLine(String line) throws InvalidFormat
    {
        line = line.trim();
        if (line.startsWith("#"))
        {
            handleComment(line.substring(1));
            return;
        }
        String[] array = line.split("\\t");
        if (array.length > getNumberColumns())
            throw new InvalidFormat("Invalid line " + m_lineNumber
                                    + ": " + line);
        startRow();
        for (int i = 0; i < array.length; ++i)
            set(i, array[i]);
    }
}

