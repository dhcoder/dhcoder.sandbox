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
import dhcoder.support.collection.ArrayMap;
import dhcoder.support.collection.ArraySet;
import megamu.mesh.MPolygon;
import megamu.mesh.Voronoi;

public class MyGdxApp extends ApplicationAdapter {

    public static final int NUM_REGIONS = 2000;
    public static final int GRID_W = 100;
    public static final int GRID_H = 50;
    public static final int GRID_START_Y = GRID_H / 10;
    private Camera myCamera;
    private ShapeRenderer myShapeRenderer;
    private Region[] myRegions;
    private DrawMode myDrawMode = DrawMode.NO_BORDERS;
    private boolean myDrawGrid;
    private CaveAutomata myCave;
    private ArraySet<Segment> myDivingBorders = new ArraySet<Segment>(NUM_REGIONS);

    @Override
    public void create() {
        myCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShapeRenderer = new ShapeRenderer();
        myShapeRenderer.setProjectionMatrix(myCamera.combined);
        Gdx.input.setInputProcessor(new MyInputHandler());

        initRegions();
        initCaveGrid();
        initBorders();
    }

    private void initRegions() {
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
                myRegions[i].mySides = new Segment[coords.length];
                for (int s = 0; s < coords.length; s++) {
                    int s2 = (s + 1) % coords.length;
                    myRegions[i].mySides[s] = new Segment(
                        new Vector2(coords[s][0], coords[s][1]),
                        new Vector2(coords[s2][0], coords[s2][1]));
                }

                myRegions[i].myType = CellType.Wall;
                i++;
            }
        }
    }

    private void initCaveGrid() {
        float halfW = Gdx.graphics.getWidth() / 2.0f;
        float halfH = Gdx.graphics.getHeight() / 2.0f;

        myCave = new CaveAutomata(GRID_W, GRID_H, GRID_START_Y);
        myCave.run(3);

        for (Region myRegion : myRegions) {
            float x = myRegion.mySite.x;
            float y = myRegion.mySite.y;
            int gridX = (int) (((x + halfW) / Gdx.graphics.getWidth()) * GRID_W);
            int gridY = (int) (((y + halfH) / Gdx.graphics.getHeight()) * GRID_H);
            myRegion.myType = myCave.getCellTypes()[gridX][gridY];
        }
    }

    private void initBorders() {
        ArrayMap<Segment, CellType> myConsideringSegments = new ArrayMap<Segment, CellType>(NUM_REGIONS);
        myDivingBorders.clear();

        for (Region region : myRegions) {
            for (Segment side : region.mySides) {
                if (!myConsideringSegments.containsKey(side)) {
                    assert (!myDivingBorders.contains(side));
                    myConsideringSegments.put(side, region.myType);
                }
                else {
                    CellType cellType = myConsideringSegments.remove(side);
                    if (cellType != region.myType) {
                        myDivingBorders.put(side);
                    }
                }
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

                Vector2 coord0 = region.mySides[0].myPt1;
                for (int s = 1; s < region.mySides.length - 1; s++) {
                    Vector2 coord1 = region.mySides[s].myPt1;
                    Vector2 coord2 = region.mySides[s].myPt2;
                    myShapeRenderer.triangle(coord0.x, coord0.y, coord1.x, coord1.y, coord2.x, coord2.y);
                }
            }
        }
        else {
            float halfW = Gdx.graphics.getWidth() / 2.0f;
            float halfH = Gdx.graphics.getHeight() / 2.0f;

            CellType[][] cells = myCave.getCellTypes();
            float cellW = Gdx.graphics.getWidth() / (float) GRID_W;
            float cellH = Gdx.graphics.getHeight() / (float) GRID_H;
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

        if (myDrawMode != DrawMode.NO_BORDERS) {
            myShapeRenderer.begin(ShapeType.Point);
            myShapeRenderer.setColor(Color.WHITE);
            for (Region region : myRegions) {
                myShapeRenderer.point(region.mySite.x, region.mySite.y, 0f);
            }
            myShapeRenderer.end();
            myShapeRenderer.begin(ShapeType.Line);
            if (myDrawMode == DrawMode.ALL_BORDERS) {
                myShapeRenderer.setColor(Color.GRAY);
                for (Region region : myRegions) {
                    for (Segment side : region.mySides) {
                        myShapeRenderer.line(side.myPt1, side.myPt2);
                    }
                }
            }
            myShapeRenderer.setColor(Color.WHITE);
            for (Segment side : myDivingBorders.getKeys()) {
                myShapeRenderer.line(side.myPt1, side.myPt2);
            }

            myShapeRenderer.end();
        }
    }

    private enum DrawMode {
        NO_BORDERS,
        DIVIDING_BORDERS,
        ALL_BORDERS;

        public DrawMode getNext() {
            DrawMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static class Region {
        private Vector2 mySite;
        private Segment mySides[];
        private CellType myType;

        public boolean contains(float x, float y) {
            Vector2 pt0 = mySides[0].myPt1;
            for (int i = 1; i < mySides.length - 1; i++) {
                Vector2 pt1 = mySides[i].myPt1;
                Vector2 pt2 = mySides[i].myPt2;
                if (isPointInTriangle(x, y, pt0.x, pt0.y, pt1.x, pt1.y, pt2.x, pt2.y)) {
                    return true;
                }
            }

            return false;
        }

        private float sign(float x1, float y1, float x2, float y2, float x3, float y3) {
            return (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
        }

        private boolean isPointInTriangle(float x, float y, float tx1, float ty1, float tx2, float ty2, float tx3, float
            ty3) {
            boolean b1, b2, b3;

            b1 = sign(x, y, tx1, ty1, tx2, ty2) < 0.0f;
            b2 = sign(x, y, tx2, ty2, tx3, ty3) < 0.0f;
            b3 = sign(x, y, tx3, ty3, tx1, ty1) < 0.0f;

            return ((b1 == b2) && (b2 == b3));
        }

    }

    private static class Segment {
        private Vector2 myPt1;
        private Vector2 myPt2;

        public Segment(Vector2 pt1, Vector2 pt2) {
            myPt1 = pt1;
            myPt2 = pt2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Segment segment = (Segment) o;

            // A-B segments equal B-A segments
            if (myPt1.equals(segment.myPt1) && myPt2.equals(segment.myPt2)) return true;
            if (myPt1.equals(segment.myPt2) && myPt2.equals(segment.myPt1)) return true;
            return false;
        }

        @Override
        public int hashCode() {
            // Hashcode should be the same for both A-B and B-A segments
            return myPt1.hashCode() + myPt2.hashCode();
        }
    }

    private class MyInputHandler extends InputAdapter {
        private CellType myTargetCellType;
        private Vector2 myTouch = new Vector2();
        private Vector3 myTouch3d = new Vector3();

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            updateMyTouch(screenX, screenY);
//            myPlayerPosition.set(myTouch);

            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            updateMyTouch(screenX, screenY);
            Region r = getRegion(myTouch.x, myTouch.y);
            myTargetCellType = (r.myType == CellType.Open ? CellType.Wall : CellType.Open);

            r.myType = myTargetCellType;

            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            initBorders();
            return true;
        }

        private Region getRegion(float worldX, float worldY) {

            for (Region region : myRegions) {
                if (region.contains(worldX, worldY)) {
                    return region;
                }
            }

            throw new IllegalArgumentException();
        }

        // Call this and then myTouch3d vec will have screen coordinates
        private void updateMyTouch(int screenX, int screenY) {
            myTouch3d.set(screenX, screenY, 0f);
            myCamera.unproject(myTouch3d);
            myTouch.set(myTouch3d.x, myTouch3d.y);
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            updateMyTouch(screenX, screenY);
            Region r = getRegion(myTouch.x, myTouch.y);
            r.myType = myTargetCellType;

            return true;
        }


        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.SPACE) {
                myDrawMode = myDrawMode.getNext();
                return true;
            }
            else if (keycode == Input.Keys.TAB) {
                myDrawGrid = !myDrawGrid;
            }

            return false;
        }
    }
}
