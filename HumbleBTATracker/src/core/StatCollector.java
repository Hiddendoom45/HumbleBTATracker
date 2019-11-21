package core;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Connection.Response;

/**
 * Class to use to simply collect bundle stats
 * @author Allen
 *
 */
public class StatCollector implements Runnable{
	private DB db;
	private String apiURL;
	
	private Consumer<HBStat> callback;
	
	private boolean decay;
	//change to false to stop loop
	public volatile boolean run = true;
	
	private volatile boolean running = false;
	
	//api url and database to put data into
	public StatCollector(String apiURL, DB db, boolean decay){
		this.apiURL = apiURL;
		this.db = db;
		this.decay = decay;
	}
	
	public boolean setCallback(Consumer<HBStat> callback){
		if(running) return false;
		this.callback = callback;
		return true;
	}
	
	//loop to run within executor or something else
	@Override
	public void run(){
		int minWait = 1;
		int t = 0;
		running = true;
		while(run){
			try{
				Response res = Jsoup.connect(apiURL).execute();
				long timestamp = System.currentTimeMillis()/1000;
				Document doc = res.parse();
				if(doc.getElementsByClass("st-numbers-table").size()==0){
					System.out.println("Bundle ended: "+apiURL);
					break;
				}
				Element table = doc.getElementsByClass("st-numbers-table").get(0);
				double paid = Double.parseDouble(table.child(0).child(0).child(1).text().replaceAll("[$,]", ""));
				int sold = Integer.parseInt(table.child(0).child(1).child(1).text().replaceAll("[,]", ""));
				HBStat stat = new HBStat(sold,timestamp, paid);
				db.addStat(stat);
				if(callback!=null)callback.accept(stat);
				TimeUnit.MINUTES.sleep(minWait);
				if(decay){
					t++;
					if(minWait==1&&t==60){
						minWait = 15;
						t = 0;
					}
					else if(minWait==15&&t==20){
						minWait = 60;
						t = 0;
					}
					
				}
			}catch(InterruptedException e){
				break;
			}catch(NumberFormatException | IndexOutOfBoundsException e){
				e.printStackTrace();
				System.out.println("Failure parsing page data "+apiURL);
				break;
			}catch(Exception e){
				try{
					TimeUnit.MINUTES.sleep(1);
				}catch(InterruptedException e1){
					break;
				}
			}
			
		}
	}

}
