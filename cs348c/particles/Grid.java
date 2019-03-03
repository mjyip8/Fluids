package cs348c.particles;

import java.util.*;
import java.util.HashSet;
import javax.vecmath.*;

//This class helps accelerate neighbor finding
public class Grid
{
	private HashMap<Vector3d, Set<Particle>> grid = new HashMap<Vector3d, Set<Particle>>();

	public Grid() {
		grid = new HashMap<Vector3d, Set<Particle>>();
	}

	public void clearGrid() {
		grid.clear();
	}

	public void addP2Grid(Particle p) {
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

    public Set<Particle> getNeighbors(Particle p) {
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
}