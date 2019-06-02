/*
 * Copyright © 2019 Henry Mercer (henry.mercer@me.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.acr31.features.javac.semantic;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class TypeAnalysis {

  private final CompilationUnitTree compilationUnitTree;
  private final ProcessingEnvironment processingEnvironment;

  public TypeAnalysis(
      CompilationUnitTree compilationUnitTree, ProcessingEnvironment processingEnvironment) {
    this.compilationUnitTree = compilationUnitTree;
    this.processingEnvironment = processingEnvironment;
  }

  public TypeMirror getTypeMirror(Tree typeDecl) {
    Trees trees = Trees.instance(processingEnvironment);
    TreePath path = TreePath.getPath(compilationUnitTree, typeDecl);
    return trees.getTypeMirror(path);
  }

  private boolean canCheckAssignability(TypeMirror type) {
    return type.getKind() != TypeKind.EXECUTABLE && type.getKind() != TypeKind.PACKAGE;
  }

  /** Returns true if typeA is assignable to typeB. */
  public boolean isAssignable(TypeMirror typeA, TypeMirror typeB) {
    if (!canCheckAssignability(typeA) || !canCheckAssignability(typeB)) {
      return false;
    }

    return processingEnvironment.getTypeUtils().isAssignable(typeA, typeB);
  }

  public String getApproxTypeName(Tree tree) {
    return getTypeMirror(tree).toString();
  }

  public Types getTypes() {
    return processingEnvironment.getTypeUtils();
  }
}
