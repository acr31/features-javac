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

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import hu.kazocsaba.imageviewer.ImageViewer;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.JDialog;
import uk.ac.cam.acr31.features.javac.graph.DotOutput;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;

public class Visualizer {

  public static void show(FeatureGraph featureGraph) {

    String dot = DotOutput.createDot(featureGraph, /* verbose= */ true);

    MutableGraph g;
    try {
      g = Parser.read(dot);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    BufferedImage image = Graphviz.fromGraph(g).height(2000).render(Format.PNG).toImage();

    ImageViewer imageViewer = new ImageViewer(image);

    JDialog frame = new JDialog();
    frame.setModal(true);
    frame.setSize(640, 480);
    frame.add(imageViewer.getComponent(), BorderLayout.CENTER);
    frame.setVisible(true);
  }
}
