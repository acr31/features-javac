/*
 * Copyright © 2018 Andrew Rice (acr31@cam.ac.uk)
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

package uk.ac.cam.acr31.features.javac.testing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

@AutoValue
public abstract class TestCompilation {

  public abstract JCTree.JCCompilationUnit compilationUnit();

  public abstract Context context();

  private static TestCompilation create(JCTree.JCCompilationUnit compilationUnit, Context context) {
    return new AutoValue_TestCompilation(compilationUnit, context);
  }

  public static TestCompilation compile(String fileName, String... source) {
    JavacTool javacTool = JavacTool.create();
    Context context = new Context();
    ImmutableList<JavaFileObject> compilationUnits =
        ImmutableList.of(
            new SimpleJavaFileObject(URI.create(fileName), JavaFileObject.Kind.SOURCE) {
              @Override
              public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return Joiner.on("\n").join(source);
              }
            });
    DiagnosticListener<? super JavaFileObject> diagnosticListener =
        diagnostic -> {
          throw new AssertionError("Compilation failed: " + diagnostic.toString());
        };
    JavacTask task =
        javacTool.getTask(null, null, diagnosticListener, null, null, compilationUnits, context);
    try {
      JCTree.JCCompilationUnit compilationUnit =
          (JCTree.JCCompilationUnit) Iterables.getOnlyElement(task.parse());
      task.analyze();
      return create(compilationUnit, context);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }
}
