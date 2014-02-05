# Graph API for Reactor

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



### What about Streams and Promises?

If you've been using Reactor for building reactive applications, then you're probably familiar with Reactor's `Stream` and `Promise` API. The `Graph` is a complement to these APIs and is not intended to replace them. Instead, it targets a general asynchronous task problem rather than focusing on a specific one like a `Stream` does (namely: how do I process data asynchronously in an efficient, straight-line manner?).

[1] - [http://en.wikipedia.org/wiki/Directed_acyclic_graph](http://en.wikipedia.org/wiki/Directed_acyclic_graph)