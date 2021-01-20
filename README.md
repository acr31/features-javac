# features-javac

A javac plugin for extracting a feature graph for plugging into machine learning models

## Prerequisites

JDK 1.10+

## Download

Latest extrator version built from HEAD: https://storage.googleapis.com/features-javac/features-javac-extractor-latest.jar


## How to extract the graph features

#### Step 1: compile
```
 mvn clean compile package
```

#### Step 2: create example source file
```
 echo "public class T {}" > T.java
```

#### Step 3: to generate .proto files
``` 
 javac -cp extractor/target/features-javac-extractor-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xplugin:FeaturePlugin T.java 
```

#### Step 4: to generate .dot files
```
java -jar dot/target/features-javac-dot-1.0.0-SNAPSHOT-jar-with-dependencies.jar -i T.java.proto -o T.java.dot
```

#### Step 5: to generate a graph image based on .dot file
```
 dot -Tpng T.java.dot > T.java.png
```

### Example Output

```
id: 0
type: AST_ELEMENT
contents: "COMPILATION_UNIT"
startPosition: 0
endPosition: 21
startLineNumber: 1
endLineNumber: 2
,

id: 1
type: FAKE_AST
contents: "TYPE_DECLS"
startPosition: 0
endPosition: 21
startLineNumber: 1
endLineNumber: 2
, 

id: 2
type: AST_ELEMENT
contents: "CLASS"
startPosition: 0
endPosition: 21
startLineNumber: 1
endLineNumber: 2

```

