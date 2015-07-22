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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import dhcoder.support.collection.ArrayMap;
import dhcoder.support.collection.ArraySet;
import megamu.mesh.MPolygon;
import megamu.mesh.Voronoi;

import java.util.ArrayList;
import java.util.Comparator;

public class MyGdxApp extends ApplicationAdapter {

    public static final String TAG = "SANDBOX";
    public static final int NUM_REGIONS = 2000;
    public static final int GRID_W = 100;
    public static final int GRID_H = 50;
    public static final int GRID_START_Y = GRID_H / 10;
    public static final float VISION_RADIUS = 100f;
    private Camera myCamera;
    private ShapeRenderer myShapeRenderer;
    private Vector2 myPos;
    private Region[] myRegions;
    private DrawMode myDrawMode = DrawMode.NO_BORDERS;
    private boolean myDrawGrid;
    private boolean myDrawVision = true;
    private CaveAutomata myCave;
    private ArraySet<Segment> myDividingBorders = new ArraySet<Segment>(NUM_REGIONS);
    private ArrayList<Vector2> myVisiblePoints = new ArrayList<Vector2>();
    private ArrayList<Float> myTest;

    @Override
    public void create() {
        myCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShapeRenderer = new ShapeRenderer();
        myShapeRenderer.setProjectionMatrix(myCamera.combined);
        Gdx.input.setInputProcessor(new MyInputHandler());
        myPos = new Vector2(0f, 0f);

        initRegions();
        initCaveGrid();
        initBorders();
    }

    private void updateVisiblePoints() {
        myVisiblePoints.clear();
        ArrayList<Segment> bordersInRange = new ArrayList<Segment>();
        final Vector2 tmp = new Vector2();

        float vision2 = VISION_RADIUS * VISION_RADIUS;
        for (Segment segment : myDividingBorders.getKeys()) {
            if (tmp.set(segment.getPt1()).sub(myPos).len2() <= vision2) {
                bordersInRange.add(segment);
            }
            else if (tmp.set(segment.getPt2()).sub(myPos).len2() <= vision2) {
                bordersInRange.add(segment);
            }
        }

        for (Segment border : bordersInRange) {
            myVisiblePoints.add(new Vector2(border.getPt1()));
            myVisiblePoints.add(new Vector2(border.getPt2()));
        }

        // Update visible points if they're blocked
        for (Vector2 pt : myVisiblePoints) {
            for (Segment border : bordersInRange) {
                if (border.getPt1().equals(pt) || border.getPt2().equals(pt)) {
                    continue;
                }
                boolean intersected = Intersector.intersectSegments(myPos, pt, border.getPt1(), border.getPt2(), tmp);
                if (intersected) {
                    pt.set(tmp);
                }
            }
        }

        myVisiblePoints.sort(new Comparator<Vector2>() {
            @Override
            public int compare(Vector2 o1, Vector2 o2) {
                float angle1 = tmp.set(o1).sub(myPos).angle();
                float angle2 = tmp.set(o2).sub(myPos).angle();

                return Float.compare(angle1, angle2);
            }
        });

        for (int i = 1; i < myVisiblePoints.size(); i++) {
            int i0 = i - 1;
            if (myVisiblePoints.get(i0).equals(myVisiblePoints.get(i))) {
                myVisiblePoints.remove(i);
                --i;
            }
        }

        myTest = new ArrayList<Float>(myVisiblePoints.size());
        for (Vector2 pt : myVisiblePoints) {
            tmp.set(pt).sub(myPos);
            myTest.add(tmp.angle());
        }
        int breakhere = 0;
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
                myRegions[i].setSite(new Vector2(voronoiPoints[i][0], voronoiPoints[i][1]));
                float[][] coords = region.getCoords();
                Segment[] sides = new Segment[coords.length];
                for (int s = 0; s < coords.length; s++) {
                    int s2 = (s + 1) % coords.length;
                    sides[s] = new Segment(
                        new Vector2(coords[s][0], coords[s][1]),
                        new Vector2(coords[s2][0], coords[s2][1]));
                }

                myRegions[i].setSides(sides);
                myRegions[i].setType(CellType.Wall);
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
            float x = myRegion.getSite().x;
            float y = myRegion.getSite().y;
            int gridX = (int) (((x + halfW) / Gdx.graphics.getWidth()) * GRID_W);
            int gridY = (int) (((y + halfH) / Gdx.graphics.getHeight()) * GRID_H);
            myRegion.setType(myCave.getCellTypes()[gridX][gridY]);
        }
    }

    private void initBorders() {
        ArrayMap<Segment, CellType> myConsideringSegments = new ArrayMap<Segment, CellType>(NUM_REGIONS);
        myDividingBorders.clear();

        for (Region region : myRegions) {
            for (Segment side : region.getSides()) {
                if (!myConsideringSegments.containsKey(side)) {
                    assert (!myDividingBorders.contains(side));
                    myConsideringSegments.put(side, region.getType());
                }
                else {
                    CellType cellType = myConsideringSegments.remove(side);
                    if (cellType != region.getType()) {
                        myDividingBorders.put(side);
                    }
                }
            }
        }

        updateVisiblePoints();
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
                if (region.getType() == CellType.Open) {
                    continue;
                }

                Segment[] sides = region.getSides();
                Vector2 coord0 = sides[0].getPt1();
                for (int s = 1; s < sides.length - 1; s++) {
                    Vector2 coord1 = sides[s].getPt1();
                    Vector2 coord2 = sides[s].getPt2();
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
                myShapeRenderer.point(region.getSite().x, region.getSite().y, 0f);
            }
            myShapeRenderer.end();
            myShapeRenderer.begin(ShapeType.Line);
            if (myDrawMode == DrawMode.ALL_BORDERS) {
                myShapeRenderer.setColor(Color.GRAY);
                for (Region region : myRegions) {
                    for (Segment side : region.getSides()) {
                        myShapeRenderer.line(side.getPt1(), side.getPt2());
                    }
                }
            }
            myShapeRenderer.setColor(Color.WHITE);
            for (Segment side : myDividingBorders.getKeys()) {
                myShapeRenderer.line(side.getPt1(), side.getPt2());
            }

            myShapeRenderer.end();
        }
        else {
            // Vision!
            myShapeRenderer.begin(ShapeType.Line);
            myShapeRenderer.setColor(Color.LIGHT_GRAY);
            Vector2 coord0 = myPos;
            for (int i = 1; i < myVisiblePoints.size(); i++) {
                int i0 = i - 1;
                Vector2 coord1 = myVisiblePoints.get(i0);
                Vector2 coord2 = myVisiblePoints.get(i);
                myShapeRenderer.triangle(coord0.x, coord0.y, coord1.x, coord1.y, coord2.x, coord2.y);
            }
            myShapeRenderer.end();
//            myShapeRenderer.begin(ShapeType.Filled);
//            myShapeRenderer.setColor(Color.RED);
//            int i = 0;
//            for (Vector2 pt : myVisiblePoints) {
//                myShapeRenderer.setColor(new Color((float)i / myVisiblePoints.size(), 0f, 1.0f, 1.0f));
//                myShapeRenderer.circle(pt.x, pt.y, 2f);
//                i++;
//            }
//            myShapeRenderer.end();
        }

        myShapeRenderer.begin(ShapeType.Filled);
        myShapeRenderer.setColor(Color.ORANGE);
        myShapeRenderer.circle(myPos.x, myPos.y, 2f);
        myShapeRenderer.end();
        if (myDrawVision) {
            myShapeRenderer.begin(ShapeType.Line);
            myShapeRenderer.circle(myPos.x, myPos.y, VISION_RADIUS);
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

    private enum InputMode {
        NOT_LISTENING,
        LISTENING,
        CONFIRMING,
    }

    private class MyInputHandler extends InputAdapter {
        private CellType myTargetCellType;
        private InputMode myInputMode = InputMode.NOT_LISTENING;
        private int myFileTarget;
        private boolean myLoadFile;
        private Vector2 myTouch = new Vector2();
        private Vector3 myTouch3d = new Vector3();
        private Json myJson = new Json(JsonWriter.OutputType.json);


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
            myTargetCellType = (r.getType() == CellType.Open ? CellType.Wall : CellType.Open);

            r.setType(myTargetCellType);

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
            r.setType(myTargetCellType);

            return true;
        }


        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.SPACE) {
                myDrawMode = myDrawMode.getNext();
                return true;
            }
            else if (keycode == Input.Keys.SHIFT_LEFT) {
                myDrawVision = !myDrawVision;
                return true;
            }
            else if (keycode == Input.Keys.TAB) {
                myDrawGrid = !myDrawGrid;
                return true;
            }
            else if (keycode == Input.Keys.S || keycode == Input.Keys.L) {
                if (myInputMode == InputMode.NOT_LISTENING) {
                    myInputMode = InputMode.LISTENING;
                    myLoadFile = keycode == Input.Keys.L;
                    Gdx.app.log(TAG, String.format("Press 0-9 to %s map, ESC to cancel", myLoadFile ? "load" : "save"));
                    return true;
                }
            }
            else if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
                if (myInputMode == InputMode.LISTENING) {
                    myInputMode = InputMode.CONFIRMING;
                    myFileTarget = keycode - Input.Keys.NUM_0;
                    Gdx.app.log(TAG, String.format("Target to %s: %d. Are you sure? (y/n)",
                        myLoadFile ? "load" : "save", myFileTarget));
                    return true;
                }
            }
            else if (keycode == Input.Keys.Y) {
                if (myInputMode == InputMode.CONFIRMING) {
                    String filename = Integer.toString(myFileTarget);
                    if (myLoadFile) {
                        Gdx.app.log(TAG, String.format("Loading: %s...", filename));
                        Region[] loaded = MapFile.load(myJson, filename);
                        if (loaded != null) {
                            myRegions = loaded;
                            initBorders();
                            myCave.clear();
                            Gdx.app.log(TAG, "Success!");
                        }
                        else {
                            Gdx.app.log(TAG, "Failed");
                        }
                    }
                    else {
                        MapFile.save(myJson, filename, myRegions);
                        Gdx.app.log(TAG, String.format("Saved: %s", filename));
                    }
                    myInputMode = InputMode.NOT_LISTENING;
                    return true;
                }
            }
            else if (keycode == Input.Keys.N) {
                if (myInputMode == InputMode.CONFIRMING) {
                    myInputMode = InputMode.NOT_LISTENING;
                    Gdx.app.log(TAG, String.format("%s cancelled", myLoadFile ? "Load" : "Save"));
                    return true;
                }
            }
            else if (keycode == Input.Keys.ESCAPE) {
                if (myInputMode != InputMode.NOT_LISTENING) {
                    myInputMode = InputMode.NOT_LISTENING;
                    Gdx.app.log(TAG, String.format("%s cancelled", myLoadFile ? "Load" : "Save"));
                    return true;
                }
            }
            else if (keycode == Input.Keys.LEFT) {
                myPos.x -= 5f;
                updateVisiblePoints();
                return true;
            }
            else if (keycode == Input.Keys.RIGHT) {
                myPos.x += 5f;
                updateVisiblePoints();
                return true;
            }
            else if (keycode == Input.Keys.UP) {
                myPos.y += 5f;
                updateVisiblePoints();
                return true;
            }
            else if (keycode == Input.Keys.DOWN) {
                myPos.y -= 5f;
                updateVisiblePoints();
                return true;
            }

            return false;
        }
    }
}
