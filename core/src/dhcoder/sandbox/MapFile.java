package dhcoder.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;

public final class MapFile {


    private final static class MapData {
        public RegionData[] regions;

        @SuppressWarnings("unused") // Used by JSON
        public MapData() {
        }

        public MapData(Region[] regions) {
            this.regions = new RegionData[regions.length];
            for (int i = 0; i < regions.length; i++) {
                this.regions[i] = new RegionData(regions[i]);
            }
        }

        public Region[] toRegions() {
            Region[] regions = new Region[this.regions.length];
            for (int i = 0; i < this.regions.length; i++) {
                regions[i] = this.regions[i].toRegion();
            }
            return regions;
        }
    }

    private final static class RegionData {
        public float x;
        public float y;
        public SideData[] sides;
        public CellType type;

        @SuppressWarnings("unused") // Used by JSON
        public RegionData() {
        }

        public RegionData(Region region) {
            x = region.getSite().x;
            y = region.getSite().y;
            sides = new SideData[region.getSides().length];
            for (int i = 0; i < region.getSides().length; i++) {
                sides[i] = new SideData(region.getSides()[i]);
            }
            type = region.getType();
        }

        public Region toRegion() {
            Region region = new Region();
            region.setSite(new Vector2(x, y));
            region.setType(type);
            Segment[] sides = new Segment[this.sides.length];
            for (int i = 0; i < this.sides.length; i++) {
                sides[i] = this.sides[i].toSegment();
            }
            region.setSides(sides);
            return region;
        }
    }

    private final static class SideData {
        public float x0;
        public float y0;
        public float x1;
        public float y1;

        @SuppressWarnings("unused") // Used by JSON
        public SideData() {
        }


        public SideData(Segment segment) {
            x0 = segment.getPt1().x;
            y0 = segment.getPt1().y;
            x1 = segment.getPt2().x;
            y1 = segment.getPt2().y;
        }

        public Segment toSegment() {
            return new Segment(new Vector2(x0, y0), new Vector2(x1, y1));
        }
    }

    public static void save(final Json json, String filename, Region[] regions) {
        FileHandle mapFile = getFileHandle(filename);

        MapData mapData = new MapData(regions);
        mapFile.writeString(json.toJson(mapData), false);
    }

    public static Region[] load(Json json, String filename) {
        FileHandle mapFile = getFileHandle(filename);
        if (mapFile.exists()) {
            MapData mapData = json.fromJson(MapData.class, mapFile.read());
            if (mapData != null) {
                return mapData.toRegions();
            }
        }

        return null;
    }

    private static FileHandle getFileHandle(String filename) {
        return Gdx.files.external(String.format("libgdx/sandbox/%s.txt", filename));
    }
}
