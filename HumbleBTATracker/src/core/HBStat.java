package core;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class HBStat{
	public final int sold;
	public final long timestamp;
	public final double paid;
	public HBStat(int sold, long timestamp,  double paid){
		this.sold = sold;
		this.timestamp = timestamp;
		this.paid = paid;
	}
	public double average(){
		///System.out.println("paid"+paid+" sold"+sold);
		return paid/sold;
	}
	
	public static double velocity(HBStat a, HBStat b, TimeUnit scale){
		HBStat o = a.timestamp>b.timestamp?b:a;
		HBStat n = a.timestamp>b.timestamp?a:b;
		return (double)(n.sold-o.sold)*scale.toSeconds(1)/(n.timestamp-o.timestamp);
	}
	public static TrendStat trend(HBStat a, HBStat b){
		HBStat o = a.timestamp>b.timestamp?b:a;
		HBStat n = a.timestamp>b.timestamp?a:b;
		if(o.average()>n.average()){
			return new TrendStat(false, (o.average()-n.average())*6000/(n.timestamp-o.timestamp));
		}
		else{
			return new TrendStat(true, (n.average()-o.average())*6000/(n.timestamp-o.timestamp));
		}
	}
	public static TrendStat toChange(HBStat a, HBStat b){
		HBStat o = a.timestamp>b.timestamp?b:a;
		HBStat n = a.timestamp>b.timestamp?a:b;
		if(o.average()>n.average()){
			return new TrendStat(false, ((n.average()-nextDecrease(n.average()))/((o.average()-n.average())*60/(n.timestamp-o.timestamp))));
		}
		else{
			return new TrendStat(true, ((nextIncrease(n.average())-n.average())/((n.average()-o.average())*60/(n.timestamp-o.timestamp))));
		}
	}
	private static double nextIncrease(double currentAvg){
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return Double.parseDouble(df.format(currentAvg))+0.005;
		
	}
	public static double nextDecrease(double currentAvg){
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return Double.parseDouble(df.format(currentAvg))-0.005;
	}
	public String toString(){
		return "s:"+sold+" t:"+timestamp+" p:"+paid;
	}
	
}
