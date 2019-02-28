package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;


/**
 * Maintains dynamic lists of Particle and Force objects, and provides
 * access to their state for numerical integration of dynamics.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public class ParticleSystem //implements Serializable
{
    /** Current simulation time. */
    public double time = 0;

    /** List of Particle objects. */
    public ArrayList<Particle>   P = new ArrayList<Particle>();

    /** List of Force objects. */
    public ArrayList<Force>      F = new ArrayList<Force>();

    /**
     * true iff prog has been initialized. This cannot be done in the
     * constructor because it requires a GL2 reference.
     */
    private boolean init = false;

    /** Filename of vertex shader source. */
    public static final String[] VERT_SOURCE = {"vert.glsl"};

    /** Filename of fragment shader source. */
    public static final String[] FRAG_SOURCE = {"frag.glsl"};

    /** The shader program used by the particles. */
    ShaderProgram prog;

    private double h = Constants.H;

    private double wpoly6_c = 315. / (64. * Math.PI * 64. * Math.pow(h, 9));

    private double wspiky_c = 45./ (Math.PI * Math.pow(h, 6));

    private double inv_rho = 1 / Constants.RHO;


    /** Basic constructor. */
    public ParticleSystem() {}

    /**
     * Set up the GLSL program. This requires that the current directory (i.e. the package in which
     * this class resides) has a vertex and fragment shader.
     */
    public synchronized void init(GL2 gl) {
        if (init) return;

        prog = new ShaderProgram();
        ShaderCode vert_code = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(), VERT_SOURCE, false);
        ShaderCode frag_code = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(), FRAG_SOURCE, false);
        if (!prog.add(gl, vert_code, System.err) || !prog.add(gl, frag_code, System.err)) {
            System.err.println("WARNING: shader did not compile");
            prog.init(gl); // Initialize empty program
        } else {
            prog.link(gl, System.err);
        }

        init = true;
    }

    /** Adds a force object (until removed) */
    public synchronized void addForce(Force f) {
        F.add(f);
    }

    /** Useful for removing temporary forces, such as user-interaction
     * spring forces. */
    public synchronized void removeForce(Force f) {
        F.remove(f);
    }

    /** Creates particle and adds it to the particle system.
     * @param p0 Undeformed/material position.
     * @return Reference to new Particle.
     */
    public synchronized Particle createParticle(Point3d p0)
    {
        Particle newP = new Particle(p0);
        P.add(newP);
        return newP;
    }

    /**
     * Helper-function that computes the nearest particle to the specified
     * (deformed) position.
     * @return Nearest particle, or null if no particles.
     */
    public synchronized Particle getNearestParticle(Point3d x)
    {
        Particle minP      = null;
        double   minDistSq = Double.MAX_VALUE;
        for(Particle particle : P) {
            double distSq = x.distanceSquared(particle.x);
            if(distSq < minDistSq) {
                minDistSq = distSq;
                minP = particle;
            }
        }
        return minP;
    }

    /** Moves all particles to undeformed/materials positions, and
     * sets all velocities to zero. Synchronized to avoid problems
     * with simultaneous calls to advanceTime(). */
    public synchronized void reset()
    {
        for(Particle p : P)  {
            p.x.set(p.x0);
            p.v.set(0,0,0);
            p.f.set(0,0,0);
            p.setHighlight(false);
        }
        time = 0;
    }

    // refer to slides
    private double Wpoly6(double r) {
        if (r >= h) return 0.;

        return wpoly6_c * Math.pow(Math.pow(h, 2) - Math.pow(r, 2), 3);
    }

    private Vector3d Wspiky(Vector3d r) {
        if (r.length() >= h) return new Vector3d(0., 0., 0.);

        Vector3d grad = new Vector3d(r);
        double ratio = wspiky_c * Math.pow(h - r.length(), 2);
        if (grad.length() != 0.) {
            grad.scale(1/r.length());
        }
        grad.scale(ratio);
        return grad;
    }

    //EQN 11
    private double calcLambda(Particle p) {
        return -Ci(p) / (sumKGrad(p) + Constants.EPSILON);
    }

    //EQN 1
    private double Ci(Particle i) {
        return (density(i) / Constants.RHO) - 1;
    }

    //EQN 2
    private double density(Particle i) {
        double rho = 0.;
        Point3d pi = i.x_star;

        for (Particle j : P) {
            Vector3d pij = new Vector3d(pi);
            Point3d pj = j.x_star;
            pij.sub(pj);

            if (pij.length() < h) {
                rho += (j.m * Wpoly6(pij.length()));
            }
        }
        return rho;
    }

    //EQN 8
    private double sumKGrad(Particle i) {
        Point3d pi = i.x_star;
        double sum_grad_pk_Ci = 0.;
        Vector3d sum_pk_grad = new Vector3d(0., 0., 0.);

        for (Particle j : P) {
            if (j != i) {
                Vector3d pij = new Vector3d(pi);
                Point3d pj = j.x_star;
                pij.sub(pj);

                if (pij.length() < h) { //neighbor check
                    Vector3d grad_pij = Wspiky(pij);
                    grad_pij.scale(inv_rho);
                    sum_pk_grad.add(grad_pij);

                    sum_grad_pk_Ci -= grad_pij.lengthSquared();
                }
            }
        }
        sum_grad_pk_Ci += sum_pk_grad.lengthSquared();
        return sum_grad_pk_Ci;
    }

    //EQN 12
    private Point3d calcDeltaP(Particle i) {
        Point3d pi = i.x_star;
        Point3d dp = new Point3d(0., 0., 0.);

        for (Particle j : P) {
            if (j != i) {
                Vector3d pij = new Vector3d(pi);
                Point3d pj = j.x_star;
                pij.sub(pj);

                if (pij.length() < h) { //neighbor check
                    Vector3d grad = Wspiky(pij);
                    grad.scale(i.lambda + j.lambda);
                    dp.add(grad);
                }
            }
        }
        dp.scale(inv_rho);
        return dp;
    }

    private double clamp(double val, double min, double max){
        return Math.max(min, Math.min(max, val));
    }

    private Point3d enforceBoundaryConstraints(Point3d v) {
        Point3d new_pos = new Point3d(v);
        new_pos.x = clamp(new_pos.x, 0., 1.);
        new_pos.y = clamp(new_pos.y, 0., 1.);
        new_pos.z = clamp(new_pos.z, 0., 1.);
        return new_pos;
    }

    /**
     * Simple implementation of a first-order time step. 
     * TODO: Implement the "Position Based Fluids" integrator here
     */
    public synchronized void advanceTime(double dt)
    {
        /// Clear force accumulators:
        for(Particle p : P)  p.f.set(0,0,0);

        {/// Gather forces: (TODO)
            for(Force force : F) {
                force.applyForce();
            }

            // HACK: GRAVITY (NEED TO USE Force OBJECT)
            for(Particle p : P) {
                p.f.y -= p.m * 10.f;
            }

        }

        /// TIME-STEP: (Symplectic Euler for now):
        for(Particle p : P) {
            p.v.scaleAdd(dt, p.f, p.v); //p.v += dt * p.f;
            p.x_star.scaleAdd(dt, p.v, p.x); //p.x += dt * p.v;
        }

        for (int i = 0; i < 1; i++) {
            for (Particle p : P) {
                p.lambda = calcLambda(p);
            }

            for (Particle p : P) {
                p.dp = calcDeltaP(p);
            }

            for (Particle p : P) {
                p.x_star.add(p.dp);
                p.x_star = enforceBoundaryConstraints(p.x_star);
            }
        }

        for (Particle p : P) {
            // update v
            Vector3d new_v = new Vector3d(p.x_star);
            new_v.sub(p.x);
            new_v.scale(1 / dt);
            p.v = new_v;

            //update x
            p.x = new Point3d(p.x_star);
        }

        time += dt;
    }

    /**
     * Displays Particle and Force objects. Modify how you like.
     */
    public synchronized void display(GL2 gl)
    {
        for(Force force : F) {
            force.display(gl);
        }

        if(!init) init(gl);

        prog.useProgram(gl, true);

        for(Particle particle : P) {
            particle.display(gl);
        }

        prog.useProgram(gl, false);
    }
}
