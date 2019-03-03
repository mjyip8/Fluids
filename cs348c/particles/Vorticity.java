package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;

public class Vorticity {
	//EQUATION 15
    public static Vector3d calcVorticity(Particle p) {
        Vector3d w = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) {
            Vector3d vij = VMath.subtract(q.v, p.v);
            Vector3d pji = VMath.subtract(p.x_star, q.x_star);
            Vector3d cp = new Vector3d(0., 0., 0.);
            cp.cross(vij, Kernel.Wspiky(pji, Constants.H));
            w.add(cp);
        }
        return w;
    }

    private static Vector3d calcEta(Particle p) {
        Vector3d eta = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) {
            Vector3d grad = Kernel.Wspiky(VMath.subtract(p.x_star, q.x_star), Constants.H);
            q.density = Incompress.getDensity(q);
            grad.scale(q.m / q.density * q.omega.length());
            eta.add(grad);
        }
        return VMath.norm(eta);
    }

    //EQUATION 16
    public static Vector3d calcFVort(Particle p) {
        Vector3d eta = calcEta(p);  
        Vector3d f = new Vector3d(0., 0., 0.);
        f.cross(eta, p.omega);
        f.scale(Constants.V_EPSILON);
        return f;
    }
}