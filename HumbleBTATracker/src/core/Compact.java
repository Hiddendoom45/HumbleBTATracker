package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Class used to compact data gathered from the StatCollector.
 * Works by creating a new temporary table, copying all the necessary rows before
 * using VACUUM to compress the database;
 * @author Allen
 *
 */
public class Compact{
	private Connection conn;
	private ResultSet data;
	private PreparedStatement insert;
	
	//merge mode, does not compact data, keeps all entries
	boolean noCompact = false;
	//last row
	private long timestamp = -1;
	private int sales = 0;
	private double paid = 0;
	
	public Compact(String filename, boolean compact) throws SQLException{
		conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
		conn.createStatement().execute("DROP TABLE IF EXISTS HB_DATA_COPY");
		conn.createStatement().execute("CREATE TABLE HB_DATA_COPY (SALES INT, TIMESTAMP INT PRIMARY KEY, PAID REAL)");
		data = conn.createStatement().executeQuery("SELECT * FROM HB_DATA ORDER BY TIMESTAMP ASC");
		insert = conn.prepareStatement("INSERT INTO HB_DATA_COPY VALUES (?,?,?)");
		this.noCompact = !compact;
	}
	
	public PriceSet next() throws SQLException{
		long first = timestamp;
		long last = first;
		double paid = this.paid;
		int sales = this.sales;
		boolean end = true;
		while(data.next()){
			end = false;
			double p = data.getDouble("PAID");

			if(first==-1){
				paid = p;
				first = data.getLong("TIMESTAMP");
				sales = data.getInt("SALES");
			}
			else if(p!=paid||noCompact){
				timestamp = data.getLong("TIMESTAMP");
				this.sales = data.getInt("SALES");
				this.paid = p;
				break;
			}
			last = data.getLong("TIMESTAMP");
			timestamp = -1;
		}
		if(end) timestamp = -1;
		if(first!=-1){
			return new PriceSet(this, first, last, sales, paid);
		}
		return null;
	}
	
	private void insert(PriceSet values) throws SQLException{
		if(values==null)return;
		insert.setInt(1, values.sales);
		insert.setLong(2, values.first);
		insert.setDouble(3, values.paid);
		insert.execute();
		if(values.first!=values.last){
			insert.setInt(1, values.sales);
			insert.setLong(2, values.last);
			insert.setDouble(3, values.paid);
			insert.execute();
		}
	}
	
	private void finish() throws SQLException{
		conn.createStatement().execute("DROP TABLE HB_DATA");
		conn.createStatement().execute("ALTER TABLE HB_DATA_COPY RENAME TO HB_DATA");
		conn.createStatement().execute("VACUUM");
	}
	private static class PriceSet implements Comparable<PriceSet>{
		Compact source;
		long first;
		long last;
		int sales;
		double paid;
		public PriceSet(Compact source, long first, long last, int sales, double paid){
			this.source = source;
			this.first = first;
			this.last = last;
			this.sales = sales;
			this.paid = paid;
		}
		
		@Override
		public int compareTo(PriceSet other){
			if(other!=null){
				if(other.first<this.first){
					if(other.last<this.first){
						return 1;
					}
					else{
						return 0;
					}
				}
				else if(other.first==this.first){
					return 0;
				}
				else{
					if(this.last<other.first){
						return -1;
					}
					else{
						return 0;
					}
				}
			}
			//null larger
			return -1;
		}
		
		public void merge(PriceSet other){
			if(other.paid==this.paid&&other.sales==this.sales){
				this.first=this.first<other.first?this.first:other.first;
				other.first=this.first;
				this.last=this.last>other.last?this.last:other.last;
				other.last=this.last;
			}
		}
	}
	
	public static void main(String[] args) throws SQLException{
		boolean compact = true;
		if(args[0].equals("merge")){
			compact = false;
			args = Arrays.copyOfRange(args, 1, args.length);
		}
		if(args.length==0){
			System.out.println("Requires at least 1 argument for database in");
		}
		else if(args.length==1){
			Compact c = new Compact(args[0], compact);
			while(true){
				PriceSet ps = c.next();
				if(ps==null)break;
				c.insert(ps);
			}
			c.finish();
		}
		else{
			Compact[] dbs = new Compact[args.length];
			for(int i = 0; i<dbs.length; i++){
				dbs[i] = new Compact(args[i], compact);
			}
			PriorityQueue<PriceSet> queue = new PriorityQueue<PriceSet>();
			for(int i = 0; i<dbs.length; i++){
				PriceSet p = dbs[i].next();
				if(p!=null){
					queue.offer(p);
				}
			}
			while(!queue.isEmpty()){
				PriceSet p = queue.poll();
				Compact c = p.source;
				PriceSet pn = c.next();
				if(pn!=null){
					queue.offer(pn);
				}
				while(p.compareTo(queue.peek())==0){
					PriceSet p2 = queue.poll();
					Compact c2 = p2.source;
					PriceSet p2n = c2.next();
					if(p2n!=null){
						queue.offer(p2n);
					}
					p.merge(p2);
				}
				for(int i = 0; i<dbs.length; i++){
					dbs[i].insert(p);
				}
			}
			for(int i = 0; i<dbs.length; i++){
				dbs[i].finish();
			}
		}
		
	}
}
