#! /bin/env python

import sys
import subprocess
from pathlib import Path

particleSize = 0.017

if len(sys.argv) != 2:
    print("Usage: " + sys.argv[0] + " <scene txt file>")
else:
    # Read the scene file
    for i in range(183):
        count = 0
        number = i;
        while (number > 0):
          number = int(number/10)
          count = count + 1
        framenum = ""
        
        for j in range(5 - count):
            framenum = framenum + str(0)
        if (i == 0):
            framenum = "0000"

        framenum = framenum + str(i);
        
        filename = sys.argv[1] + "-" + framenum
        
        scene = open(filename+ ".txt").readlines()[1:]
    
        particleString = """
        <sensor type="perspective">
            <transform name="toWorld">
                <translate x=".5" y="0.4" z ="-2.3"/>
                <rotate x="1" angle="10"/>
            </transform>
            <float name="fov" value="45"/>
            
            <sampler type="independent">
                <integer name="sampleCount" value="32"/>
            </sampler>
            
            <!-- Generate a PNG image at HD resolution -->
            <film type="ldrfilm">
                <integer name="width" value="1920"/>
                <integer name="height" value="1080"/>
            </film>
        </sensor>
        """
    
        for l in scene:
            loc = [float(x) for x in l.split()]
    
            particleString += """
        <shape type="sphere">
            <transform name="toWorld">
                <scale value="%f"/>
                <translate x="%f" y="%f" z="%f"/>
            </transform>
            <bsdf type="thindielectric"/>
        </shape>
    """ % (particleSize, loc[0], loc[1], loc[2])
    
        # Now write the file
        fName = filename
        fName += '.xml'
    
        of = open(fName, 'w')
        of.write('<scene version="0.5.0">\n')
        of.write(particleString)
        of.write('</scene>')
        
        

