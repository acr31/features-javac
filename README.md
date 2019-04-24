# features-javac

A javac plugin for extracting a feature graph for plugging into machine learning models

## Prerequisite

JDK 1.10+


## How to extrace the graph features

#### Step1: compile
```
 mvn clean compile package
```

#### Step2: to generate .dot and .proto files
``` 
 javac -cp target/features-javac-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xplugin:FeaturePlugin Source_code 

```

#### Step3: to generate a graph image based on .dot file
```
 dot -Tpng Souce.java.dot > Souce.java.png
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

