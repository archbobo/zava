Reactive Streams in Java
====

https://github.com/jhusain/learnrxjava/

A sequential program runs on a flat timeline.  Each task is only started after the previous one completes. In concurrent programs, multiple tasks may be running during the same time period and a new task may begin at any time.
 
In threaded programs, introducing concurrency trades space for time. Allocating memory for more threads allows application servers to make network requests concurrently instead of sequentially. Threaded network requests can dramatically reduce server response times, but like all trade-offs this approach has its limits. Unchecked thread creation can cause a server to run out of memory or to spend too much time to context switching. Thread pools can help manage these problems, but under heavy load the number of threads in an application server’s pool will eventually be exhausted.  When this happens network requests will be serialized, causing response times to rise. At this point the only way to bring down response times again is to scale up more servers, which increases costs.
 
Reactive programming allows concurrent network requests to be made without threads. Instead of creating a thread which immediately blocks on IO, a callback is asynchronously invoked when data is received from the stream. This dramatically increases the number of open connections an application server can manage at any time. Furthermore it allows application servers to better tolerate long-running connections, either due to a failure in a downstream service, or the use of a persistent connection protocol such as web sockets.
  
Concurrent programming is inherently more complicated than sequential programming, because concurrent programs force us to think multi-dimensionally. At first, concurrency may seem overwhelming. How to produce clear, concise, and correct code in the face of all of this additional complexity? 

Reactive programming adds additional complications. Reactive APIs hold onto references to our objects through our callbacks. In the event of an error we must ensure that these Reactive APIs free their references to our callbacks so that they can be garbage collected, as well as cancel any ongoing tasks.

The good news is that in practice, managing concurrency with reactive programming is not as complicated as it appears. In fact most concurrency and parallel problems can be solved with a few simple functions. First you will learn how to use these functions to transform I datatype that you are already comfortable with: the Java List. Then we will learn how you can apply the same functions to streams of data arriving over time.
