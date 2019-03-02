package cs348c.particles;

/**
 * Default constants. Add your own as necessary.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public interface Constants
{
    /** Mass of a particle. */
    public static final double PARTICLE_MASS     = 1.0;
    public static final double H = .1;
    public static final double RHO = 6378.0;
    public static final int DENSITY_IT = 4;
    public static final double DT = 0.0083;
    public static double EPSILON = 600.;
    public static final double S_CORR = 0.0001;
    public static final double DELTA_Q = .03;
    public static final double N = 4;
    public static final double C = 0.00001;
    public static final double V_EPSILON = .0000018;

    public static final int GRID_SIZE = 10;


    /** Camera rotation speed constants. */
    public static final double CAM_SIN_THETA     = Math.sin(0.2);
    public static final double CAM_COS_THETA     = Math.cos(0.2);
}
