 #!/bin/bash

 javac Q1.java
 echo "Running q4..."
 java Q1 corpus.en.gz corpus.de.gz
 echo "Done! Output available in Top10Translations.txt and First20Alignments.txt"


