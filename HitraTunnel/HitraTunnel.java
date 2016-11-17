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
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
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
		cleanM.setSpeed(900);
		cleanM.forward();
	}

	public void stopClean() {
		cleanM.stop();
	}
}

public class HitraTunnel {

	class Control {
		public volatile int stop = 0;
		public volatile boolean snu = false;
		public volatile boolean exit = false;
	}

	final Control control = new Control();

	class ThreadClean implements Runnable {
		public void run() {

			Clean cleaner = new Clean();

			final float wheelDiameter = 5.5F;
			final float wheelDistance = 10.8F;
			final boolean reverse = false;

			DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, wheelDistance, leftM, rightM, reverse);

			pilot.setLinearAcceleration(50);
			pilot.setLinearSpeed(8);
			pilot.setAngularSpeed(60);

			int steps = 600;
			int fram = 0;
			int svart = 0;
			int speed = 0;
			int targetSpeed = 100;

			leftM.setSpeed(100);
			rightM.setSpeed(99);

			for (int i = 0;i < 1000 ;i++ ) {
				Delay.msDelay(1);
			}

			while (!control.exit) {
				if (control.stop > 0) {
					pilot.stop();
					cleaner.stopClean();
					control.stop--;
				} else if (control.snu && control.stop == 0) {
					cleaner.stopClean();
					cleaner.setPos(false);
					pilot.travel(-5);
					pilot.rotate(-180);
					pilot.travel(10);

					steps = 600;
					fram = 0;
					svart++;
					if (svart == 2) {
						control.exit = true;
					}
					control.snu = false;
				} else {
					/*if (speed < targetSpeed) {
						speed += 5;
						leftM.setSpeed(speed);
						rightM.setSpeed(speed);
					}*/


					if (fram != 1 && steps != 0) {
					rightM.forward();
					leftM.forward();
					cleaner.startClean();
					} else if (steps != 0) {
						rightM.backward();
						leftM.backward();
					}
					if (steps == 0) {
						if (fram == 0) {
							fram = 1;
							pilot.stop();
							//cleaner.setPos(false);
							cleaner.stopClean();
							steps = 600;
							//speed = 0;

						} else if (fram == 1)  {
							fram = 2;
							pilot.stop();
							cleaner.setPos(true);
							cleaner.startClean();
							steps = 600;
							//speed = 0;

						} else {
							fram = 0;
							pilot.stop();
							cleaner.stopClean();
							cleaner.setPos(false);
							cleaner.startClean();
							steps = 600;
							//speed = 0;
						}

					}
					steps--;


					/*pilot.travel(10);
					cleaner.stopClean();
					pilot.travel(-10);
					cleaner.setPos(true);
					cleaner.startClean();
					pilot.travel(10);
					cleaner.stopClean();
					cleaner.setPos(false); */

					//control.exit = true; //ersgdfg
				}
				Delay.msDelay(1);
			}
		}
	}

	class ThreadSensor implements Runnable {
		public void run() {

			//	Brick brick = BrickFinder.getDefault();
			//	EV3 ev3 = (EV3) BrickFinder.getLocal();
			//	TextLCD lcd = ev3.getTextLCD();

			//	Port s3 = brick.getPort("S3"); // ultrasonisksensor
			//    NXTSoundSensor soundS = new NXTSoundSensor(s3); // ev3-fargesensor
			//	SampleProvider soundR = soundS.getMode("DBA");  // svart = 0.01..
			//	float[] soundSample = new float[soundR.sampleSize()];
    		//SampleProvider fargeLeser = fargesensor.getColorIDMode();
				Brick brick = BrickFinder.getDefault();
				EV3 ev3 = (EV3) BrickFinder.getLocal();
				TextLCD lcd = ev3.getTextLCD();

				LCD.drawString("SS: ", 0, 0);

				Port s4 = brick.getPort("S4");

				Port s3 = brick.getPort("S3"); // ultrasonisksensor
			//    NXTSoundSensor soundS = new NXTSoundSensor(s3);

				EV3ColorSensor fargeSensor = new EV3ColorSensor(s4);
				SampleProvider fargeLeser = fargeSensor.getRedMode();
				float[] fargeSample = new float[fargeLeser.sampleSize()];

				NXTSoundSensor soundS = new NXTSoundSensor(s3); // ev3-fargesensor
				SampleProvider soundR = soundS.getDBAMode();  // svart = 0.01..
				float[] soundSample = new float[soundR.sampleSize()];

				float maxS = 0.0F;

			while (!control.exit) {
				if (!control.snu) {
					fargeLeser.fetchSample(fargeSample, 0); //konvertering fra farge til tallverdi
					if (fargeSample[0] < 0.07F) {
						control.snu = true;
						LCD.drawString("Snu", 0, 3);
						Delay.msDelay(4000);
					}
					LCD.drawString("SS: " + soundSample[0], 0, 1);

				}

				if (true) {//control.stop == 0) {
					soundR.fetchSample(soundSample, 0);
					if (soundSample[0] > 0.8F) {
						control.stop = 300;

						//Delay.msDelay(3000);

					}
					LCD.drawString("FS: " + fargeSample[0], 0, 0);

				}

				if (soundSample[0] > maxS) {
					maxS = soundSample[0];
					LCD.drawString("maxSS: " + maxS, 0, 2);
				}

				LCD.drawString("Stopping: " + control.stop, 0, 4);

			}
		}
	}

	private void tunnel() {
		ThreadClean tClean = new ThreadClean();
		ThreadSensor tSensor = new ThreadSensor();

		new Thread(tSensor).start();
		new Thread(tClean).start();

	}

	static RegulatedMotor leftM = Motor.A;
	static RegulatedMotor rightM = Motor.B;

	public static void main(String[] args) {

		HitraTunnel func = new HitraTunnel();
		func.tunnel();

	}
}