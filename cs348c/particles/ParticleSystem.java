package cs348c.particles;

import java.util.*;
import java.util.HashSet;
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

    private double clamp(double x) {
        if (x <= 0.) return 0;
        if (x >= 1.) return 1.;
        return x;
    }

    private Point3d handleBoxCollisions(Point3d old) {
        Point3d result = new Point3d(old);
        result.x = clamp(result.x);
        result.y = clamp(result.y);
        result.z = clamp(result.z);
        return result;
    }

    private void addP2Grid(Particle p) {


        int x = (int) (.999999999999 * p.x_star.x * Constants.GRID_SIZE);
        int y = (int) (.999999999999 * p.x_star.y * Constants.GRID_SIZE);
        int z = (int) (.999999999999 * p.x_star.z * Constants.GRID_SIZE);

        Vector3d cell = new Vector3d(x, y, z);
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


    private double Wpoly6(Vector3d r, double h) {
        if (r.length() <= 0. || r.length() >= h) {
            return 0.;
        }

        double result = 315. / (64. * Math.PI * Math.pow(h, 9));
        result *= Math.pow((h * h - r.lengthSquared()), 3);
        //System.out.println("Wpoly6 = " + result);
        return result;
    }

    private Vector3d Wspiky(Vector3d r, double h) {
        if (r.length() <= 0. || r.length() >= h) {
            return new Vector3d(0., 0., 0.);
        }
        Vector3d norm = VMath.norm(r);
        double scale = Math.pow(h - r.length(), 2) * 45. / (Math.PI * Math.pow(h, 6));
        return VMath.scalMult(norm, -scale);
    }


    private double distance(Particle p, Particle q) {
        Vector3d diff = VMath.subtract(p.x_star, q.x_star);
        return diff.length();
    }

    private Set<Particle> getCellNeighbors(Vector3d cell, Particle p) {
        Set<Particle> Ni = new HashSet<Particle>();
        for (Particle q : grid.get(cell)) {
            if (p != q && distance(p, q) < Constants.H) {
                Ni.add(q);
            }
        }
        
        return Ni;
    } 

    private Set<Particle> getNeighbors(Particle p) {
        int x = (int) (.999999999999 * p.x_star.x * Constants.GRID_SIZE);
        int y = (int) (.999999999999 * p.x_star.y * Constants.GRID_SIZE);
        int z = (int) (.999999999999 * p.x_star.z * Constants.GRID_SIZE);
        Set<Particle> Ni = new HashSet<Particle>();

        for (int i = Math.max(0, x - 1); i < Math.min(Constants.GRID_SIZE, x + 2); i++) {
            for (int j = Math.max(0, y - 1); j < Math.min(Constants.GRID_SIZE, y + 2); j++) {
                for (int k = Math.max(0, z - 1); k < Math.min(Constants.GRID_SIZE, z + 2); k++) {
                    Vector3d curr = new Vector3d(i, j, k);
                    if (grid.containsKey(curr)) {
                        Ni.addAll(getCellNeighbors(curr, p));
                    }
                }
            }
        }

        return Ni;
    }

    private Vector3d XPSHViscosity(Particle p) {
        Vector3d result = new Vector3d(0., 0., 0.);

        for (Particle q : p.Ni) {
            Vector3d vij = VMath.subtract(q.v, p.v);
            Vector3d pij = VMath.subtract(p.x_star, q.x_star);
            double W = Wpoly6(pij, Constants.H);
            vij.scale(W);
            result.add(vij);                    
        }
        result.scale(Constants.C);
        return result;
    }

    // EQUATION 1
    private double Ci(Particle p) {
        double density = getDensity(p);
        double result = (density / Constants.RHO) - 1;
        return result;
    }

    // EQUATION 2
    private double getDensity(Particle p) {
        double density = 0.;
        for (Particle q : p.Ni) {
            density += (q.m * Wpoly6(VMath.subtract(p.x_star, q.x_star), Constants.H));
        }
        density += (p.m * Wpoly6(new Vector3d(0., 0., 0.), Constants.H));
        return density;
    }

        // EQUATION 8
    private double sumKGradCiSq(Particle p) {
        double sum_grad_Ci = 0.;
        Vector3d grad_Ci = new Vector3d(0., 0., 0.);

        if (p.Ni.size() == 0) return 0.;

        for (Particle q : p.Ni) {
            Vector3d grad_pk_Ci = Wspiky(VMath.subtract(p.x_star, q.x_star), Constants.H);
            grad_pk_Ci.scale( 1 / Constants.RHO);
            sum_grad_Ci += grad_pk_Ci.lengthSquared();
            grad_Ci.add(grad_pk_Ci);
            
        }

        double total = sum_grad_Ci + grad_Ci.lengthSquared();
        return sum_grad_Ci + grad_Ci.lengthSquared();
    }

    // EQUATION 11
    private double calcLambda(Particle p) {
        return -Ci(p)/(sumKGradCiSq(p) + Constants.EPSILON);
    }

    // EQUATION 12
    private Vector3d calcDeltaP(Particle p) {
        Vector3d delta_p = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) { //p.Ni
            Vector3d pij = VMath.subtract(p.x_star, q.x_star);
            Vector3d gradW = Wspiky(pij, Constants.H);
            double s_corr = calcSCorr(pij);
            gradW.scale(p.lambda + q.lambda - s_corr);
            delta_p.add(gradW);
        }
        return VMath.scalDiv(delta_p, Constants.RHO);
    }

    private double calcSCorr(Vector3d pij) {
        Vector3d delta_q_v = new Vector3d(Constants.DELTA_Q, 0., 0.);
        double ratio = Wpoly6(pij, Constants.H) / Wpoly6(delta_q_v, Constants.H);
        return Constants.S_CORR * Math.pow(ratio, Constants.N);
    }

    private Vector3d calcVorticity(Particle p) {
        Vector3d w = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) {
            Vector3d vij = VMath.subtract(q.v, p.v);
            Vector3d pji = VMath.subtract(p.x_star, q.x_star);
            Vector3d cp = new Vector3d(0., 0., 0.);
            cp.cross(vij, Wspiky(pji, Constants.H));
            w.add(cp);
        }
        return w;
    }

    private Vector3d calcEta(Particle p) {
        Vector3d eta = new Vector3d(0., 0., 0.);
        for (Particle q : p.Ni) {
            Vector3d grad = Wspiky(VMath.subtract(p.x_star, q.x_star), Constants.H);
            grad.scale(q.m / Math.max(getDensity(q), 100.) * q.omega.length());
            eta.add(grad);
        }
        return VMath.norm(eta);
    }

    private Vector3d calcFVort(Particle p) {
        Vector3d eta = calcEta(p);  
        Vector3d f = new Vector3d(0., 0., 0.);
        f.cross(eta, p.omega);
        f.scale(Constants.V_EPSILON);
        return f;
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
                p.x_star = handleBoxCollisions(p.x_star);
            }

            for (Particle p : P) {
                addP2Grid(p);
            }
        }

        for (Particle p : P) {
            p.Ni = getNeighbors(p);
        }

        for (int i = 0; i < Constants.DENSITY_IT; i++) {
            for (Particle p : P) {
                // calculate lambda
                p.lambda = calcLambda(p);
            }

            for (Particle p : P) {
                //calculate delta pi
                p.dp = calcDeltaP(p);
            }

            for (Particle p : P) {
                p.x_star.add(p.dp);
                p.x_star = handleBoxCollisions(p.x_star);
            }

        }


        /// TIME-STEP: (Symplectic Euler for now):
        for (Particle p : P) {
            p.omega = calcVorticity(p);
        }

        for (Particle p : P) {
            Vector3d v = new Vector3d(p.x_star);
            v.sub(p.x);
            v.scale(1 / dt);
            p.v = v;
            p.v.add(calcFVort(p));
            p.v.add(XPSHViscosity(p));

            p.x = new Point3d(p.x_star);
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
