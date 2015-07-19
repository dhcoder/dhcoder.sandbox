package megamu.mesh;

public class MPolygon {

	float[][] coords;
	int count;
	
	public MPolygon(){
		this(0);
	}

	public MPolygon(int points){
		coords = new float[points][2];
		count = 0;
	}

	public void add(float x, float y){
		coords[count][0] = x;
		coords[count++][1] = y;
	}

	public int count(){
		return count;
	}

	public float[][] getCoords(){
		return coords;
	}

	public boolean contains(float x, float y) {
		float[] pt0 = coords[0];
		for (int i = 1; i < coords.length - 1; i++) {
			float[] pt1 = coords[i];
			float[] pt2 = coords[i+1];
			if (isPointInTriangle(x, y, pt0[0], pt0[1], pt1[0], pt1[1], pt2[0], pt2[1])) {
				return true;
			}
		}

		return false;
	}

	private float sign(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		return (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
	}

	private boolean isPointInTriangle(float x, float y, float tx1, float ty1, float tx2, float ty2, float tx3, float ty3)
	{
		boolean b1, b2, b3;

		b1 = sign(x, y, tx1, ty1, tx2, ty2) < 0.0f;
		b2 = sign(x, y, tx2, ty2, tx3, ty3) < 0.0f;
		b3 = sign(x, y, tx3, ty3, tx1, ty1) < 0.0f;

		return ((b1 == b2) && (b2 == b3));
	}

}