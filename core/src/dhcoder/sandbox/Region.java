package dhcoder.sandbox;

import com.badlogic.gdx.math.Vector2;

/**
 * TODO: Missing header comment
 */
public final class Region {
    private Vector2 mySite;
    private Segment mySides[];
    private CellType myType;

    public Vector2 getSite() {
        return mySite;
    }

    public void setSite(Vector2 site) {
        mySite = site;
    }

    public Segment[] getSides() {
        return mySides;
    }

    public void setSides(Segment[] sides) {
        mySides = sides;
    }

    public CellType getType() {
        return myType;
    }

    public void setType(CellType type) {
        myType = type;
    }

    public boolean contains(float x, float y) {
        Vector2 pt0 = mySides[0].getPt1();
        for (int i = 1; i < mySides.length - 1; i++) {
            Vector2 pt1 = mySides[i].getPt1();
            Vector2 pt2 = mySides[i].getPt2();
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
