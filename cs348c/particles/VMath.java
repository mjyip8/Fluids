package cs348c.particles;

import java.util.*;
import java.util.HashMap;
import javax.vecmath.*;

public class VMath
{

	public static Vector3d subtract(Vector3d u, Point3d v) {
		return new Vector3d(u.x - v.x, u.y - v.y, u.z - v.z);
	}

	public static Vector3d subtract(Point3d u, Vector3d v) {
		return new Vector3d(u.x - v.x, u.y - v.y, u.z - v.z);
	}

	public static Vector3d subtract(Vector3d u, Vector3d v) {
		return new Vector3d(u.x - v.x, u.y - v.y, u.z - v.z);
	}

	public static Vector3d subtract(Point3d u, Point3d v) {
		return new Vector3d(u.x - v.x, u.y - v.y, u.z - v.z);
	}

	public static Vector3d add(Vector3d u, Point3d v) {
		return new Vector3d(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector3d add(Point3d u, Vector3d v) {
		return new Vector3d(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector3d add(Vector3d u, Vector3d v) {
		return new Vector3d(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector3d add(Point3d u, Point3d v) {
		return new Vector3d(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector3d scalMult(Vector3d v, double c) {
		double xVal = (v.x * c == Double.NEGATIVE_INFINITY || v.x * c == Double.POSITIVE_INFINITY)? 0 : v.x * c;
		double yVal = (v.y * c == Double.NEGATIVE_INFINITY || v.y * c == Double.POSITIVE_INFINITY)? 0 : v.y * c;
		double zVal = (v.z * c == Double.NEGATIVE_INFINITY || v.z * c == Double.POSITIVE_INFINITY)? 0 : v.z * c;

		return new Vector3d(xVal, yVal, zVal);
	}

	public static Vector3d scalDiv(Vector3d v, double c) {
		return new Vector3d(v.x / c, v.y / c, v.z / c);
	}

	public static Vector3d norm(Vector3d v) {
		return (v.length() == 0)? v : scalDiv(v, v.length());
	}
}