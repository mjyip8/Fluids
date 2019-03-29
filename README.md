# Position-Based Fluids
## IMPLEMENTATION DETAILS
I implemented a particle-based fluid simulator related to smoothed particle hydrodynamics (SPH), based on the "Position Based Fluids" (PBF) approach described in [Macklin and Muller 2013](http://blog.mmacklin.com/publications/). As outlined by the paper, I used density to enforce incompressibility in the Incompress class. I added surface tension for more blobbiness and vorticity confinement in the Vorticity class and viscosity in the Viscosity class for more fluid-like motion. 

## EXTRA FEATURES
1. I rendered dam break in Mitsuba.
![alt text](https://raw.githubusercontent.com/mjyip8/Fluids/master/artifacts/mitsuba_render.gif)

2. I also created an feature in ParticleSystemBuilder where you can change the position of one of the boxes walls while the simulation is going. You press the key 'J' to go to the left and the key 'K' to go to the right.

3. I changed the color of the particles in Particle class so that particles in areas with less density are more white to make the waves look more clear.
