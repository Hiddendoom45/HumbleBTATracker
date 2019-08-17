package core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

/**
 * Class to use to simply collect bundle stats
 * @author Allen
 *
 */
public class StatCollector implements Runnable{
	private DB db;
	private String apiURL;
	
	//change to false to stop loop
	public volatile boolean run = true;
	
	//api url and database to put data into
	public StatCollector(String apiURL, DB db){
		this.apiURL = apiURL;
		this.db = db;
	}
	
	//loop to run within executor or something else
	@Override
	public void run(){
		Matcher units = Pattern.compile("\"units\": (\\d*)").matcher("");
		Matcher timestamp = Pattern.compile("\"timestamp\": (\\d*)").matcher("");
		Matcher gmv = Pattern.compile("\"gmv\": \"([0-9.]*)\"").matcher("");
		SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss zz");
		while(run){
			try{
				Response res = Jsoup.connect(apiURL).ignoreContentType(true).execute();
				units.reset(res.body());
				timestamp.reset(res.body());
				gmv.reset(res.body());
				if(units.find()&&timestamp.find()&&gmv.find()){
					HBStat stat = new HBStat(
							Integer.parseInt(units.group(1)),
							Long.parseLong(timestamp.group(1)),
							Double.parseDouble(gmv.group(1)));
					db.addStat(stat);
				}
				Date expire = format.parse(res.header("expires"));
				TimeUnit.MILLISECONDS.sleep(expire.getTime()-System.currentTimeMillis());
			}catch(InterruptedException e){
				break;
			}catch(Exception e){
				try{
					TimeUnit.SECONDS.sleep(3);
				}catch(InterruptedException e1){
					break;
				}
			};
			
		}
	}

}
