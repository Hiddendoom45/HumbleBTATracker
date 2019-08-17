package core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
	public static void loop(String apiURL) throws InterruptedException{
		Matcher units = Pattern.compile("\"units\": (\\d*)").matcher("");
		Matcher timestamp = Pattern.compile("\"timestamp\": (\\d*)").matcher("");
		Matcher gmv = Pattern.compile("\"gmv\": \"([0-9.]*)\"").matcher("");
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zz");
		while(true){
			try{
				Response res = Jsoup.connect(apiURL).ignoreContentType(true).execute();
				units.reset(res.body());
				timestamp.reset(res.body());
				gmv.reset(res.body());
				System.out.println(res.body());
				if(units.find()&&timestamp.find()&&gmv.find()){
					HBStat stat = new HBStat(
							Integer.parseInt(units.group(1)),
							Long.parseLong(timestamp.group(1)),
							Double.parseDouble(gmv.group(1)));
					db.addStat(stat);
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
				Date expire = format.parse(res.header("expires"));
				TimeUnit.MILLISECONDS.sleep(expire.getTime()-System.currentTimeMillis());
			}catch(Exception e){
				TimeUnit.SECONDS.sleep(3);
			};
			
		}
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
		URL url = null;
		try{
			url = new URL(args[0]);
		}catch (MalformedURLException e){
			System.err.println("Bad URL, "+args[0]);
			return;
		}
		try{
			Document doc = Jsoup.connect(url.toString()).get();
			String bundleJson = doc.getElementById("webpack-bundle-data").toString();
			Matcher m = Pattern.compile("\"hash_for_stats\": \"([^\"]*)\"").matcher(bundleJson);
			Matcher m2 = Pattern.compile("\"static_url\": \"([^\"]*)\"").matcher(bundleJson);
			Matcher m3 = Pattern.compile("\"product_machine_name\": \"([^\"]*)\"").matcher(bundleJson);
			if(m.find()&&m2.find()&&m3.find()){
				loop(m2.group(1)+"/humbler/bundlestats/"+m3.group(1)+"/"+m.group(1));
			}
			else{
				System.out.println("fail");
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		
	}
}
