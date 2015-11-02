# play-framework-nashorn-vs-node-benchmark
Play framework 2.3.x rendering a (VERY) simple ReactJS app on the server using Java8's Nashorn engine vs using Node + JSEngine from Typesafe


### I wanted to compare the following things:
 - Time and memory for processing the whole action(Request => Response)
 - Time and memory for the Engine to render
 - Time and memory to run all the tests
 - How costly is to use a new nashorn engine per request

 
### Machine Info
MacBook Pro (Retina, 15-inch, Late 2013)
2.6 GHz Intel Core i7
16 GB 1600 MHz DDR3

 
### The tests
I ran the following tests:
 - Client: Just to have a general idea of standard Request => Response timing
 - Nashorn new engine: Creating a new instance of nashorn per request
 - Nashorn new factory: Just to see if there was a significant difference with the previous
 - Nashorn same engine: Use the same engine on all requests
 - Node: Using JSE and node to render the same
 
I ran each test certain number of times:
 - Single: single run.
 - Cores: number of cores - 1
 - StressL0: 50 runs, more than this on Nashorn(new engine/factory) takes too long
 - StressL1: 250 runs, JSE+Node starts having issues if more than that
 - StressL2: 1 000 runs
 - StressL3: 10 000 runs, just to see if I could brake Nashorn
 
#### Results

*Client:* Not going to chart this one since is just for reference

Totals:

    Complete test:
        time: 192ms (avg 192ms)
        memory: 82846896 bytes (80905 KB) (79 MB) AVG.: 82846896 bytes (80905 KB) (79 MB)
        
    Request:
        runs: 1000
        time: 187ms (avg 0ms)
        memory: 88580680 bytes (86504 KB) (84 MB) AVG.: 88580 bytes (86 KB) (0 MB)
        success: 1000 (100.0%)
        
        
*Nashorn(using same engine) vs Node + JSE:*

![Rendering time](https://raw.githubusercontent.com/dribba/play-node-benchmark/master/results/raw_rendering.png)

![Memory usage](https://raw.githubusercontent.com/dribba/play-node-benchmark/master/results/memory_usage.png)



#### Conclusions
Even when I had very little expectations for Nashorn, I was quite surprised that, even with 10 000 render requests, it didn't even sweat. What I was also surprised about, was that Node+JSE starts giving up at around 350~ requests(too many files open). Interestingly nashorn can take very long to start a new engine, but reusing the same has Node comparable performance.