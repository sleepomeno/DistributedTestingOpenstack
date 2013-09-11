# Distributed Testing on OpenStack

This project has been created for a computer science course at the technical university of Vienna. It will not be continued.

## Goal
The scenario: Let local Java tests testing JavaScript resources be run on OpenStack platform for scaling support. Assuming time-consuming tests or high test loads the scaling boosts performance.

## Requirements
- The quota needs to allow to create at least 7 hosts (already running hosts are not respected by the implementation)
- The openstack properties in *Server/src/main/resources/openstack.properties* are valid
- The ports 9874, 9875, 9876 and 9877 are free.

## How to start it

### Start the Server Processes

Run this on a machine of your OpenStack platform:

1. In */*: _mvn clean install -Dmaven.test.skip=true_
2. In */Server*:  _mvn exec:java -Dexec.mainClass="org.mozartspaces.core.SpaceServer"_ (starts the Space server)
3. In */Server*: _mvn exec:java -Dexec.mainClass="at.ac.tuwien.infosys.praktikum.Master"_ (starts the Master process)

### Demo test

For testing there are 2 ways:

4A) In */Tester*: _mvn test_

   This runs the test suites in the normal maven test phase. Doesn't take too long.

OR

4B) In */Tester*: _mvn exec:java -Dexec.mainClass="at.ac.tuwien.infosys.praktikum.Simulation"_
      
   This runs a simulation over a longer period of time with a little bit of waiting between the test suite requests.

##  Statistics
You get the test results directly in the output of the 4A or 4B process.
To get the specific hosts' and suites' information you have to type in the command "suites" or "hosts" in the console of the
Master process (+ Enter). This writes the information to the files Server/hosts and Server/suites.
