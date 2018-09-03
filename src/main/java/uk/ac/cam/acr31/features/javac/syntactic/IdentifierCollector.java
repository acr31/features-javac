package uk.ac.cam.acr31.features.javac.syntactic;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;

class IdentifierCollector extends TreeScanner<Void, Void> {

  List<IdentifierTree> identifiers = new ArrayList<>();

  @Override
  public Void visitIdentifier(IdentifierTree node, Void aVoid) {
    identifiers.add(node);
    return super.visitIdentifier(node, aVoid);
  }
}
