package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupAgent{
	public static BackupAgent instance;
	
	private ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private List<DB> list = Collections.synchronizedList(new ArrayList<DB>());
	
	
	private BackupAgent(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				for(DB db:list){
					db.backup();
				}
			}
		});
		pool.scheduleAtFixedRate(()->{
			for(DB db:list){
				db.backup();
			}
		}, 15, 15, TimeUnit.MINUTES);
	}
	
	public void addDB(DB db){
		list.add(db);
	}
	
	public static BackupAgent getInstance(){
		if(instance==null) instance = new BackupAgent();
		return instance;
	}
}
