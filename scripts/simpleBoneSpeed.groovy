float waterSpeed = 1482f;

raytracer.setBoneRefractionSpeed(waterSpeed);
raytracer.setBoneSpeed(waterSpeed);

//model.setAttribute("boneRefractionSOS", 1482f);

//raytracer.doCalc();

raytracer.setIsDirty(true);
view.setIsDirty(true);
