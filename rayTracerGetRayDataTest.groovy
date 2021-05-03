
//this.binding.variables.each {k,v -> println "$k = $v"}

view.loadScene("D:\\Documents\\TLEexample.krx");

data = raytracer.getRayData();

println "ray count = " + data.length;
for (int j=0; j<data.length; j++) {
    println "Element " + j + ": outer normal = " + data[j].outerNormal.x + ", " +  data[j].outerNormal.y + ", " +  data[j].outerNormal.z
    for (int i=0; i<data[j].rayVerts.length; i++) {
       // print i + " = ";
       // print data[j].rayVerts[i].x + ", ";
       // print data[j].rayVerts[i].y + ", ";
       // println data[j].rayVerts[i].z;
    }
}

model.setAttribute("showRayTracer", true);

for (int i=0; i<model.getSonicationCount(); i++) {
    model.setAttribute("currentSonication", i);
    raytracer.doSDRCalc();
    println "Sonication " + (i+1) + ":";
    println "   SDR = " + raytracer.getSDR();
    println "   Active elements = " + raytracer.getActiveElementCount();
}