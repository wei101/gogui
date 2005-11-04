//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Move.
    Contains a point (null for pass move) and a color.
    The color is usually black or white, but empty can be used for
    removing a stone on the board.
    This class is immutable, references are unique.
*/
public final class Move
{
    public static Move create(int x, int y, GoColor color)
    {
        return create(GoPoint.create(x, y), color);
    }

    public static Move create(GoPoint point, GoColor color)
    {
        if (point == null)
        {
            if (color == GoColor.BLACK)
                return s_passBlack;
            assert(color == GoColor.WHITE);
            return s_passWhite;
        }
        int x = point.getX();
        int y = point.getY();
        int max = Math.max(x, y);
        if (max >= s_size)
            grow(max + 1);
        if (color == GoColor.BLACK)
            return s_movesBlack[x][y];
        else if (color == GoColor.WHITE)
            return s_movesWhite[x][y];
        else
            return s_movesEmpty[x][y];
    }
    
    public GoColor getColor()
    {
        return m_color;
    }

    public GoPoint getPoint()
    {
        return m_point;
    }

    public String toString()
    {
        return m_string;
    }

    private static int s_size;

    private static Move s_passBlack;

    private static Move s_passWhite;

    private static Move[][] s_movesBlack;

    private static Move[][] s_movesEmpty;

    private static Move[][] s_movesWhite;

    private final GoColor m_color;

    private final GoPoint m_point;

    private final String m_string;

    static
    {
        s_passBlack = new Move(null, GoColor.BLACK);
        s_passWhite = new Move(null, GoColor.WHITE);
        s_size = 0;
        grow(GoPoint.DEFAULT_SIZE);
    };

    private static void grow(int size)
    {
        assert(size > s_size);
        s_movesBlack = grow(size, GoColor.BLACK, s_movesBlack);
        s_movesWhite = grow(size, GoColor.WHITE, s_movesWhite);
        s_movesEmpty = grow(size, GoColor.EMPTY, s_movesEmpty);
        s_size = size;
    }

    private static Move[][] grow(int size, GoColor color, Move[][] moves)
    {
        assert(size > s_size);
        Move[][] result = new Move[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
                if (x < s_size && y < s_size)
                    result[x][y] = moves[x][y];
                else
                    result[x][y] = new Move(GoPoint.create(x, y), color);
        return result;
    }

    private Move(GoPoint point, GoColor color)
    {
        m_point = point;
        m_color = color;
        m_string = m_color.toString() + " " + GoPoint.toString(m_point);
    }
}

//----------------------------------------------------------------------------
