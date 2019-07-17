package core;

public class TrendStat{
	public final boolean increasing;
	public final boolean decreasing;
	public final double value;
	public TrendStat(boolean increasing, double value){
		this.increasing = increasing;
		this.decreasing = !increasing;
		this.value = value;
	}
}
