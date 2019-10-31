package core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Main{
	
	private static DB db;
	
	private static String trendString(HBStat stat, HBStat old){
		double vel = HBStat.velocity(stat, old, TimeUnit.MINUTES);
		String velocity = "";
		if(vel<1){
			vel = HBStat.velocity(stat, old, TimeUnit.HOURS);
			velocity = String.format("% 9.3f sold/hour", vel);
		}
		else{
			velocity = String.format("% 9.3f sold/min ", vel);
		}
		TrendStat tr = HBStat.trend(stat, old);
		String trend = String.format("%s % 8.6fcents/minute", tr.increasing?"increasing by":"decreasing by", tr.value);
		TrendStat ch = HBStat.toChange(stat, old);
		String change = String.format("%s in %9.3f minutes",ch.increasing?"increases":"decreases", ch.value);
		return velocity+" "+trend+", "+change;
	}
	
	public static void main(String[] args) throws InterruptedException{
		if(args.length<1){
			System.out.println("arguments: [url to bundle] [database file name?]");
		}
		if(args.length>=2){
			db = new DB(args[1]);
		}
		else{
			db = new DB();
		}
		boolean decay = true;
		if(args[0].startsWith("nodecay:")){
			args[0] = args[0].substring("nodecay:".length());
			decay = false;
		}
		URL url = null;
		try{
			url = new URL(args[0]);
		}catch (MalformedURLException e){
			System.err.println("Bad URL, "+args[0]);
			return;
		}
		StatCollector collector = new StatCollector(url.toString(), db, decay);
		collector.setCallback(new Printer());
		Thread t = new Thread(collector);
		t.start();
	}
	
	private static class Printer implements Consumer<HBStat>{
		@Override
		public void accept(HBStat stat){
			System.out.println("\nCurrent Average: "+stat.average());
			HBStat min = db.minBefore(stat);
			HBStat min15 = db.min15Before(stat);
			HBStat hour = db.hourBefore(stat);
			if(stat.sold!=min.sold){
				System.out.println("Past Minute: "+trendString(stat,min));
			}
			if(min15.sold!=min.sold){
				System.out.println("Past 15 Min: "+trendString(stat,min15));
			}
			if(hour.sold!=min15.sold){
				System.out.println("Past Hour  : "+trendString(stat,hour));
			}
		}
		
	}
}
