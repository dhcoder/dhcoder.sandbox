package dhcoder.sandbox;

import com.badlogic.gdx.math.MathUtils;

public final class CaveAutomata extends CellAutomata {
    private final int myStartY;
    private int[][] myNeighborCounts;
    private int myCaveEntry;

    public CaveAutomata(int gridW, int gridH, int startY) {
        super(gridW, gridH);
        myStartY = startY;
        myNeighborCounts = new int[gridW][gridH];
    }

    @Override
    protected void initialize() {
        int gridW = myCellTypes.length;
        int gridH = myCellTypes[0].length;
        myCaveEntry = MathUtils.random(4, gridW - 4);
        
        for (int x = 0; x < myCellTypes.length; x++) {
            for (int y = 0; y < myCellTypes[x].length; y++) {
                if (y >= gridH - myStartY) {
                    myCellTypes[x][y] = CellType.Open;
                }
                else if (x == 0 || x == gridW - 1 || y == 0) {
                    myCellTypes[x][y] = CellType.Wall;
                }
                else if (y == gridH - myStartY - 1) {
                    myCellTypes[x][y] = CellType.Wall;
                    if (x == myCaveEntry) {
                        myCellTypes[x][y] = CellType.Open;
                    }
                }
                else {
                    myCellTypes[x][y] = MathUtils.randomBoolean(0.45f) ? CellType.Wall : CellType.Open;
                }
            }
        }
    }


    @Override
    protected void iterate() {
        int gridW = myCellTypes.length;
        int gridH = myCellTypes[0].length;

        for (int x = 0; x < gridW; x++) {
            for (int y = 0; y < gridH; y++) {
                myNeighborCounts[x][y] = 0;
            }
        }

        for (int x = 0; x < gridW; x++) {
            for (int y = 0; y < gridH; y++) {
                for (int i = -1; i <= 1; ++i) {
                    for (int j = -1; j <= 1; ++j) {
                        int adjX = x + i;
                        int adjY = y + j;
                        if (adjX < 0 || adjX >= gridW || adjY < 0 || adjY >= gridH) {
                            continue;
                        }

                        if (myCellTypes[adjX][adjY] == CellType.Wall) {
                            myNeighborCounts[x][y]++;
                        }
                    }
                }
            }
        }

        for (int x = 1; x < gridW - 1; ++x) {
            for (int y = 1; y < gridH - myStartY - 1; ++y) {
                myCellTypes[x][y] = myNeighborCounts[x][y] >= 5 ? CellType.Wall : CellType.Open;
            }
        }
    }

    @Override
    protected void finish() {
        int gridH = myCellTypes[0].length;

        // Make sure the cave entrance drops down into open space
        {
            int y = gridH - myStartY - 2;
            while (myCellTypes[myCaveEntry][y] == CellType.Wall) {
                myCellTypes[myCaveEntry][y] = CellType.Open;
                y--;
            }
        }
    }
}
