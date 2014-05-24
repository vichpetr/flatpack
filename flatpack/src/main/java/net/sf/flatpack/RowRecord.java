package net.sf.flatpack;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;

import net.sf.flatpack.structure.ColumnMetaData;
import net.sf.flatpack.structure.Row;
import net.sf.flatpack.util.FPConstants;
import net.sf.flatpack.util.FPStringUtils;
import net.sf.flatpack.util.ParserUtils;
import net.sf.flatpack.xml.MetaData;

public class RowRecord implements Record {
    private Row row;
    private boolean columnCaseSensitive;
    private MetaData metaData;
    private Properties pzConvertProps = null;
    private boolean strictNumericParse;
    private boolean upperCase;
    private boolean lowerCase;
    private boolean nullEmptyString;

    public RowRecord(Row row, MetaData metaData, boolean columnCaseSensitive, Properties pzConvertProps, boolean strictNumericParse,
            boolean upperCase, boolean lowerCase, boolean nullEmptyString) {
        super();
        this.row = row;
        this.metaData = metaData;
        this.columnCaseSensitive = columnCaseSensitive;
        this.pzConvertProps = pzConvertProps;
        this.strictNumericParse = strictNumericParse;
        this.upperCase = upperCase;
        this.lowerCase = lowerCase;
        this.nullEmptyString = nullEmptyString;
    }

    @Override
    public boolean isRecordID(final String recordID) {
        String rowID = row.getMdkey();
        if (rowID == null) {
            rowID = FPConstants.DETAIL_ID;
        }

        return rowID.equals(recordID);
    }

    @Override
    public int getRowNo() {
        return row.getRowNumber();
    }

    @Override
    public boolean isRowEmpty() {
        return row.isEmpty();
    }

    @Override
    public boolean contains(final String column) {
        final Iterator<ColumnMetaData> cmds = ParserUtils.getColumnMetaData(row.getMdkey(), metaData).iterator();
        while (cmds.hasNext()) {
            final ColumnMetaData cmd = cmds.next();
            if (cmd.getColName().equalsIgnoreCase(column)) {
                return true;
            }
        }

        return false;

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.flatpack.DataSet#getColumns()
     */
    @Override
    public String[] getColumns() {
        ColumnMetaData column = null;
        String[] array = null;

        if (/* columnMD != null || */metaData != null) {
            final List cmds = metaData.getColumnsNames();// ParserUtils.getColumnMetaData(PZConstants.DETAIL_ID,
                                                         // columnMD);

            array = new String[cmds.size()];
            for (int i = 0; i < cmds.size(); i++) {
                column = (ColumnMetaData) cmds.get(i);
                array[i] = column.getColName();
            }
        }

        return array;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.flatpack.DataSet#getColumns(java.lang.String)
     */
    @Override
    public String[] getColumns(final String recordID) {
        String[] array = null;

        if (metaData != null) {
            final List cmds = ParserUtils.getColumnMetaData(recordID, metaData);
            array = new String[cmds.size()];
            for (int i = 0; i < cmds.size(); i++) {
                final ColumnMetaData column = (ColumnMetaData) cmds.get(i);
                array[i] = column.getColName();
            }
        }

        return array;
    }

    @Override
    public Date getDate(String column, SimpleDateFormat sdf, Supplier<Date> defaultSupplier) throws ParseException {
        Date d = getDate(column, sdf);
        if (d == null) {
            return defaultSupplier.get();
        }
        return d;
    }
    
    @Override
    public Date getDate(String column, Supplier<Date> defaultSupplier) throws ParseException {
        Date d = getDate(column);
        if (d == null) {
            return defaultSupplier.get();
        }
        return d;
    }

    @Override
    public Date getDate(final String column) throws ParseException {
        return getDate(column, new SimpleDateFormat("yyyyMMdd"));
    }

    @Override
    public Date getDate(final String column, final SimpleDateFormat sdf) throws ParseException {
        final String s = getStringValue(column);
        if (FPStringUtils.isBlank(s)) {
            // don't do the parse on empties
            return null;
        }
        return sdf.parse(s);
    }

    @Override
    public double getDouble(String column, Supplier<Double> defaultSupplier) {
        final String s = getStringValue(column);
        if (FPStringUtils.isBlank(s)) {
            return defaultSupplier.get();
        }
        return getDouble(column);
    }

    @Override
    public double getDouble(final String column) {
        final StringBuilder newString = new StringBuilder();
        final String s = getStringValue(column);

        if (!strictNumericParse) {
            newString.append(ParserUtils.stripNonDoubleChars(s));
        } else {
            newString.append(s);
        }

        return Double.parseDouble(newString.toString());
    }

    @Override
    public int getInt(String column, Supplier<Integer> defaultSupplier) {
        final String s = getStringValue(column);
        if (FPStringUtils.isBlank(s)) {
            return defaultSupplier.get();
        }
        return getInt(column);
    }

    @Override
    public int getInt(final String column) {
        final String s = getStringValue(column);

        if (!strictNumericParse) {
            return Integer.parseInt(ParserUtils.stripNonLongChars(s));
        }

        return Integer.parseInt(s);
    }

    @Override
    public long getLong(String column, Supplier<Long> defaultSupplier) {
        final String s = getStringValue(column);
        if (FPStringUtils.isBlank(s)) {
            return defaultSupplier.get();
        }
        return getLong(column);
    }

    @Override
    public long getLong(final String column) {
        final String s = getStringValue(column);

        if (!strictNumericParse) {
            return Long.parseLong(ParserUtils.stripNonLongChars(s));
        }

        return Long.parseLong(s);
    }

    private String getStringValue(final String column) {
        return row.getValue(ParserUtils.getColumnIndex(row.getMdkey(), metaData, column, columnCaseSensitive));
    }

    @Override
    public Object getObject(final String column, final Class<?> classToConvertTo) {
        final String s = getStringValue(column);
        return ParserUtils.runPzConverter(pzConvertProps, s, classToConvertTo);
    }

    @Override
    public BigDecimal getBigDecimal(String column, Supplier<BigDecimal> defaultSupplier) {
        BigDecimal bd = getBigDecimal(column);
        if (bd == null) {
            return defaultSupplier.get();
        }
        return bd;
    }

    @Override
    public BigDecimal getBigDecimal(final String column) {
        final String s = getStringValue(column);
        if (FPStringUtils.isBlank(s)) {
            // don't do the parse on empties
            return null;
        }

        return new BigDecimal(s);
    }

    @Override
    public String getString(String column, Supplier<String> defaultSupplier) {
        final String s = getString(column);
        if (FPStringUtils.isBlank(s)) {
            return defaultSupplier.get();
        }
        return s;
    }

    @Override
    public String getString(final String column) {
        String s = getStringValue(column);

        if (nullEmptyString && FPStringUtils.isBlank(s)) {
            s = null;
        } else if (upperCase) {
            // convert data to uppercase before returning
            // return row.getValue(ParserUtils.findColumn(column,
            // cmds)).toUpperCase(Locale.getDefault());
            s = s.toUpperCase(Locale.getDefault());
        } else if (lowerCase) {
            // convert data to lowercase before returning
            // return row.getValue(ParserUtils.findColumn(column,
            // cmds)).toLowerCase(Locale.getDefault());
            s = s.toLowerCase(Locale.getDefault());
        }

        // return value as how it is in the file
        return s;
    }

    @Override
    public String getRawData() {
        return row.getRawData();
    }

}