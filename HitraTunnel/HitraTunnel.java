import lejos.hardware.motor.*;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.port.Port;
import lejos.hardware.ev3.EV3;
import lejos.hardware.Keys;
import lejos.hardware.lcd.*;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.*;
import lejos.utility.Delay;

class Clean {

	boolean isUp = false;
	RegulatedMotor cleanM = Motor.D;
	RegulatedMotor posM = Motor.C;

	public Clean() {
		//Brick brick = BrickFinder.getDefault();
		//Port m1 = brick.getPort("MD");
		//Port m2 = brick.getPort("M1");
	}

	public boolean isUp() {
		return isUp;
	}

	public void setPos(boolean b) { //up = true, down = false
		if (b && isUp == false) {
			posM.rotate(-450);
			isUp = true;
		} else if (isUp) {
			posM.rotate(450);
			isUp = false;
		}

	}

	public void startClean() {
		cleanM.forward();
	}

	public void stopClean() {
		cleanM.stop();
	}
}

class HitraTunnel {

	static RegulatedMotor leftM = Motor.A;
	static RegulatedMotor rightM = Motor.B;

	public static void main(String[] args) {

		Clean cleaner = new Clean();

		final float wheelDiameter = 5.5F;
		final float wheelDistance = 10.8F;
		final boolean reverse = false;

		DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, wheelDistance, leftM, rightM, reverse);

		pilot.setLinearAcceleration(500);
		pilot.setLinearSpeed(5);
		pilot.setAngularSpeed(60);


		while (true) {
			break;
		}

			for (int i = 1; i < 5; i++) {
					cleaner.startClean();
					pilot.travel(10);
					cleaner.stopClean();
					pilot.travel(-10);
					cleaner.setPos(true);
					cleaner.startClean();
					pilot.travel(10);
					cleaner.stopClean();
					cleaner.setPos(false);

				}

			pilot.rotate(-90);
			pilot.travel(15);
			pilot.rotate(-90);

			cleaner.setPos(true);
			cleaner.startClean();
			pilot.travel(20);
			cleaner.startClean();


			cleaner.setPos(false);


	}

}