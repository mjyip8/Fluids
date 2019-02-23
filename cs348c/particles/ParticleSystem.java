package cs348c.particles;

import java.util.*;
import java.util.HashMap;
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

    public HashMap<Vector3d, Set<Particle>> grid = new HashMap<Vector3d, Set<Particle>>();

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

    private Point3d handleBoxCollisions(Point3d old) {
        Point3d result = new Point3d();
        result.x = (old.x <= 0)? 0 : old.x;
        result.x = (old.x >= 1)? 1 : result.x; 
        result.y = (old.y <= 0)? 0 : old.y;
        result.y = (old.y >= 1)? 1 : result.y;         
        result.z = (old.z <= 0)? 0 : old.z;
        result.z = (old.z >= 1)? 1 : result.z; 
        return result;
    }

    private void addP2Grid(Particle p) {
        Vector3d cell = new Vector3d((int) (p.x.x / Constants.GRID_SIZE),
                                     (int) (p.x.y / Constants.GRID_SIZE),
                                     (int) (p.x.z / Constants.GRID_SIZE));
        if (!grid.containsKey(cell)) {
            Set<Particle> residents = new HashSet<Particle>();
            residents.add(p);
            grid.put(cell, residents);
        } else {

            Set<Particle> residents = grid.get(cell);
            residents.add(p);
            grid.put(cell, residents);
        }
    }

    private double distance(Particle p, Particle q) {
        Vector3d diff = new Vector3d(p.x.x - q.x.x, p.x.y - q.x.y, p.x.z - q.x.z);
        return diff.length();
    }

    private Set<Particle> getCellNeighbors(Vector3d cell, Particle p) {
        Set<Particle> Ni = new HashSet<Particle>();
        for (Particle q : grid.get(cell)) {
            if (p != q && distance(p, q) <= Constants.H) {
                Ni.add(q);
            }
        }
        
        return Ni;
    } 

    private Set<Particle> getNeighbors(Particle p) {
        int x = (int) (p.x.x / Constants.GRID_SIZE);
        int y = (int) (p.x.y / Constants.GRID_SIZE);
        int z = (int) (p.x.z / Constants.GRID_SIZE);
        Set<Particle> Ni = new HashSet<Particle>();

        for (int i = x - 1; i < x + 2; i++) {
            for (int j = y - 1; j < y + 2; j++) {
                for (int k = z - 1; k < z + 2; k++) {
                    Vector3d curr = new Vector3d(i, j, k);
                    if (grid.containsKey(curr)) {
                        Ni.addAll(getCellNeighbors(curr, p));
                    }
                }
            }
        }

        return Ni;
    }

    /**
     * Simple implementation of a first-order time step. 
     * TODO: Implement the "Position Based Fluids" integrator here
     */
    public synchronized void advanceTime(double dt)
    {
        grid = new HashMap<Vector3d, Set<Particle>>();

        /// Clear force accumulators:
        for(Particle p : P)  {
            p.f.set(0,0,0);
            addP2Grid(p);
        }

        {/// Gather forces: (TODO)
            for(Force force : F) {
                force.applyForce();
            }

            // HACK: GRAVITY (NEED TO USE Force OBJECT)
            for(Particle p : P)   p.f.y -= p.m * 10.f;

        }

        /// TIME-STEP: (Symplectic Euler for now):
        for(Particle p : P) {
            Set<Particle> Ni = getNeighbors(p);

            p.v.scaleAdd(dt, p.f, p.v); //p.v += dt * p.f;
            p.x.scaleAdd(dt, p.v, p.x); //p.x += dt * p.v;
            //for (n : Ni) {
                //handle XPSHViscosity

            //}
            System.out.println(Ni.size());
            //p.v = XPSHViscosity(p.v);
            p.x = handleBoxCollisions(p.x);

        }


        time += dt;
        grid.clear();
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
