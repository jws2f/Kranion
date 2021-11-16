model.setAttribute("foregroundVolumeSlices", 600f);

model.setAttribute("showRayTracer", false);

for (int i=600; i>=-400; i-=2) {

    model.setAttribute("foregroundVolumeSlices", (float)i);
    view.setIsDirty(true);
}

for (int i=-400; i<=600; i+=2) {

    model.setAttribute("foregroundVolumeSlices", (float)i);
    view.setIsDirty(true);
}