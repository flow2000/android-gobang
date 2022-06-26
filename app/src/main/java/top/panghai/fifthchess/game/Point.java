package top.panghai.fifthchess.game;

import java.io.Serializable;

public class Point implements Serializable {
    private static final long serialVersionUID = 4958644533922626552L;
    public int x;
    public int y;
    private boolean isLast;

    public Point() {
    }


    public int getX() {
        return x;
    }

    public Point setX(int x) {
        this.x = x;
        return this;
    }

    public int getY() {
        return y;
    }

    public Point setY(int y) {
        this.y = y;
        return this;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.isLast = false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (x != point.x) return false;
        return y == point.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "Point(" + x + "," + y + ")";
    }
}