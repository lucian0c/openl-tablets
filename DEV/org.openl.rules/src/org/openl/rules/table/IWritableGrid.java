/**
 * Created Feb 15, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.Map;


/**
 * @author snshor
 * 
 */
public interface IWritableGrid extends IGrid
{
	void copyCell(int colFrom, int rowFrom, int colTo, int RowTo);

	void clearCell(int col, int row);

	void setCellStringValue(int col, int row, String value);

	void setCellValue(int col, int row, Object value);

	void setCellType(int col, int row, int type);


	void putCellMetaInfo(int col, int row, Object key, Object value);

	Object getCellMetaInfo(int col, int row, Object key);

	Map getCellMetaInfoMap(int col, int row);

	static public class Tool
	{
		public static IWritableGrid getWritableGrid(IGridTable table)
		{
			IGrid grid = table.getGrid();
			if (grid instanceof IWritableGrid)
				return (IWritableGrid) grid;
			return null;
		}

		public static void putCellMetaInfo(IGridTable table, int col, int row,
				Object key, Object value)
		{
			IWritableGrid wgrid = getWritableGrid(table);
			if (wgrid == null)
				return;
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			wgrid.putCellMetaInfo(gcol, grow, key, value);
		}

		public static Object getCellMetaInfo(IGridTable table, int col, int row,
				Object key)
		{
			IWritableGrid wgrid = getWritableGrid(table);
			if (wgrid == null)
				return null;
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			return wgrid.getCellMetaInfo(gcol, grow, key);
		}

		public static Object getCellMetaInfoMap(IGridTable table, int col, int row)
		{
			IWritableGrid wgrid = getWritableGrid(table);
			if (wgrid == null)
				return null;
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);

			return wgrid.getCellMetaInfoMap(gcol, grow);
		}

		public static IUndoableGridAction removeColumns(int nCols, int startColumn, IGridRegion region)
		{
			int firstToMove = startColumn + nCols;
			int w = IGridRegion.Tool.width(region);
			int h = IGridRegion.Tool.height(region);
			int left = region.getLeft();
			int top = region.getTop();
			
			ArrayList actions = new ArrayList(h * (w-startColumn));
			for (int i = firstToMove; i < w; i++)
			{
					for (int j = 0; j < h; j++)
					{
						actions.add(new UndoableCopyValueAction(left+i,top+j, left+i -nCols, top + j));
					}
			}
			for (int i = 0; i < nCols; i++)
			{
					for (int j = 0; j < h; j++)
					{
						actions.add(new UndoableClearAction(left+w-1-i,top+j));
					}
			}
			return new UndoableCompositeAction(actions);
		}
		
		
		public static IUndoableGridAction removeRows(int nRows, int startRow, IGridRegion region)
		{
			int firstToMove = startRow + nRows;
			int w = IGridRegion.Tool.width(region);
			int h = IGridRegion.Tool.height(region);
			int left = region.getLeft();
			int top = region.getTop();
			
			ArrayList actions = new ArrayList(w * (h-startRow));
			for (int i = firstToMove; i < h; i++)
			{
					for (int j = 0; j < w; j++)
					{
						actions.add(new UndoableCopyValueAction(left+j,top+i, left+j, top + i - nRows));
					}
			}
			for (int i = 0; i < nRows; i++)
			{
					for (int j = 0; j < w; j++)
					{
						actions.add(new UndoableClearAction(left+j,top+h-1-i));
					}
			}
			return new UndoableCompositeAction(actions);
		}

		public static IUndoableGridAction insertRows(int nRows, int beforeRow, IGridRegion region,
				IWritableGrid wgrid)
		{
			int h = IGridRegion.Tool.height(region);
			int w = IGridRegion.Tool.width(region);
			int rowsToMove = h - beforeRow;
			int left = region.getLeft();

			ArrayList actions = new ArrayList(w * rowsToMove);
			
			// move row
			for (int i = 0; i < rowsToMove; i++)
			{
				int row = region.getBottom() - i;
				for (int j = 0; j < w; j++)
				{
//					wgrid.copyCell(left+j, row, left+j, row + nRows);
					actions.add(new UndoableCopyValueAction(left+j, row, left+j, row + nRows));
				}
			}
			return new UndoableCompositeAction(actions);
		}
		

		public static IUndoableGridAction setStringValue(int col, int row, IGridTable table, String value)
		{
//			IWritableGrid wgrid = getWritableGrid(table);
			int gcol = table.getGridColumn(col, row);
			int grow = table.getGridRow(col, row);
			
//		wgrid.setCellStringValue(gcol, grow, value);
			return new UndoableSetValueAction(gcol, grow , value); 
			
		}
	

		public static IUndoableGridAction setStringValue(int col, int row, IGridRegion region, String value)
		{
			int gcol = region.getLeft()  + col;
			int grow = region.getTop() + row;
			
//		wgrid.setCellStringValue(gcol, grow, value);
			return new UndoableSetValueAction(gcol, grow , value); 
		}
	
		
		
		public static IUndoableGridAction insertColumns(int nColumns, int beforeColumns, IGridRegion region,
				IWritableGrid wgrid)
		{
			int h = IGridRegion.Tool.height(region);
			int w = IGridRegion.Tool.width(region);
			int columnsToMove = w - beforeColumns;
			int top = region.getTop();
			
			ArrayList actions = new ArrayList(h * columnsToMove);
			
			// move columns
			for (int i = 0; i < columnsToMove; i++)
			{
				int col = region.getRight() - i;
				for (int j = 0; j < h; j++)
				{
//					wgrid.copyCell(col, top+j, col + nColumns, top+j);
					actions.add(new UndoableCopyValueAction(col, top+j, col + nColumns, top+j));
				}
			}
			
			return new UndoableCompositeAction(actions);
		}
	}

	/**
	 * @param to
	 */
	void removeMergedRegion(IGridRegion to);

	/**
	 * @param copyFrom
	 */
	void addMergedRegion(IGridRegion reg);

}
