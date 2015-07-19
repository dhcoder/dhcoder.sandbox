package dhcoder.sandbox;

public abstract class CellAutomata {
    protected CellType[][] myCellTypes;

    public CellAutomata(int gridX, int gridY) {
        myCellTypes = new CellType[gridX][gridY];
    }

    public final CellType[][] getCellTypes() {
        return myCellTypes;
    }

    public final void run(int numIterations) {
        initialize();
        for (int i = 0; i < numIterations; i++) {
            iterate();
        }
        finish();
    }

    protected abstract void initialize();
    protected abstract void iterate();
    protected void finish() {}
}
