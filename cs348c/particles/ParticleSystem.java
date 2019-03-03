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

    public double rightWallLoc = 1.;

    /** List of Particle objects. */
    public ArrayList<Particle>   P = new ArrayList<Particle>();

    /** List of Force objects. */
    public ArrayList<Force>      F = new ArrayList<Force>();

    private Grid grid = new Grid();

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


    /**
     * Simple implementation of a first-order time step. 
     * TODO: Implement the "Position Based Fluids" integrator here
     */
    public synchronized void advanceTime(double dt)
    {
        Grid grid = new Grid();

        /// Clear force accumulators:
        for(Particle p : P)  {
            p.f.set(0,0,0);
            p.x_star.set(0, 0, 0);
        }

        {/// Gather forces: (TODO)
            for(Force force : F) {
                force.applyForce();
            }

            // HACK: GRAVITY (NEED TO USE Force OBJECT)
            for(Particle p : P) {
                p.Ni.clear();
                p.f.y -= p.m * 10.f;
                p.v.scaleAdd(dt, p.f, p.v); //p.v += dt * p.f;
                p.x_star.scaleAdd(dt, p.v, p.x);
                p.x_star = Incompress.handleBoxCollisions(p.x_star, rightWallLoc);
            }

            for (Particle p : P) {
                grid.addP2Grid(p);
            }
        }

        for (Particle p : P) {
            p.Ni = grid.getNeighbors(p);
        }

        for (int i = 0; i < Constants.DENSITY_IT; i++) {
            for (Particle p : P) {
                // calculate lambda
                p.lambda = Incompress.calcLambda(p);
            }

            for (Particle p : P) {
                //calculate delta p
                p.dp = Incompress.calcDeltaP(p);
            }

            for (Particle p : P) {
                p.x_star.add(p.dp);
                p.x_star = Incompress.handleBoxCollisions(p.x_star, rightWallLoc);
            }

        }


        /// TIME-STEP: (Symplectic Euler for now):
        for (Particle p : P) {
            p.omega = Vorticity.calcVorticity(p);
        }

        for (Particle p : P) {
            Vector3d v = new Vector3d(p.x_star);
            v.sub(p.x);
            v.scale(1 / dt);
            p.v = v;
            p.v.add(Vorticity.calcFVort(p));
            p.v.add(Viscosity.XPSHViscosity(p));

            p.x = new Point3d(p.x_star);
        }

        time += dt;
        grid.clearGrid();
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
