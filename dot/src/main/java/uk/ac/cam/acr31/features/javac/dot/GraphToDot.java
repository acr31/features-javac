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

package uk.ac.cam.acr31.features.javac.dot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;

public class GraphToDot {

  /** Entry point for converting protos to dot files from the command line. */
  public static void main(String[] args) throws IOException, ParseException {
    Options option = new Options();
    option.addOption("i", "input-file", true, "Input filename");
    option.addOption("o", "output-file", true, "Output filename");
    option.addOption("v", "verbose-dot", false, "Verbose dot output");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(option, args);

    try (FileInputStream fis = new FileInputStream(cmd.getOptionValue("input-file"))) {
      DotOutput.writeToDot(
          new File(cmd.getOptionValue("output-file")),
          GraphProtos.Graph.parseFrom(fis),
          cmd.hasOption("verbose-dot"));
    }
  }
}
