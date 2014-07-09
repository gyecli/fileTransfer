JFLAGS = -g
JC =  javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
		sender.java \
		receiver.java \
		packet.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class
