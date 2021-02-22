use rusqlite::Connection;

use rusqlite::{params, NO_PARAMS};

use std::path::Path;
use std::time::SystemTime;

use crate::scraper::collector::HBStat;

pub struct HBStatDB {
    conn: Connection,
    backupdb: String,
    lastbackup: SystemTime,
}

impl HBStatDB {
    pub fn new(name: &String) -> Result<Self, rusqlite::Error> {
        let mut membackup = false;
        let mut dbname: String = name.clone();
        let mut restorename: String = "".to_string();
        if dbname.starts_with("membackup:") {
            membackup = true;
            restorename = dbname
                .strip_prefix("membackup:")
                .unwrap_or_default()
                .to_string();
            dbname = ":mem:".to_string();
        }

        let mut conn = if dbname != ":mem:".to_string() {
            Connection::open(dbname)?
        } else {
            Connection::open_in_memory()?
        };

        if membackup {
            //ignore as it may fail if the database has not been saved before
            //Also use of Some |_|() as compiler is not able to infer type of F
            let _ = conn.restore(
                rusqlite::DatabaseName::Main,
                Path::new(&restorename),
                Some(|_| ()),
            );
        }
        //init db
        match conn.execute(
            "CREATE TABLE IF NOT EXISTS HB_DATA (SALES INT, TIMESTAMP UNSIGNED BIG INT PRIMARY KEY, PAID REAL)",
            NO_PARAMS,
        ) {
            Err(err) => return Err(err),
            _ => (),
        }

        Ok(Self {
            conn: conn,
            backupdb: restorename,
            lastbackup: SystemTime::now(),
        })
    }

    pub fn addStat(&mut self, stat: &HBStat) -> Result<(), rusqlite::Error> {
        let res = match self.conn.execute(
            "INSERT INTO HB_DATA VALUES (?1,?2,?3)",
            &[
                &stat.sold.to_string(),
                &stat.timestamp.to_string(),
                &stat.paid.to_string(),
            ],
        ) {
            Err(err) => Err(err),
            _ => Ok(()),
        };
        if self.backupdb != "".to_string() {
            //check if enough time has elapsed between backups
            match self.lastbackup.elapsed() {
                Ok(dur) => {
                    if dur.as_secs() > 60 * 15 {
                        self.backup();
                        self.lastbackup = SystemTime::now();
                    }
                }
                Err(_) => (),
            }
        }
        res
    }

    #[allow(dead_code)]
    pub fn mostRecent(&self) -> Result<HBStat, rusqlite::Error> {
        let mut stmt = self
            .conn
            .prepare("SELECT * FROM HB_DATA ORDER BY TIMESTAMP DESC LIMIT 1")?;
        let mut res = stmt.query(NO_PARAMS)?;
        let row = res.next()?.expect("Empty database");

        Ok(HBStat {
            paid: row.get("PAID")?,
            sold: row.get("SOLD")?,
            timestamp: {
                let r: i64 = row.get("TIMESTAMP")?;
                r as u64
            },
        })
    }
    pub fn minBefore(&self, since: HBStat) -> Result<HBStat, rusqlite::Error> {
        self.oldestSince(since.timestamp - 60)
    }
    pub fn min15Before(&self, since: HBStat) -> Result<HBStat, rusqlite::Error> {
        self.oldestSince(since.timestamp - (15 * 60))
    }
    pub fn hourBefore(&self, since: HBStat) -> Result<HBStat, rusqlite::Error> {
        self.oldestSince(since.timestamp - (60 * 60))
    }
    pub fn oldestSince(&self, timestamp: u64) -> Result<HBStat, rusqlite::Error> {
        let mut stmt = self
            .conn
            .prepare("SELECT * FROM HB_DATA WHERE TIMESTAMP > ?1 ORDER BY TIMESTAMP ASC LIMIT 1")?;
        let mut res = stmt.query(params![(timestamp - 10).to_string()])?;
        let row = res.next()?.ok_or(rusqlite::Error::QueryReturnedNoRows)?;

        Ok(HBStat {
            paid: row.get("PAID")?,
            sold: row.get("SALES")?,
            timestamp: {
                let r: i64 = row.get("TIMESTAMP")?;
                r as u64
            },
        })
    }


    fn backup(&self) {
        let bakflag = match std::fs::copy(
            Path::new(&self.backupdb),
            Path::new(&(self.backupdb.clone() + ".bak")),
        ) {
            Ok(_) => true,
            _ => false,
        };

        if self
            .conn
            .backup(
                rusqlite::DatabaseName::Main,
                Path::new(&self.backupdb),
                None,
            )
            .is_err()
        {
            //quick return if it fails
            return ();
        }
        //allow errors as it doesn't matter if it succeeded or not, just a cleanup operation
        #[allow(unused_must_use)]
        if bakflag {
            std::fs::remove_file(std::path::Path::new(&&(self.backupdb.clone() + ".bak")));
        }
    }
}
