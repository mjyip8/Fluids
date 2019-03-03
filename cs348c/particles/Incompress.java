package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;

//This class handles the incompressibility constrants
public class Incompress {
	private static double clamp(double x, double min, double max) {
        if (x <= min) return min;
        if (x >= max) return max;
        return x;
    }

    public static Point3d handleBoxCollisions(Point3d old, double rightWallLoc) {
        Point3d result = new Point3d(old);
        result.x = clamp(result.x, 0., rightWallLoc);
        result.y = clamp(result.y, 0., 1.);
        result.z = clamp(result.z, 0., 1.);
        return result;
    }

    // EQUATION 1
    private static double Ci(Particle p) {
        p.density = getDensity(p);
        double result = (p.density / Constants.RHO) - 1;
        return result;
    }

    // EQUATION 2
    public static double getDensity(Particle p) {
        double density = 0.;
        for (Particle q : p.Ni) {
            density += (q.m * Kernel.Wpoly6(VMath.subtract(p.x_star, q.x_star), Constants.H));
        }
        density += (p.m * Kernel.Wpoly6(new Vector3d(0., 0., 0.), Constants.H));
        return density;
    }

    // EQUATION 8
    private static double sumKGradCiSq(Particle p) {
        double sum_grad_Ci = 0.;
        Vector3d grad_Ci = new Vector3d(0., 0., 0.);

        if (p.Ni.size() == 0) return 0.;

        for (Particle q : p.Ni) {
            Vector3d grad_pk_Ci = Kernel.Wspiky(VMath.subtract(p.x_star, q.x_star), Constants.H);
            grad_pk_Ci.scale( 1 / Constants.RHO);
            sum_grad_Ci += grad_pk_Ci.lengthSquared();
            grad_Ci.add(grad_pk_Ci);
            
        }

        double total = sum_grad_Ci + grad_Ci.lengthSquared();
        return sum_grad_Ci + grad_Ci.lengthSquared();
    }

    //EQUATION 13
    private static double calcSCorr(Vector3d pij) {
        Vector3d delta_q_v = new Vector3d(Constants.DELTA_Q, 0., 0.);
        double ratio = Kernel.Wpoly6(pij, Constants.H) / Kernel.Wpoly6(delta_q_v, Constants.H);
        return Constants.S_CORR * Math.pow(ratio, Constants.N);
    }

    // EQUATION 11
    public static double calcLambda(Particle p) {
        return -Ci(p)/(sumKGradCiSq(p) + Constants.EPSILON);
    }

    // EQUATION 14
    public static Vector3d calcDeltaP(Particle p) {
        Vector3d delta_p = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) { //p.Ni
            Vector3d pij = VMath.subtract(p.x_star, q.x_star);
            Vector3d gradW = Kernel.Wspiky(pij, Constants.H);
            double s_corr = calcSCorr(pij);
            gradW.scale(p.lambda + q.lambda - s_corr);
            delta_p.add(gradW);
        }
        return VMath.scalDiv(delta_p, Constants.RHO);
    }
}