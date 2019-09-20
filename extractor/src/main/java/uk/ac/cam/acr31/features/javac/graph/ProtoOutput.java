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

package uk.ac.cam.acr31.features.javac.graph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Static methods for writing proto files. */
public class ProtoOutput {

  /** Write this feature graph to the given output file. */
  public static void write(File outputFile, FeatureGraph featureGraph) {
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      featureGraph.toProtobuf().writeTo(fos);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write protobuf", e);
    }
  }
}
