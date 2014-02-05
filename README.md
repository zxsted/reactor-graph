# Graph API for Reactor

_NOTE:_ This code is *experimental*. Please file issues in this repo, ask questions on the [Reactor mailing list](https://groups.google.com/forum/#!forum/reactor-framework), or post questions on StackOverflow with the tag `#projectreactor` to discuss this API. Don't consider anything you see here final or production-ready! You've been warned. :)

Reactor's Graph API is inspired by the Directed Acyclic Graph [1] concept and provides a concise way to wire artibrary actions together based on different routing predicates based on custom logic or the presence of a specific type of Exception.

### How to use the Graph API

Graphs are created ahead of time and data is fed into them by calling the `Graph.accept(T)` method. Whatever `Node` that has been set as the `startNode` will be notified of this new value. If no `startNode` has been explicitly set and there is only one registered `Node` in the `Graph`, then that `Node` will be considered the `startNode`.

Graphs are a set set of connected `Nodes`. The connections between Nodes are called `Routes`. Either Routes or Nodes can have actions assigned to them, depending on what flow you're trying to achieve.

### Create a Graph

To create a Graph, just use the static factory method and pass your `Environment`. It will use the default `Dispatcher` for the `Environment` (with default configuration that will be the `RingBufferDispatcher`).

```java
// Create a Graph that will accept Strings
Graph<String> graph = Graph.create(env);
```

Since Graphs are made up of Nodes, you need a `Node` to do any work. Create one from by calling the `Graph.node()` method (or one of its variants). The zero-arg version will return an "anonymous" `Node` which just means it uses a UUID to ensure a unique name. The other versions allow you to specify the name of the `Node` you're creating and optionally allow you to specify a `Dispatcher` other than the default if you want events occurring inside the `Node` to be in different threads than the initial, incoming events that enter the Graph at the "top".

```java
Node<String> countNode = graph.node("counter")
                              .consume(new Consumer<String>() {
                                public void accept(String s) {
                                  atomicCounter.incrementAndGet();
                                }
                              });
```

Since this is the only `Node` currently created inside this `Graph`, sending a value into the `Graph` at this point would cause the "counter" `Node` to receive the value and then be dropped.

If we want to start with a different `Node` and then route only certain elements to this counter, then we'd create another `Node` and use a `Predicate` to route the value to this `Node`.

```java
Node<String> startNode = graph.node("start")
                              .when(new Predicate<String>() {
                                public boolean test(String s) {
                                  return s.startsWith(PACKET_PREFIX);
                                }
                              })
                              .routeTo("counter");

// Tells the Graph to use 'start' as a startNode
graph.startNode("start");
```

A `Predicate` is basically a filter, something you'll be familiar with if you're using Reactor's `Stream` API. But Graphs are unique in that besides placing actions inline (after the `Predicate` definition) to process values that pass the test, Graphs can also route values to arbitrary Nodes. It's similar to a GOTO in the Basic programming language.

### Examples

Check out the `graph-examples` submodule for examples of how to wire Nodes together and perform complex processing.

### What about Streams and Promises?

If you've been using Reactor for building reactive applications, then you're probably familiar with Reactor's `Stream` and `Promise` API. The `Graph` is a complement to these APIs and is not intended to replace them. Instead, it targets a general asynchronous task problem rather than focusing on a specific one like a `Stream` does (namely: how do I process data asynchronously in an efficient, straight-line manner?).

[1] - [http://en.wikipedia.org/wiki/Directed_acyclic_graph](http://en.wikipedia.org/wiki/Directed_acyclic_graph)