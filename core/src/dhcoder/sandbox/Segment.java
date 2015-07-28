package dhcoder.sandbox;

import com.badlogic.gdx.math.Vector2;

/**
 * TODO: Missing header comment
 */
public final class Segment {
    private Vector2 myPt1;
    private Vector2 myPt2;

    public Segment(Vector2 pt1, Vector2 pt2) {
        myPt1 = pt1;
        myPt2 = pt2;
    }

    public Vector2 getPt1() {
        return myPt1;
    }

    public Vector2 getPt2() {
        return myPt2;
    }

    public boolean contains(Vector2 pt) {
        return myPt1.equals(pt) || myPt2.equals(pt);
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
