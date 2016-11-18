Erlike
======

![version-ALPHA](http://img.shields.io/badge/version-ALPHA-green.svg?style=flat)
![jdk-8](http://img.shields.io/badge/jdk-8-blue.svg?style=flat)
[![license-AGPL3](http://img.shields.io/badge/license-AGPL3-red.svg?style=flat)](https://github.com/axblount/erlike/blob/master/LICENSE)

Experimental Erlang-style concurrency for Java
----------------------------------------------

Erlike is a framework for spawning threads and communicating between them. It 
uses Java 1.8 lambda expressions and `@FunctionalInterface`s to make life easier. The 
syntax and mechanics are inspired by [Erlang](https://www.erlang.org/).

```java
import erlike.*; // Node & ProcId
import static erlike.Library.*; // self & receive

public class Main {
  public static void main(String[] args) {
    Node node = new Node("test");

    ProcId first = test.spawn(() -> {
      System.out.println("first process running as " + self());
      receive(msg ->
        System.out.println("I got: " + msg));
    });

    node.spawn(pid -> {
      System.out.println("second process running as " + self());
      Thread.sleep(500);
      pid.send("Hello from " + self());
    }, first);
  }
}
```

Features
--------

Erlike is still in the early alpha stage. The API is likely to change 
dramatically from commit to commit.

### Current

* Spawning processes.
* Sending and receiving messages.
* Creating links between processes.

### Future Milestones

* Monitors
* Networking
  - This will be an excellent first opportunity for dogfooding.
* Command-line interface
  - Startup and shutdown
  - Node status
  - Loading jars
  
Implementation
--------------

For message passing Erlike uses the multiple producer, single consumer lock-free 
queue found at [1024cores][1024cores]. You can find my implementation in
[Mailbox.java][mailbox]. This is the same algorithm that [Akka](http://akka.io) uses, 
though I did not use their implementation in any way. If you notice a problem 
with my version, please raise an issue!

Erlike requires JDK 1.8. The only external dependencies are JUnit and 
[SLF4J](http://slf4j.org).

[erlang]: https://www.erlang.org/
[1024cores]: http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue
[mailbox]: https://github.com/axblount/erlike/blob/master/src/lambda/java/erlike/Mailbox.java
