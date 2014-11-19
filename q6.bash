#! /bin/bash

javac Q3.java
echo "Running q6..."
java Q3 scrambled.en original.de tParams2.txt qParams2.txt
echo "Done! Output available in unscrambled.en"
