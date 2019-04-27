# Makefile for comp3310 tcp/udp lab

PROGS=server client Server.class Client.class

all: $(PROGS)

%: %.c
	gcc -Wall -o $* $*.c
%.class: %.java
	javac $*.java

clean:
	rm -f $(PROGS) *.class
