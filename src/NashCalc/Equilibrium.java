

package NashCalc;

import java.util.Arrays;
import java.lang.StringBuilder;

public class Equilibrium{

    private double[] p1Policy;
    private double[] p2Policy;
    private double p1Reward;
    private double p2Reward;

    public Equilibrium(double[] p1Policy, double[] p2Policy,
		       double p1Reward, double p2Reward){
	this.p1Policy = p1Policy;
	this.p2Policy = p2Policy;
	this.p1Reward = p1Reward;
	this.p2Reward = p2Reward;
    }

    public double[] getP1Policy(){
	return this.p1Policy;
    }

    public double[] getP2Policy(){
	return this.p2Policy;
    }

    public double getP1Reward(){
	return this.p1Reward;
    }
    
    public double getP2Reward(){
	return this.p2Reward;
    }

    public String toString(){
	StringBuilder finalString = new StringBuilder();
	finalString.append("Row Player\n");
	finalString.append("==========================\n");
	finalString.append("Reward: " + String.valueOf(p1Reward) + "\n");
	finalString.append("Policy: " + Arrays.toString(p1Policy) + "\n");

	finalString.append("Col Player\n");
	finalString.append("==========================\n");
	finalString.append("Reward: " + String.valueOf(p2Reward) + "\n");
	finalString.append("Policy: " + Arrays.toString(p2Policy) + "\n");
	return finalString.toString();
    }
}
