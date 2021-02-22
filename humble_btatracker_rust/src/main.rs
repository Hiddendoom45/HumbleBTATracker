#![allow(non_snake_case)]

extern crate retry;
extern crate rusqlite;

use scraper::collector::HBStat;
use util::db::HBStatDB;
use util::trend;
mod scraper;
mod util;

use std::env;

fn main() {
    let mut args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        println!("arguments: [url to bundle] [database file name?]");
        return;
    }
    let mut decay = true;
    if args[1].starts_with("nodecay:") {
        decay = false;
        args[1] = args[1]
            .strip_prefix("nodecay:")
            .unwrap_or(args[0].as_str())
            .to_string();
    }
    let dbName = if args.len() < 3 {
        ":mem:".to_string()
    } else {
        args[2].to_string()
    };
    scraper::collector::scrape(args[1].to_string(), dbName, callback, decay);
}

fn callback(stat: HBStat, db: &HBStatDB) {
    println!("\nCurrent Average: {}", stat.average());
    let min = db.minBefore(stat);
    let min15 = db.min15Before(stat);
    let hour = db.hourBefore(stat);
    if min.is_ok() {
        let min = min.unwrap();
        if stat.sold != min.sold {
            println!("Past Minute: {}", trend::trendString(stat, min));
        }
    }
    if min15.is_ok() {
        let min15 = min15.unwrap();
        if stat.sold != min15.sold {
            println!("Past 15 Min: {}", trend::trendString(stat, min15));
        }
    }
    if hour.is_ok() {
        let hour = hour.unwrap();
        if stat.sold != hour.sold {
            println!("Past Hour: {}", trend::trendString(stat, hour));
        }
    }
    //old print
    //println!("Stats {} {} {}", stat.paid, stat.sold,stat.timestamp);
}
