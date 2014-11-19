#! /bin/bash

javac Q2.java
echo "Running q5..."
java Q2 corpus.en.gz corpus.de.gz tParams.txt
echo "Done! Output available in First20Alignments2.txt"
