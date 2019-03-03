package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;

public class Kernel {
	public static double Wpoly6(Vector3d r, double h) {
        if (r.length() <= 0. || r.length() >= h) {
            return 0.;
        }

        double result = 315. / (64. * Math.PI * Math.pow(h, 9));
        result *= Math.pow((h * h - r.lengthSquared()), 3);
        return result;
    }

    public static Vector3d Wspiky(Vector3d r, double h) {
        if (r.length() <= 0. || r.length() >= h) {
            return new Vector3d(0., 0., 0.);
        }
        Vector3d norm = VMath.norm(r);
        double scale = Math.pow(h - r.length(), 2) * 45. / (Math.PI * Math.pow(h, 6));
        return VMath.scalMult(norm, -scale);
    }
}