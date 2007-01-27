//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Dimension;
import java.awt.Point;
import net.sf.gogui.go.GoPoint;

/** Const functions of gui.GuiBoard.
    @see GuiBoard
*/
public interface ConstGuiBoard
{
    int getBoardSize();

    Dimension getFieldSize();

    String getLabel(GoPoint point);

    Point getLocationOnScreen(GoPoint point);

    boolean getMark(GoPoint point);

    boolean getMarkCircle(GoPoint point);

    boolean getMarkSquare(GoPoint point);

    boolean getMarkTriangle(GoPoint point);

    boolean getSelect(GoPoint point);

    boolean getShowCursor();

    boolean getShowGrid();
}