#!/bin/bash
CLASSPATH="./bin/:$(find "./lib" -name '*.jar' | tr "\\n" ":")"

# Run all peers
for user in "Jose" "Joao" "Pedro" "Maria" "Cristina"; do
  echo "Starting peer ${user} ..."
  nohup java -classpath $CLASSPATH net.pgp2p.test.${user}Peer \
    2> ./log/${user}.log & 
done

echo ""
echo "Press any key to kill all peers."
read

echo "Killing all peers ..."
for process in $(ps ax | grep pgp2p | cut -d" " -f 1); do 
  kill $process 2> /dev/null; 
done

echo "Done!"
