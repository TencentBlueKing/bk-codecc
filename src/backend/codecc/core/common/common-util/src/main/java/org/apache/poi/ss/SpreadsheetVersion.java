package org.apache.poi.ss;

import org.apache.poi.ss.util.CellReference;

/**
 * easy excel的配置
 * 复制源码，改动一点点
 */
public enum SpreadsheetVersion {
    /**
     * Excel97 format aka BIFF8
     * <ul>
     * <li>The total number of available rows is 64k (2^16)</li>
     * <li>The total number of available columns is 256 (2^8)</li>
     * <li>The maximum number of arguments to a function is 30</li>
     * <li>Number of conditional format conditions on a cell is 3</li>
     * <li>Number of cell styles is 4000</li>
     * <li>Length of text cell contents is 32767</li>
     * </ul>
     */
    EXCEL97(0x10000, 0x0100, 30, 3, 4000, 32767),

    /**
     * Excel2007
     *
     * <ul>
     * <li>The total number of available rows is 1M (2^20)</li>
     * <li>The total number of available columns is 16K (2^14)</li>
     * <li>The maximum number of arguments to a function is 255</li>
     * <li>Number of conditional format conditions on a cell is unlimited
     * (actually limited by available memory in Excel)</li>
     * <li>Number of cell styles is 64000</li>
     * <li>Length of text cell contents is 32767</li>
     * </ul>
     */
    EXCEL2007(0x100000, 0x4000, 255, Integer.MAX_VALUE, 64000, Integer.MAX_VALUE);

    private final int maxRows;
    private final int maxColumns;
    private final int maxFunctionArgs;
    private final int maxCondFormats;
    private final int maxCellStyles;
    private final int maxTextLength;

    private SpreadsheetVersion(int maxRows, int maxColumns, int maxFunctionArgs,
                               int maxCondFormats, int maxCellStyles, int maxText) {
        this.maxRows = maxRows;
        this.maxColumns = maxColumns;
        this.maxFunctionArgs = maxFunctionArgs;
        this.maxCondFormats = maxCondFormats;
        this.maxCellStyles = maxCellStyles;
        this.maxTextLength = maxText;
    }

    /**
     * the maximum number of usable rows in each spreadsheet
     * @return the maximum number of usable rows in each spreadsheet
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * the last (maximum) valid row index, equals to <code> getMaxRows() - 1 </code>
     * @return the last (maximum) valid row index, equals to <code> getMaxRows() - 1 </code>
     */
    public int getLastRowIndex() {
        return maxRows - 1;
    }

    /**
     * the maximum number of usable columns in each spreadsheet
     * @return the maximum number of usable columns in each spreadsheet
     */
    public int getMaxColumns() {
        return maxColumns;
    }

    /**
     * the last (maximum) valid column index, equals to <code> getMaxColumns() - 1 </code>
     * @return the last (maximum) valid column index, equals to <code> getMaxColumns() - 1 </code>
     */
    public int getLastColumnIndex() {
        return maxColumns - 1;
    }

    /**
     * the maximum number arguments that can be passed to a multi-arg function (e.g. COUNTIF)
     * @return the maximum number arguments that can be passed to a multi-arg function (e.g. COUNTIF)
     */
    public int getMaxFunctionArgs() {
        return maxFunctionArgs;
    }

    /**
     * the maximum number of conditional format conditions on a cell
     * @return the maximum number of conditional format conditions on a cell
     */
    public int getMaxConditionalFormats() {
        return maxCondFormats;
    }

    /**
     * the maximum number of cell styles per spreadsheet
     * @return the maximum number of cell styles per spreadsheet
     */
    public int getMaxCellStyles() {
        return maxCellStyles;
    }

    /**
     * the last valid column index in a ALPHA-26 representation
     * @return the last valid column index in a ALPHA-26 representation
     *      (<code>IV</code> or <code>XFD</code>).
     */
    public String getLastColumnName() {
        return CellReference.convertNumToColString(getLastColumnIndex());
    }

    /**
     * the maximum length of a text cell
     * @return the maximum length of a text cell
     */
    public int getMaxTextLength() {
        return maxTextLength;
    }
}
