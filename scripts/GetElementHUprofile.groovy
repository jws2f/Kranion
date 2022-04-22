


model.setAttribute("showRayTracer", true);
data = raytracer.getRayData();

float waterSpeed = 1482f;

raytracer.setBoneRefractionSpeed(waterSpeed);
raytracer.setBoneSpeed(waterSpeed);

raytracer.doCalc();
raytracer.setIsDirty(true);
view.updateTargetAndSteering();
view.updatePressureCalc();
view.setIsDirty(true);


for (int r=0; r<3; r++) {

println "ray count = " + data.length;
for (int j=0; j<data.length; j++) {
    println "Element " + j
    
    List<Float> hu = raytracer.getHUprofile(j, 700f);
    
    int count = 0;
    float sum = 0f;
    hu.each {
        //println("\t" + it);
        count++;
        sum += org.fusfoundation.kranion.CTSoundSpeed.lookupSpeed((float)(it + 1000f));
    }
    
   // println("SOS: " + sum/count);
    
    raytracer.setBoneRefractionSpeed(j, (float)(sum/count));

}

raytracer.doCalc();
raytracer.setIsDirty(true);
view.updateTargetAndSteering();
view.updatePressureCalc();
view.setIsDirty(true);

}

println("Done.");