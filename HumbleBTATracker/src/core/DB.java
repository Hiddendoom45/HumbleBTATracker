package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

public class DB{
	private static Connection conn;
	private static PreparedStatement insert = null;
	private static PreparedStatement get = null;
	private static PreparedStatement recent = null;
	static{
		retry(":memory:");
	}
	public static void retry(String filename){
		try{
			conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
			conn.createStatement().execute("CREATE TABLE IF NOT EXISTS HB_DATA (SALES INT, TIMESTAMP INT PRIMARY KEY, PAID REAL)");
			insert = conn.prepareStatement("INSERT INTO HB_DATA VALUES (?,?,?)");
			get = conn.prepareStatement("SELECT * FROM HB_DATA WHERE TIMESTAMP > ? ORDER BY TIMESTAMP ASC LIMIT 1");
			recent = conn.prepareStatement("SELECT * FROM HB_DATA ORDER BY TIMESTAMP DESC LIMIT 1");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void addStat(HBStat stat){
		try{
			insert.setInt(1, stat.sold);
			insert.setLong(2, stat.timestamp);
			insert.setDouble(3, stat.paid);
			insert.execute();
		}catch(Exception e){}
	}
	public static HBStat mostRecent(){
		try{
			ResultSet res = recent.executeQuery();
			HBStat stat = new HBStat(res.getInt(1),res.getLong(2),res.getDouble(3));
			return stat;
		}catch(Exception e){
			return null;
		}
	}
	public static HBStat minBefore(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.MINUTES.toSeconds(1));
	}
	public static HBStat min15Before(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.MINUTES.toSeconds(15));
	}
	public static HBStat hourBefore(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.HOURS.toSeconds(1));
	}
	private static HBStat oldestSince(long timestamp){
		try{
			get.setLong(1, timestamp);
			ResultSet res = get.executeQuery();
			HBStat stat =  new HBStat(res.getInt(1),res.getLong(2),res.getDouble(3));
			return stat;
		}catch(Exception e){
			return null;
		}
	}
}
