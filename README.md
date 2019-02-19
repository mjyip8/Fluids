# CS348C Assignment \#5: Position-Based Fluids
## Introduction
In this first assignment, you will implement a particle-based fluid simulator related to smoothed particle hydrodynamics (SPH).  Your implementation will be based on the recent "Position Based Fluids" (PBF) approach described in [Macklin and Muller 2013](http://blog.mmacklin.com/publications/). You will extend a simple starter-code implementation to support the basic PBF functionality, then extend it to produce a nontrivial animation/simulation of your choosing.

### Groups
Work on your own, or in a group of at most two people. Additional work is expected from a group submission, such as a more elaborate creative artifact or modeled phenomena. PhD students are encouraged to work alone, and pursue a more challenging creative artifact. You can use the Piazza group-finding feature if you are not sure who is also looking for partners.

## Installation instructions
We provide a simple framework code in Java to get you started, primarily to support basic OpenGL rendering and a simple Swing GUI. In this assignment, you will modify this package as needed.

### Ubuntu
Extract the archive into a folder, and run the following commands from the terminal:
```
sudo apt-get install default-jre default-jdk -y
sudo apt-get install libvecmath-java -y
sudo apt-get install libjogl2-java libgluegen2-rt-java -y
make build​
```

To compile and run the simulation framework, simply do
```
make
make run
```

###
Windows and MacOS
(The setup should be exactly the same as Assignment 3: Spaghetti Factory)

1. Get eclipse. Extract the starter archive to a folder, and use the project wizard to create a java project from the source code in that folder.
1a. Create a java project with any name, and create a new package in the 'src' folder with the name cs348c.particles
1b. Add the code files from the zip folder in the cs348c/particles directory.
2. Unzip the zipfile corresponding to your OS in the lib folder. 
4. In eclipse, go to project->properties, Select Java Build Path, and Go to the Libraries tab
5. Hit "Add External JARs" (boxed in blue) and select all the jars in the starter code folder. Hit OK.
6. Then for each of jogl2.jar and gluegen2-rt.jar, hit the dropdown arrow (green), then select "Native library location:" (red) and hit the Edit button (maroon). Then select the folder where you unzipped the native libs.
7. Double-click ParticleSystemBuilder.java, and go to Run->Run.

## Rendering
To render with the Mitsuba template file:

1. In the frames folder, run the script `makeMitsubaFile.py` with
the text frame file
```
python makeMitsubaFile.py cube-drop.txt
```
This will create a file called `cube-drop.xml`, which describes the
particle positions for Mitsuba.

2. Run the mitsuba template file, telling it which particle file to use
```
mitsuba -DpFile=cube-drop.xml -o cube-drop.png particleTemplate.xml
```
You can do this for each frame, just replace cube-drop with the frame file name,
then make them into a movie. You can use mtsgui instead of mitsuba while debugging
to get an interactive viewer to adjust the camera etc.

The `particleTemplate.xml` file currently renders the particles with a blue
diffuse material. It is also possible to use a realistic water material
(currently commented out in the xml file), but it may take some fiddling
to get it to look good. You will need to tweak the camera position and view direction
to make sure it includes the entire simulation. Also feel free to tweak the lighting,
scene, materials, etc., to your heart's content.

Mitsuba will be much slower than just dumping opengl frames. Using the default `particleTemplate.xml`
file, it takes about 30 seconds to render one frame using 4 cores. You only need to render frames at
whatever framerate you want for output, probably 30 fps. By default the particle code
dumps frames at 100fps. On the flip side, if you have more time or access to
more compute power, feel free to increase the number of samples for the path tracer,
or render higher resolution images.
