package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class DB implements Iterable<HBStat>{
	private Connection conn;
	private PreparedStatement insert = null;
	private PreparedStatement get = null;
	private PreparedStatement recent = null;
	
	private String backupdb = "";
	
	public DB(){
		this(":memory:");
	}
	
	
	public DB(String dbname){
		if(dbname.startsWith("membackup:")){
			backupdb = dbname.substring("membackup:".length());
			BackupAgent.getInstance().addDB(this);
			dbname = ":memory:";
		}
		retry(dbname);
		if(!backupdb.equals("")&&new File(backupdb).exists()){
			try{
				conn.createStatement().execute("restore from "+backupdb);
			}catch(SQLException e){}
		}
	}
	public void retry(String filename){
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
	/**
	 * Iterator on the data in the table, should not be used when data is actively being written to database. 
	 * @return null if something fails. 
	 * @throws SQLException
	 */
	public Iterator<HBStat> iterator(){
		try{
			ResultSet r = conn.createStatement().executeQuery("SELECT * FROM HB_DATA ORDER BY TIMESTAMP ASC");
			return new Iterator<HBStat>(){
				boolean hasNext = r.next();
				@Override
				public boolean hasNext(){
					return hasNext;
				}

				@Override
				public HBStat next(){
					try{
						HBStat stat = new HBStat(r.getInt("SALES"), r.getLong("TIMESTAMP"), r.getDouble("PAID"));
						hasNext = r.next();
						return stat;
					}catch(SQLException e){
						e.printStackTrace();
					}
					return null;
				}
			};
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
	}
	public void addStat(HBStat stat){
		try{
			insert.setInt(1, stat.sold);
			insert.setLong(2, stat.timestamp);
			insert.setDouble(3, stat.paid);
			insert.execute();
		}catch(Exception e){}
	}
	public HBStat mostRecent(){
		try{
			ResultSet res = recent.executeQuery();
			HBStat stat = new HBStat(res.getInt(1),res.getLong(2),res.getDouble(3));
			return stat;
		}catch(Exception e){
			return null;
		}
	}
	public HBStat minBefore(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.MINUTES.toSeconds(1));
	}
	public HBStat min15Before(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.MINUTES.toSeconds(15));
	}
	public HBStat hourBefore(HBStat since){
		return oldestSince(since.timestamp-TimeUnit.HOURS.toSeconds(1));
	}
	private HBStat oldestSince(long timestamp){
		try{
			//connection delay offset
			timestamp-=10;
			get.setLong(1, timestamp);
			ResultSet res = get.executeQuery();
			HBStat stat =  new HBStat(res.getInt(1),res.getLong(2),res.getDouble(3));
			return stat;
		}catch(Exception e){
			return null;
		}
	}
	public void backup(){
		try{
			boolean bakflag = false;
			try{
				Files.copy(new File(backupdb).toPath(), new File(backupdb+".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
				bakflag = true;
			}catch(IOException e){}
			conn.createStatement().execute("backup to "+backupdb);
			if(bakflag){
				Files.delete(new File(backupdb+".bak").toPath());
			}
		}catch(SQLException | IOException e){}
	}
}
