package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;

//This class adds viscosity behavior
public class Viscosity {

    //EQUATION 17
	public static Vector3d XPSHViscosity(Particle p) {
        Vector3d result = new Vector3d(0., 0., 0.);

        for (Particle q : p.Ni) {
            Vector3d vij = VMath.subtract(q.v, p.v);
            Vector3d pij = VMath.subtract(p.x_star, q.x_star);
            double W = Kernel.Wpoly6(pij, Constants.H);
            vij.scale(W);
            result.add(vij);                    
        }
        result.scale(Constants.C);
        return result;
    }
}