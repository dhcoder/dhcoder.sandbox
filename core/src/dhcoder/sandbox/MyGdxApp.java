package dhcoder.sandbox;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import megamu.mesh.MPolygon;
import megamu.mesh.Voronoi;

public class MyGdxApp extends ApplicationAdapter {

    private static final int AUTOMATA_ITERATIONS = 5;
    private static final int CELL_SIZE = 10;
    public static final int START_WALL_Y = 6;
    CellType[][] myCells;
    int[][] myNeighborCounts;
    Vector2[][] myPoints;
    MPolygon[][] myRegions;
    Star[] myStars;
    private boolean myDrawCellOutlines;
    private Camera myCamera;
    private ShapeRenderer myShapeRenderer;

    @Override
    public void create() {
        myCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShapeRenderer = new ShapeRenderer();
        myShapeRenderer.setProjectionMatrix(myCamera.combined);
        Gdx.input.setInputProcessor(new MyInputHandler());

        myStars = new Star[1000];
        float hW = Gdx.graphics.getWidth() / 2f;
        float hH = Gdx.graphics.getHeight() / 2f;
        for (int i = 0; i < myStars.length; i++) {
            myStars[i] = new Star(
                MathUtils.random(-hW, hW),
                MathUtils.random(-hH, hH),
                MathUtils.randomBoolean(0.9f) ? Color.WHITE : Color.RED);
        }

        int size = CELL_SIZE;
        int numPointsX = Gdx.graphics.getWidth() / size;
        int numPointsY = Gdx.graphics.getHeight() / size;

        myPoints = new Vector2[numPointsX][numPointsY];
        myCells = new CellType[numPointsX][numPointsY];
        myNeighborCounts = new int[numPointsX][numPointsY];
        myRegions = new MPolygon[numPointsX][numPointsY];
        int halfSize = size / 2;
        int noise = halfSize / 2;
        int left = -Gdx.graphics.getWidth() / 2;
        int top = -Gdx.graphics.getHeight() / 2;
        for (int x = 0; x < numPointsX; ++x) {
            for (int y = 0; y < numPointsY; ++y) {
                float finalX = left + halfSize + x * size;
                float finalY = top + halfSize + y * size;
                finalX += MathUtils.random(-noise, noise);
                finalY += MathUtils.random(-noise, noise);
                myPoints[x][y] = new Vector2(finalX, finalY);
            }
        }

        // Initialize cave via cell automata
        // http://www.roguebasin.com/index.php?title=Cellular_Automata_Method_for_Generating_Random_Cave-Like_Levels
        int caveEntry = MathUtils.random(4, numPointsX - 4);
        for (int x = 0; x < numPointsX; ++x) {
            for (int y = 0; y < numPointsY; ++y) {
                if (y > numPointsY - START_WALL_Y) {
                    myCells[x][y] = CellType.OuterSpace;
                }
                else if (y == numPointsY - START_WALL_Y) {
                    myCells[x][y] = CellType.Wall;
                    if (x == caveEntry) {
                        myCells[x][y] = CellType.Open;
                    }
                }
                else if (y == 0 || x == 0 || x == numPointsX - 1) {
                    myCells[x][y] = CellType.Wall;
                }
                else {
                    myCells[x][y] = MathUtils.randomBoolean(0.45f) ? CellType.Wall : CellType.Open;
                }
            }
        }

        int repeat = AUTOMATA_ITERATIONS;
        while (repeat > 0) {
            for (int x = 0; x < numPointsX; x++) {
                for (int y = 0; y < numPointsY; y++) {
                    myNeighborCounts[x][y] = 0;
                }
            }

            for (int x = 0; x < numPointsX; x++) {
                for (int y = 0; y < numPointsY; y++) {
                    for (int i = -1; i <= 1; ++i) {
                        for (int j = -1; j <= 1; ++j) {
                            int adjX = x + i;
                            int adjY = y + j;
                            if (adjX < 0 || adjX >= numPointsX || adjY < 0 || adjY >= numPointsY) {
                                continue;
                            }

                            if (myCells[adjX][adjY] == CellType.Wall) {
                                myNeighborCounts[x][y]++;
                            }
                        }
                    }
                }
            }

            for (int x = 1; x < numPointsX - 1; ++x) {
                for (int y = 1; y < numPointsY - START_WALL_Y; ++y) {
                    myCells[x][y] = myNeighborCounts[x][y] >= 5 ? CellType.Wall : CellType.Open;
                }
            }


            repeat--;
        }
        // Make sure the cave entrance drops down into open space
        {
            int y = numPointsY - START_WALL_Y - 1;
            while (myCells[caveEntry][y] == CellType.Wall) {
                myCells[caveEntry][y] = CellType.Open;
                y--;
            }
        }

        // Create voronoi regions corresponding to grid cells
        int numPoints = numPointsX * numPointsY;
        float[][] voronoiPoints = new float[numPoints][2];
        for (int i = 0; i < numPoints; ++i) {
            Vector2 fromVec = myPoints[i % numPointsX][i / numPointsX];
            voronoiPoints[i][0] = fromVec.x;
            voronoiPoints[i][1] = fromVec.y;
        }
        Voronoi voronoi = new Voronoi(voronoiPoints);
        int nX = 0;
        int nY = 0;
        for (MPolygon region : voronoi.getRegions()) {
            myRegions[nX][nY] = region;
            nX++;
            if (nX == numPointsX) {
                nX = 0;
                nY++;
            }
        }
    }

    @Override
    public void render() {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        myShapeRenderer.begin(ShapeType.Point);
        for (int i = 0; i < myStars.length; i++) {
            myShapeRenderer.setColor(myStars[i].myColor);
            myShapeRenderer.point(myStars[i].myX, myStars[i].myY, 0f);
        }
        myShapeRenderer.end();

        myShapeRenderer.begin(ShapeType.Filled);

        int numPointsX = myCells.length;
        int numPointsY = myCells[0].length;

        for (int x = 0; x < numPointsX; x++) {
            for (int y = 0; y < numPointsY; y++) {
                switch (myCells[x][y]) {
                    case Wall:
                        myShapeRenderer.setColor(Color.DARK_GRAY);
                        break;
                    case Open:
                        myShapeRenderer.setColor(Color.GRAY);
                        break;
                }

                if (myCells[x][y] != CellType.OuterSpace) {
                    float[][] coords = myRegions[x][y].getCoords();
                    float x1 = coords[0][0];
                    float y1 = coords[0][1];
                    for (int i = 1; i < coords.length - 1; i++) {
                        float x2 = coords[i][0];
                        float y2 = coords[i][1];
                        float x3 = coords[i + 1][0];
                        float y3 = coords[i + 1][1];
                        myShapeRenderer.triangle(x1, y1, x2, y2, x3, y3);
                    }
                }
            }
        }

        myShapeRenderer.end();

        if (myDrawCellOutlines) {
            myShapeRenderer.setColor(Color.WHITE);
            myShapeRenderer.begin(ShapeType.Point);
            myShapeRenderer.identity();
            for (int x = 0; x < numPointsX; x++) {
                for (int y = 0; y < numPointsY; y++) {
                    myShapeRenderer.point(myPoints[x][y].x, myPoints[x][y].y, 0f);
                }
            }
            myShapeRenderer.end();

            myShapeRenderer.begin(ShapeType.Line);
            for (int x = 0; x < numPointsX; x++) {
                for (int y = 0; y < numPointsY; y++) {
                    float[][] coords = myRegions[x][y].getCoords();
                    for (int i = 0; i < coords.length; i++) {
                        int i2 = (i + 1) % coords.length;
                        myShapeRenderer.line(coords[i][0], coords[i][1], coords[i2][0], coords[i2][1]);
                    }
                }
            }
            myShapeRenderer.end();
        }
    }

    private enum CellType {
        OuterSpace,
        Wall,
        Open,
    }

    private static class Star {
        float myX, myY;
        Color myColor;

        public Star(float x, float y, Color color) {
            myX = x;
            myY = y;
            myColor = color;
            myColor.a = 0.5f;
        }
    }

    private class MyInputHandler extends InputAdapter {
        private CellType myTargetCellType = CellType.Open;

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.CONTROL_LEFT) {
                myTargetCellType = CellType.Wall;
                return true;
            }
            else if (keycode == Input.Keys.ALT_LEFT) {
                myTargetCellType = CellType.OuterSpace;
                return true;
            }
            else if (keycode == Input.Keys.SPACE) {
                myDrawCellOutlines = !myDrawCellOutlines;
                return true;
            }

            return super.keyDown(keycode);
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.ALT_LEFT) {
                myTargetCellType = CellType.Open;
                return true;
            }

            return super.keyUp(keycode);
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return handleTouch(screenX, screenY);
        }

        private boolean handleTouch(int screenX, int screenY) {
            Vector3 touch = new Vector3(screenX, screenY, 0);
            myCamera.unproject(touch);

            for (int x = 0; x < myRegions.length; x++) {
                for (int y = 0; y < myRegions[x].length; y++) {
                    if (myRegions[x][y].contains(touch.x, touch.y)) {
                        myCells[x][y] = myTargetCellType;
                        break;
                    }
                }
            }

            return true;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return handleTouch(screenX, screenY);
        }
    }


}
