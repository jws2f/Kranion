
//this.binding.variables.each {k,v -> println "$k = $v"}

view.loadScene("D:\\Documents\\TLEexample.krx"); // Load a Kranion file or comment out to operate on currently loaded dataset

model.setAttribute("showRayTracer", true);

data = raytracer.getRayData();
sdrs = raytracer.getSDRsFull();
angles = raytracer.getIncidentAnglesFull();
thickness = raytracer.getNormSkullThicknesses()

println "ray count = " + data.length;
for (int j=0; j<data.length; j++) {
    println "Element " + j + ": outer normal = " + data[j].outerNormal.x + ", " +  data[j].outerNormal.y + ", " +  data[j].outerNormal.z
    println "    SDR = " + sdrs[j] + "  IncidentAngle = " + angles[j] + "  Skull thickness = " + thickness[j];
    println "    Element location = " +  data[j].rayVerts[0];
    
//    println "   Ray verticies (element, outer skull, inner skull, nearest to target)"
//    for (int i=0; i<data[j].rayVerts.length; i++) {
//        print "\t" +  i + " = ";
//        print "\t" +  data[j].rayVerts[i].x + ", ";
//        print "\t" +  data[j].rayVerts[i].y + ", ";
//        println "\t" +  data[j].rayVerts[i].z;
//    }
}

model.setAttribute("showRayTracer", true);

for (int i=0; i<model.getSonicationCount(); i++) {
    model.setAttribute("currentSonication", i);
    raytracer.doSDRCalc();
    println "Sonication " + (i+1) + ":";
    println "   SDR = " + raytracer.getSDR();
    println "   Active elements = " + raytracer.getActiveElementCount();
}
