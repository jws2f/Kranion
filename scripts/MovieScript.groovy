model.setAttribute("foregroundVolumeSlices", 600f);
model.setAttribute("showRayTracer", true);

 model.setAttribute("currentSonication", 22);
sleep(2000);
 model.setAttribute("currentSonication", 23);
sleep(2000);

model.setAttribute("showRayTracer", false);

for (int i=600; i>=0; i-=10) {

    model.setAttribute("foregroundVolumeSlices", (float)i);
}

model.setAttribute("showRayTracer", true);

sleep(1000);

model.setAttribute("currentSonication", 25);

sleep(2000);

model.setAttribute("showRayTracer",false);
model.setAttribute("showThermometry", true);

sleep(1000);

for (int i=0; i<=600; i+=25) {

    model.setAttribute("foregroundVolumeSlices", (float)i);
}