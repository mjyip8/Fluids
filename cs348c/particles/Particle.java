package cs348c.particles;

import javax.vecmath.*;
import java.util.*;
import java.util.HashSet;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;

/**
 * Simple particle implementation, with miscellaneous adornments.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public class Particle
{
    /** Radius of particle's sphere graphic. */
    public static final double PARTICLE_RADIUS = 0.015;

    /** Display list index. */
    private static int PARTICLE_DISPLAY_LIST = -1;

    /** Highlighted appearance if true, otherwise white. */
    private boolean highlight = false;

    /** Default mass. */
    double   m = Constants.PARTICLE_MASS;

    /** Deformed Position. */
    Point3d  x = new Point3d();

    Point3d x_star = new Point3d();

    Vector3d dp = new Vector3d();

    Vector3d omega = new Vector3d();

    Set<Particle> Ni = new HashSet<Particle>();

    /** Undeformed/material Position. */
    Point3d  x0 = new Point3d();

    /** Velocity. */
    Vector3d v = new Vector3d();
    
    /** Force accumulator. */
    Vector3d f = new Vector3d();

    double lambda = 0;

    double density = 7000.;

    /**
     * Constructs particle with the specified material/undeformed
     * coordinate, x0.
     */
    Particle(Point3d x0)
    {
        this.x0.set(x0);
        x.set(x0);
    }

    /** Draws spherical particle using a display list. */
    public void display(GL2 gl)
    {
        if(PARTICLE_DISPLAY_LIST < 0) {// MAKE DISPLAY LIST:
            int displayListIndex = gl.glGenLists(1);
            GLU glu = GLU.createGLU();
            GLUquadric quadric = glu.gluNewQuadric();
            gl.glNewList(displayListIndex, GL2.GL_COMPILE);
            glu.gluSphere(quadric, PARTICLE_RADIUS, 16, 8);
            gl.glEndList();
            glu.gluDeleteQuadric(quadric);

            // For older versions of JOGL
            //glu.destroy();

            System.out.println("MADE DISPLAY LIST "+displayListIndex+" : "+gl.glIsList(displayListIndex));
            PARTICLE_DISPLAY_LIST = displayListIndex;
        }

        /// COLOR: DEFAULT CYAN; GREEN IF HIGHLIGHTED
        float[] c = {0f, 1f, 1f, 1f};//default: cyan
        if(highlight) {
            c[2] = 0;
        } 


        // Hack to make things more colorful/interesting
        c[1] = (float)x.y;

        if (density > 5300) {
            if (density > 6500) {
                c[1] = 0;
            } else {
                c[0] = ((float) density - 5300)/1500;
                c[1] = ((float) density - 5300)/(6500 - 5300);
            }
        } else if (v.length() > 1) {
            c[0] = 1;
            c[1] = 1;
        }

        if (x.y <= 0.02) {
            c[1] = (float) x.y;
            c[0] = 0;
        }
        
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, c, 0); // Color used by shader

        /// DRAW ORIGIN-CIRCLE TRANSLATED TO "p":
        gl.glPushMatrix();
        gl.glTranslated(x.x, x.y, x.z);
        gl.glCallList(PARTICLE_DISPLAY_LIST); // Draw the particle
        gl.glPopMatrix();
    }

    /** Specifies whether particle should be drawn highlighted. */
    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }
    /** True if particle should be drawn highlighted. */
    public boolean getHighlight() {
        return highlight;
    }
}
