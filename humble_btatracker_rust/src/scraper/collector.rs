//extern crate soup;

use std::thread::sleep;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use rusqlite::Result;
use select::document::Document;

use regex::Regex;

use crate::util::db::HBStatDB;

#[derive(Clone, Copy)]
pub struct HBStat {
    pub paid: f64,
    pub sold: i32,
    pub timestamp: u64,
}

impl HBStat {
    pub fn average(&self) -> f64 {
        self.paid / f64::from(self.sold)
    }
}

#[allow(non_snake_case)]
pub fn scrape(apiURL: String, dbName: String, callback: fn(HBStat, &HBStatDB) -> (), decay: bool) {
    let mut sleepTime = 60;
    let mut t = 0;

    let mut db = HBStatDB::new(&dbName).unwrap();
    loop {
        let stats = retry::retry(
            retry::delay::Exponential::from_millis_with_factor(100, 2.0),
            || fetch_stats(apiURL.as_str()),
        );

        if stats.is_ok() {
            let stats = stats.unwrap();
            match db.addStat(&stats) {
                Ok(_) => (),
                Err(_) => {
                    sleep(Duration::from_secs(60));
                    continue;
                }
            };
            // match conn.execute("INSERT INTO HB_DATA (SALES, TIMESTAMP, PAID) VALUES (?1,?2,?3)", &[&stats.sold.to_string(), &stats.timestamp.to_string(), &stats.paid.to_string()]) {
            //     Ok(_) => (),
            //     Err(_) => {
            //         sleep(Duration::from_secs(60));
            //         continue;
            //     },
            // }
            callback(stats, &db);
        }
        //progressively increase time between scraping, once per min for first hour, then once per 15 min for the next 5
        //before going to once per hour rate.
        if decay {
            if sleepTime == 60 && t == 60 {
                sleepTime = 60 * 15;
                t = 0;
            } else if sleepTime == 60 * 15 && t == 20 {
                sleepTime = 60 * 60;
                t = 0;
            }
            t = t + 1;
        }

        sleep(Duration::from_secs(sleepTime));
    }
}

#[allow(non_snake_case)]
fn fetch_stats(apiURL: &str) -> Result<HBStat, Box<dyn std::error::Error>> {
    let res = reqwest::blocking::get(apiURL)?.text()?;
    let timestamp = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();

    let doc = Document::from(res.as_str());
    let table = doc
        .find(select::predicate::Class("st-numbers-table"))
        .next()
        .ok_or("Unable to find table")?;

    //WTF does children, first child + nth/unwrap all give empty elements
    let mut i = 0;
    let mut paid: f64 = 0.0;
    let mut sold: i32 = 0;

    for ele in Document::from(table.html().as_str()).find(select::predicate::Class("st-td")) {
        if i == 0 {
            paid = Regex::new(r"[^0-9.]")?
                .replace_all(ele.text().as_str(), "")
                .parse()?;
        } else if i == 1 {
            sold = ele.text().replace(",", "").parse()?;
        }
        i += 1;
    }

    Ok(HBStat {
        paid,
        sold,
        timestamp,
    })
}
