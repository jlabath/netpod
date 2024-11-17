# netpod

Babashka netpods are programs that can be used as Clojure libraries by [babashka](https://babashka.org/).  
This is the Clojure library that enables their use. 
The idea is the same as babashka pods except the babashka script and child process communicate over unix sockets rather than stdin and stdout.

## Existing netpods

* [MongoDB](https://github.com/jlabath/netpod-mongo)
* [DuckDB](https://github.com/jlabath/netpod-duckdb)

## Motivation

I had a need to talk to MongoDB, and I did not feel like getting familiar with Java ecosystem and rewriting my script as Clojure/JVM app.  
At first I tried writing it as a [babashka pod](https://github.com/babashka/pods) but that version did not mean my requrements in terms of async I/O and concurrency.  
So I took the ideas of babashka pods and modified them slightly for client/server model based on unix sockets.  

## Technical Details

Netpod protocol is the subset of [pods protocol](https://github.com/babashka/pods/blob/master/README.md#the-protocol). Currently only the `describe` and `invoke` operations are supported.  
At present only `json` is supported as an exchange format.
