Erlike
======

![version-ALPHA](http://img.shields.io/badge/version-ALPHA-green.svg?style=flat)
![jdk-8](http://img.shields.io/badge/jdk-8-blue.svg?style=flat)
[![license-AGPL3](http://img.shields.io/badge/license-AGPL3-red.svg?style=flat)](https://github.com/axblount/erlike/blob/master/LICENSE)

Experimental Erlang-style concurrency for Java 1.8
--------------------------------------------------

This library isn't doing anything new when it comes to concurrency.
It's mostly just Erlang-like syntax around `java.lang.Thread`. It uses Java 1.8 
lambdas and `@FunctionalInterface`s to make spawning processes and communicating 
between them easy.

```java
import erlike.*; // Node & Pid
import static erlike.Library.*; // self & receive

public class Main {
  public static void main(String[] args) {
    Node test = new Node("test");

    Pid p1 = test.spawn(() -> {
      System.out.println("first process running as " + self());
      receive(msg ->
        System.out.println("I got: " + msg));
    });

    test.spawn(pid -> {
      System.out.println("second process running as " + self());
      Thread.sleep(500);
      pid.send("Hello from " + self());
    }, p1);
  }
}
```

For message passing `erlike` uses the multiple producer, single consumer 
lock-free queue found at [1024cores][1]. You can find my implementation in
[Mailbox][2]. I believe this is the same algorithm that [Akka][3] uses. If you 
notice a problem with my version, please raise an issue!

At the moment, the only external dependencies are JUnit and SLF4J.

Features
--------

* Spawning processes.
* Sending and receiving messages.
* Creating links between processes.

Future Milestones
-----------------

* ~~Links~~ and monitors
* Networking
  * This will be an excellent first opportunity for dogfooding.
* Command-line interface
  * Startup and shutdown
  * Node status
  * Loading jars

[1]:http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue
[2]:https://github.com/axblount/erlike/blob/master/src/lambda/java/erlike/Mailbox.java
[3]:http://akka.io
