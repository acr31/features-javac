package uk.ac.cam.acr31.features.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

public class FeaturePlugin implements Plugin {


    @Override
    public String getName() {
        return "FeaturePlugin";
    }

    @Override
    public void init(JavacTask task, String... args) {
        System.out.println("Plugin executed");
    }
}