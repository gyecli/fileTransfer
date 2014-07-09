Editor: Guotian Ye
Userid: gye
ID: 20381209

Date: 2013 March 16

CS246 Assignment 1


Running Instructions:
1. Compile	
	Open a terminal, go to the directory where the files are stored 
	Run "make"

2. Open 3 different UW student-cs-environment machines, go to the files directory.
   (Here use machine linux008, linux024, linux032 to demonstrate)
   Check IP addresses of thress machine by command: ip addr show

   Example Execution: 
   Step 1: start emulator on linux008
	a. change permission: chmod u+x nEmulator-linux386
	b. start emulator: ./nEmulator-linux386 7001 129.97.167.53 7004 7003 129.97.167.54 7002 1 0.2 0

   Step 2: start receiver on linux032:
	a. start receiver command: java receiver 129.97.167.51 7003 7004 outputfile
	b. Wait for sender to send packets
	c. check arrival.log file recording any received packets
	
   Step 3: start sender on linux032:
	a. start sender command: java sender 129.97.167.51 7001 7002 inputfile
	b. check ack.log file and seqnum.log file, recording received ACK packets and andy sended packets respectively.

	


Notes:
1. There are 3 java source files: sender.java, receiver.java, packet.java

1. The timeout delay set up is 100ms.

2. The version of the compilor:
	java version "1.6.0_20"
	OpenJDK Runtime Environment (IcedTea6 1.9.7) (6b20-1.9.7-Oubuntu1~9.10.1)
	OpenJDK 64-Bit Server VM (build 19.0-b09, mixed mode)

3. The version of make:
	GNU Make 3.81
