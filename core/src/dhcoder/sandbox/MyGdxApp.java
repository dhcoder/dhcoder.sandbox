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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import dhcoder.support.collection.ArrayMap;
import dhcoder.support.collection.ArraySet;
import megamu.mesh.MPolygon;
import megamu.mesh.Voronoi;

public class MyGdxApp extends ApplicationAdapter {

    public static final int NUM_REGIONS = 2000;
    public static final int GRID_W = 100;
    public static final int GRID_H = 50;
    public static final int GRID_START_Y = GRID_H / 10;
    public static final String TAG = "SANDBOX";
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
        myDivingBorders.clear();

        for (Region region : myRegions) {
            for (Segment side : region.getSides()) {
                if (!myConsideringSegments.containsKey(side)) {
                    assert (!myDivingBorders.contains(side));
                    myConsideringSegments.put(side, region.getType());
                }
                else {
                    CellType cellType = myConsideringSegments.remove(side);
                    if (cellType != region.getType()) {
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
            for (Segment side : myDivingBorders.getKeys()) {
                myShapeRenderer.line(side.getPt1(), side.getPt2());
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

            return false;
        }
    }
}
