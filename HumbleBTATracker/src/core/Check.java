package core;

/**
 * Class to check various properties of databases
 * @author Allen
 *
 */
public class Check{

	public static void main(String[] args){
		if(args.length<1){
			System.out.println("Requires database argument");
			return;
		}
		DB db = new DB(args[0]);
		HBStat last = new HBStat(Integer.MIN_VALUE,Long.MIN_VALUE,Double.MIN_VALUE);
		boolean iter = false;
		int err = 0;
		for(HBStat stat:db){
			iter = true;
			if(stat.paid<last.paid||stat.sold<last.sold){
				err++;
				System.out.println("Violation of monotonicity property at timestamp "+stat.timestamp+", previous timestamp"+last.timestamp);
				if(stat.paid<last.paid){
					System.out.println("Amount paid "+stat.paid+"<"+last.paid);
				}
				if(stat.sold<last.sold){
					System.out.println("Amount sold "+stat.sold+"<"+last.sold);
				}
			}
			last = stat;
		}
		if(iter&&err==0){
			System.out.println(args[0]+" passed all checks");
		}
		else if(!iter){
			System.out.println(args[0]+" does not have any data, the database may be corrupted");
		}
		else{
			System.out.println(args[0]+" errors: "+err);
		}
		

	}

}
