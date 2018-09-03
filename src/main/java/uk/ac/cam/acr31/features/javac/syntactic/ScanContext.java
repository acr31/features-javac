package uk.ac.cam.acr31.features.javac.syntactic;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

public class ScanContext {

  public final ClassTree classTree;
  public final MethodTree methodTree;

  public ScanContext(ClassTree classTree, MethodTree methodTree) {
    this.classTree = classTree;
    this.methodTree = methodTree;
  }

  public ScanContext withMethodTree(MethodTree newMethodTree) {
    return new ScanContext(classTree, newMethodTree);
  }

  public ScanContext withClassTree(ClassTree newClassTree) {
    return new ScanContext(newClassTree, methodTree);
  }
}
