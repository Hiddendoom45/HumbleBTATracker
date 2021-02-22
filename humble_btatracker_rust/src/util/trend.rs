use crate::scraper::collector::HBStat;

use std::time::Duration;

fn velocity(a: HBStat, b: HBStat, scale: Duration) -> f64 {
    let (o, n) = match a.timestamp > b.timestamp {
        true => (b, a),
        false => (a, b),
    };
    f64::from(n.sold - o.sold) * scale.as_secs_f64() / (n.timestamp - o.timestamp) as f64
}

fn trend(a: HBStat, b: HBStat) -> (bool, f64) {
    let (o, n) = match a.timestamp > b.timestamp {
        true => (b, a),
        false => (a, b),
    };
    if o.average() > n.average() {
        (
            false,
            (o.average() - n.average()) * 6000.0 / (n.timestamp - o.timestamp) as f64,
        )
    } else {
        (
            true,
            (n.average() - o.average()) * 6000.0 / (n.timestamp - o.timestamp) as f64,
        )
    }
}

fn toChange(a: HBStat, b: HBStat) -> (bool, f64) {
    let (o, n) = match a.timestamp > b.timestamp {
        true => (b, a),
        false => (a, b),
    };
    if o.average() > n.average() {
        let nextD = n.average() * 100.0 / 100.0 - 0.005;
        (
            false,
            (n.average() - nextD)
                / ((o.average() - n.average()) * 60.0 / (n.timestamp - o.timestamp) as f64),
        )
    } else {
        let nextI = n.average() * 100.0 / 100.0 + 0.005;
        (
            true,
            nextI
                - n.average()
                    / ((n.average() - o.average()) * 60.0 / (n.timestamp - o.timestamp) as f64),
        )
    }
}

pub fn trendString(old: HBStat, new: HBStat) -> String {
    let vel: f64 = velocity(old, new, Duration::from_secs(60));
    let velocity: String = if vel < 1.0 {
        let vel = velocity(old, new, Duration::from_secs(60 * 60));
        format!("{:>9.3} sold/hour ", vel)
    } else {
        format!("{:>9.3} sold/min ", vel)
    };
    let (tr, trValue) = trend(new, old);
    let trDir = if tr { "increasing by" } else { "decreasing by" };
    let trend: String = format!("{} {:>8.6}cents/minute, ", trDir, trValue);
    let (ch, chValue) = toChange(new, old);
    let chDir = if ch { "increases" } else { "decreases" };
    let change: String = format!("{} {:>9.3} minutes", chDir, chValue);
    velocity + &trend + &change
}
