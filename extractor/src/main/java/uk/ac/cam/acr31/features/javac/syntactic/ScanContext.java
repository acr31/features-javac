/*
 * Copyright © 2018 The Authors (see NOTICE file)
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
