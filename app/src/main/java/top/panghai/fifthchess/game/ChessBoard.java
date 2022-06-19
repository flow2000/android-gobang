package top.panghai.fifthchess.game;


import java.util.LinkedList;
import java.util.List;

public class ChessBoard implements IChessboard{

    private List<Point> pointList;

    private int n;

    public ChessBoard(int n){
        this.n = n;
        pointList = new LinkedList<>();
        clear();
    }

    public void setPointList(List<Point> pointList) {
        this.pointList = pointList;
    }

    @Override
    public int getMaxX() {
        return 15;
    }

    @Override
    public int getMaxY() {
        return 15;
    }

    @Override
    public List<Point> getFreePoints() {
        return pointList;
    }

    @Override
    public void clear() {
        pointList.clear();
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                pointList.add(new Point(i,j));
            }
        }
    }
}
