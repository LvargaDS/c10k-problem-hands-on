class: center, middle

# C10k problem from java world perspective

This presentation is intended as light intro into a deep topic with slight (and random) info from history.

Ľubomír Varga from digitalsystems.eu

[show presentation notes](#p1)

???
Just a few words about a topic. When the C10k problem first appeared, it was a matter of a efficiency. 1GB of memory, 500 Mhz single core cpu. Now it is like code kata http://codekata.com problem.

---

# Agenda

1. Introduction to C10k problem
2. A little bit of history
3. Technological approaches (http world)
4. Simulating many devices efficiently

---

# Introduction

* C10K stands for 10_000 concurrent connections.
* Serving many concurrent connections.
* Maintaining latency and
* connections (TCP stack) for each client.

---

# A little bit of history

* the number of Internet users has risen
* cdrom.com (ftp server), 10_000 connections, year 1999
* 2001, epoll gets into kernel 2.6
* WhatsUp, 2_000_000 connections, 2011–2012
* C10M as successor


???
It originated long after cgi scripts served dynamic web pages, and www world has been dominated by LAMP (linux apache mysql php). It was coined after cdrom.com has reached given number of parallel connections on their ftp server. https://en.wikipedia.org/wiki/C10k_problem

epoll approach was one of five possible, and it has been tested in pursuit of more efficient network transfers.

The Next step was WhatsUp, serving 2_000_000 connections in parallel on commodity server using erlang app. Commodity server with 24 cores.

Late C10M was introduced (using java app).

---

# Technological approaches for backends

* Xinetd -> redirect stdin/stdout to TCP/UDP of any program
* the world wide web has been born
* cgi for serving files and also dynamic html pages
  * app generates response
* application servers
  * running app serves response
    * html code hardcoded in-app code (php)
    * code in html (jsp)
    * templating languages (wicket, FreeMarker)
* self-contained backends (embedded undertow, tomcat, ...)
* vm as http server
* lambda functions

???

First of all, backend, for this slide purpose, is any service running on server, which has its main purpose in responding with data to a client.

CGI (common gateway interface), basically parse http headers, and when target url points to "html" file, file is served back. If requested url points to executable (also a bash script is executable), given executable is started and its stdout output is sent back to a client as the html/image/whatever response...
Mostly c/c++ cgi scripts, sometimes perl and other scripting languages.

To lover latency of response, instead of starting a program, we can always have started application server. This way, our business logic does not reside in app which is started with each request again and again, but it is started only once. We save at least the latency of app startup.

Other approaches are from c10k problem point of view mostly identical. One does have fewer problems with kernel tuning, another is more complex to code...

---

# Performance testing, simulating many tcp devices

* this is based on performance test of application
* instead of many clients -> single server, we need
* few servers -> huge number of servers
* simulate many tcp clients (between 100k and one million)
* simple state machine communication protocol
* each client should have its ip address and port
* lets say, we simulate:
  * electronic toothbrushes
  * IP telephones
  * IoT devices like light bulbs, charging stations for electric cars
* we would like to simulate also network properties (latency and packet loss of GPRS)

???
This was my problem to solve, so I will talk about this specific problem and show possible approaches and their efficiencies.

Ip addresses can be shared to save IP addresses. It does not make any difference.

netem - network emulator, part of a linux.

```shell
tc qdisc add dev eth1 root netem loss 1%
tc qdisc add dev br0 root netem delay 10ms 20ms
```

---

# CGI like approach

* cgi-approach project
* Start single java app for single listening port

```java
ServerSocket serverSocket = new ServerSocket(port);
Socket clientSocket = serverSocket.accept();
BufferedReader out = new PrintWriter(clientSocket.getOutputStream(), true);
PrintWriter in = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));
while ((inputLine = in.readLine()) != null) {
    if (inputLine.equals(secretPassword)) {
        out.println("You hit it. All your base belong to us.");
    } else {
        out.println("no");
    }
}
```

* about 260 parallel servers in 1GB of memory
* about 4MB per "server"
* 3.7 TB of memory would be needed for 1M clients

???

Using a simple C application for the same purpose is not significantly better!

---

# Thread-based approach

* threaded-approach
* start single java server app
* start single thread with ~same code for listening

```java
for (int i = 0; i < LISTENERS_COUNT; i++) {
    final Thread t = app.getThreadForPort(i);
    t.setDaemon(true);
    t.start();
}
```

* about 16_500 listeners in single GB of memory
* about 64kB per listener
* about 60BG memory for 1M clients (perhaps feasible)
* how about scheduling such many threads?

???
Have in mind that there is no business logic, aka state machine. This is a minimal server just with single "if".

---

# epoll based approach (java NIO)

* start ServerSocketChannel and register it within Selector
* in endless listen loop, iterate over selector.selectedKeys()

```java
final Selector socketSelector = SelectorProvider.provider().openSelector();
final InetAddress hostAddress =  InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
final int port = 2000;
final InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
final ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false);
serverChannel.socket().bind(isa);
serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
```

```java
while (true) {
  selector.select();
  Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
  while (selectedKeys.hasNext()){
    SelectionKey key = selectedKeys.next();
    selectedKeys.remove();
    process(key);}}
```

* about 169_000 listeners in single GB of memory
* under 7kB per listener
* about 5.9BG memory for 1M clients
* processing done in single thread

---

# Can we do better?

* what is involved in nio (epoll) based approach?
* context switch from userspace to kernel
* `selectedKeys()` -> call to kernel
* `socketChannel.read(buffer)` -> call to kernel
* `socketChannel.write(buffer)` -> call to kernel
* io_uring

---

# io_uring, context switching

.right[![io_uring ring buffers](https://mattermost.com/wp-content/uploads/2020/06/queues-e1619992851556.png)]

???
Image from https://mattermost.com/blog/iouring-and-go/ blog.
