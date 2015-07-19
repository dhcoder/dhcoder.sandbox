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
import megamu.mesh.MPolygon;
import megamu.mesh.Voronoi;

public class MyGdxApp extends ApplicationAdapter {

    public static final int NUM_REGIONS = 2000;
    private Camera myCamera;
    private ShapeRenderer myShapeRenderer;
    private Region[] myRegions;
    private boolean myDrawBorders;
    private boolean myDrawGrid;
    private CaveAutomata myCave;
    public static final int GRID_W = 100;
    public static final int GRID_H = 50;
    public static final int GRID_START_Y = GRID_H / 10;

    private class MyInputHandler extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.SPACE) {
                myDrawBorders = !myDrawBorders;
                return true;
            }
            else if (keycode == Input.Keys.TAB) {
                myDrawGrid = !myDrawGrid;
            }

            return false;
        }
    }

    private static class Region {
        private Vector2 mySite;
        private Vector2 myCoords[];
        private CellType myType;
    }

    @Override
    public void create() {
        myCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShapeRenderer = new ShapeRenderer();
        myShapeRenderer.setProjectionMatrix(myCamera.combined);
        Gdx.input.setInputProcessor(new MyInputHandler());

        float halfW = Gdx.graphics.getWidth() / 2.0f;
        float halfH = Gdx.graphics.getHeight() / 2.0f;

        int numRegions = NUM_REGIONS;
        float[][] voronoiPoints = new float[numRegions][2];
        for (int i = 0; i < numRegions; ++i) {
            voronoiPoints[i][0] = MathUtils.random(-halfW, halfW);
            voronoiPoints[i][1] = MathUtils.random(-halfH, halfH);
        }

        myRegions = new Region[numRegions];
        {
            Voronoi voronoi = new Voronoi(voronoiPoints);
            int i = 0;
            for (MPolygon region : voronoi.getRegions()) {
                myRegions[i] = new Region();
                myRegions[i].mySite = new Vector2(voronoiPoints[i][0], voronoiPoints[i][1]);
                float[][] coords = region.getCoords();
                myRegions[i].myCoords = new Vector2[coords.length];
                for (int c = 0; c < coords.length; c++) {
                    myRegions[i].myCoords[c] = new Vector2(coords[c][0], coords[c][1]);
                }
                myRegions[i].myType = CellType.Wall;
                i++;
            }
        }

        {
            myCave = new CaveAutomata(GRID_W, GRID_H, GRID_START_Y);
            myCave.run(3);

            for (int i = 0; i < myRegions.length; i++) {
                float x = myRegions[i].mySite.x;
                float y = myRegions[i].mySite.y;
                int gridX = (int)(((x + halfW) / Gdx.graphics.getWidth()) * GRID_W);
                int gridY = (int)(((y + halfH) / Gdx.graphics.getHeight()) * GRID_H);
                myRegions[i].myType = myCave.getCellTypes()[gridX][gridY];
            }
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        myShapeRenderer.begin(ShapeType.Filled);
        myShapeRenderer.setColor(Color.DARK_GRAY);

        boolean drawRegions = !myDrawGrid;
        if (drawRegions) {
            for (Region region : myRegions) {
                if (region.myType == CellType.Open) {
                    continue;
                }

                Vector2[] coords = region.myCoords;
                Vector2 coord0 = coords[0];
                for (int i = 1; i < coords.length - 1; i++) {
                    Vector2 coord1 = coords[i];
                    Vector2 coord2 = coords[i + 1];
                    myShapeRenderer.triangle(coord0.x, coord0.y, coord1.x, coord1.y, coord2.x, coord2.y);
                }
            }
        }
        else {
            float halfW = Gdx.graphics.getWidth() / 2.0f;
            float halfH = Gdx.graphics.getHeight() / 2.0f;

            CellType[][] cells = myCave.getCellTypes();
            float cellW = Gdx.graphics.getWidth() / (float)GRID_W;
            float cellH = Gdx.graphics.getHeight() / (float)GRID_H;
            for (int x = 0; x < cells.length; x++) {
                for (int y = 0; y < cells[x].length; y++) {
                    if (cells[x][y] == CellType.Open) {
                        continue;
                    }

                    float cellX = -halfW + x * cellW;
                    float cellY = -halfH + y * cellH;
                    myShapeRenderer.rect(cellX, cellY, cellW, cellH);
                }
            }
        }

        myShapeRenderer.end();

        if (myDrawBorders) {
            myShapeRenderer.begin(ShapeType.Point);
            myShapeRenderer.setColor(Color.WHITE);
            for (Region region : myRegions) {
                myShapeRenderer.point(region.mySite.x, region.mySite.y, 0f);
            }
            myShapeRenderer.end();
            myShapeRenderer.begin(ShapeType.Line);
            for (Region region : myRegions) {
                for (int c = 0; c < region.myCoords.length; c++) {
                    int c2 = (c + 1) % region.myCoords.length;

                    myShapeRenderer.line(region.myCoords[c], region.myCoords[c2]);
                }
            }
            myShapeRenderer.end();
        }
    }
}
