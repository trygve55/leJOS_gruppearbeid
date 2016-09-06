import lejos.hardware.motor.*;
import lejos.utility.Delay;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;

public class GolfBaneBil3 {

	static RegulatedMotor leftM = Motor.B;
	static RegulatedMotor rightM = Motor.A;
	final float wheelDiameter = 5.7F;
	final float wheelDistance = 16.0F;
	final boolean reverse = true;

	public static void main(String[] args) throws Exception {

		final float wheelDiameter = 5.7F;
		final float wheelDistance = 16.0F;
		final boolean reverse = true;

		DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, wheelDistance, leftM, rightM, reverse);

		pilot.setLinearAcceleration(1000);
		pilot.setLinearSpeed(30);
		pilot.setAngularSpeed(120);

		pilot.travel(20);
	}
}