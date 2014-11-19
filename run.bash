#!/bin/bash
mkdir output

echo "running question4."
date
./q4.bash
scp Top10Translations.txt output/
scp First20Alignments1.txt output/
echo '\n'
echo "running question5."
date
./q5.bash
scp First20Alignments2.txt output/
echo '\n'
echo "running question6."
date
./q6.bash
scp unscrambled.en output/
echo "finished."
