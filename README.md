# HumbleBTATracker
Tracks Humble Bundle's Beat the Average prices as it fluctuates
 and gives a summary of the following stats based on changes within the last minute/15 minutes/hour
- Amount sold per minute
- How fast the price is increasing/decreasing in cents/minute
- How long it will take for the BTA price to increase or decrease by 1 cent

This should help with determining whether to buy now or wait for the average to decrease for bundles with Beat the Average tiers.

## Installation

Currently only a java version is available. To use, install the latest version of java 
and download the jar file from the [releases](https://github.com/Hiddendoom45/HumbleBTATracker/releases) page.

I may rewrite this in another language to avoid having to install java.


## Usage

In your terminal type:
```
java -jar BTATrack.jar [url to bundle] [database file name?]
```

You can drag and drop the BTATrack.jar file into the terminal instead of typing out the full path. 
Make sure to quote the bundle url with `""` to avoid additional processes being spawned by `&` characters in the url.

The database file name is optional if you want to keep the data recorded or avoid losing data between program restarts. By default the program uses the in memory sql database. A prefix of membackup: will use the in memory database backing up to the given database file. This is less disk intensive than specifying just the database file at the risk of losing some data if the program stops unexpectedly i.e. power loss.

As the program runs forever to terminate press `CTRL - C` to stop the program

The program will print the information in a format like this
```
Current Average:5.219690360796984
Past Minute:     4.286 sold/min  increasing by  0.003132cents/minute, increases in   169.542 minutes
Past 15Min :     4.805 sold/min  increasing by  0.012320cents/minute, increases in    43.098 minutes
Past Hour  :     5.285 sold/min  increasing by  0.020388cents/minute, increases in    26.043 minutes
```

Initially it will only print the current average with the changed averaged over the past minute/15 minutes/hour added as more 
data is collected


### Dependancies

 - JSoup, to scrape the data from Humble Bundle's api
 - JDBC for sqlite, to easily store and query data collected
