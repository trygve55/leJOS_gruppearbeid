import lejos.hardware.motor.*;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.port.Port;
import lejos.hardware.ev3.EV3;
import lejos.hardware.Keys;
import lejos.hardware.Key;
import lejos.hardware.lcd.*;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.*;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class FuryRoad {

	class Control {
		public volatile boolean stop = false;
		public volatile double targetMotorSpeed = 180.0;
		public volatile double correctionMotorSpeed = 1;
		public volatile double rightMotorSpeed = 0; //targetMotorSpeed;
		public volatile double leftMotorSpeed = 0; //targetMotorSpeed;
		public volatile int path = -1;			//1 = left, 2 = right
		public volatile int pathProg = 0;
		public volatile int[] pathLeftLength = {0, 88, 0, 65, 0, 70, 0, 50, 0, 0};			//lengde på action i cm        0 for sensor basert
		public volatile int[] pathRightLength = pathLeftLength;			//	deg = s*5.5*pi/720
		public volatile int[] pathLeftAction = {2,  2, 5,  2, 5,  2, 5,  2, 5, 0};    		// 0 = slow(speed 100), 1 = medium(speed 150), 2 = fast (speed 200), 3 = very fast(300), 4 = crossPass, 5 = sving
		public volatile int[] pathRightAction = pathLeftAction;
		public volatile int tachoMeter = 0;
		public volatile boolean tachoMeterReset = false;
		public volatile int waitForTurn = 0;     //0 = off, 1 = left, 2 = right
		public volatile boolean exit = false;
	}

	final Control control = new Control();

	class ThreadDrive implements Runnable {
		public void run() {

			double rS = 0, lS = 0;

			RegulatedMotor leftM = Motor.B;
			RegulatedMotor rightM = Motor.A;

			while (!control.exit) {
				//control.rightMotorSpeed++;
				//control.leftMotorSpeed++;
				if (control.rightMotorSpeed != rS) {
					Motor.A.setSpeed((int) control.rightMotorSpeed);
					Motor.A.backward();                         //test
					rS = control.rightMotorSpeed;
			 	}
				if (control.leftMotorSpeed != lS) {
					Motor.B.backward();							//test
					Motor.B.setSpeed((int) control.leftMotorSpeed);
					lS = control.leftMotorSpeed;
				}

				if (control.tachoMeterReset) {
					leftM.resetTachoCount();
					rightM.resetTachoCount();
					control.tachoMeter = 0;
					control.tachoMeterReset = false;
				} else {
					control.tachoMeter = (leftM.getTachoCount() + rightM.getTachoCount())/(-21);
				}
				//Motor.A.backward();
				//Motor.B.backward();
				//Delay.msDelay(5000);
				//control.exit = true;
			}
		}
	}

	class ThreadSensor implements Runnable {
		public void run() {

			Brick brick = BrickFinder.getDefault();
			EV3 ev3 = (EV3) BrickFinder.getLocal();
			TextLCD lcd = ev3.getTextLCD();
			Keys keys = ev3.getKeys();

			LCD.drawString("Loading sensors", 0, 0);

			Port s1 = brick.getPort("S1");
			Port s2 = brick.getPort("S2");
			Port s4 = brick.getPort("S4");

			EV3ColorSensor fargeSensorR = new EV3ColorSensor(s1);
			SampleProvider fargeLeserR = fargeSensorR.getRedMode();
			float[] fargeSampleR = new float[fargeLeserR.sampleSize()];

			EV3ColorSensor fargeSensorL = new EV3ColorSensor(s2);
			SampleProvider fargeLeserL = fargeSensorL.getRedMode();
			float[] fargeSampleL = new float[fargeLeserL.sampleSize()];

			EV3GyroSensor gyroSensor = new EV3GyroSensor(s4);
			SampleProvider gyroLeser = gyroSensor.getAngleMode();
			float[] gyroSample = new float[gyroLeser.sampleSize()];

			LCD.clear();
			LCD.drawString("Kalibrer svart.", 0, 0);
			keys.waitForAnyPress();
			fargeLeserR.fetchSample(fargeSampleR, 0);
			float kalFRightS = fargeSampleR[0];
			fargeLeserL.fetchSample(fargeSampleL, 0);
			float kalFLeftS = fargeSampleL[0];
			Delay.msDelay(500);
			LCD.drawString("Kalibrer hvit.", 0, 0);
			keys.waitForAnyPress();
			fargeLeserR.fetchSample(fargeSampleR, 0);
			float kalFRightH = fargeSampleR[0];
			fargeLeserL.fetchSample(fargeSampleL, 0);
			float kalFLeftH = fargeSampleL[0];

			float rightLightThreshold = (kalFRightS + kalFRightH)/2;
			float leftLightThreshold = (kalFLeftS + kalFLeftH)/2;

			LCD.drawString("kalFRightS: " + kalFRightS, 0, 0);
			LCD.drawString("kalFRightH: " + kalFRightH, 0, 1);
			LCD.drawString("kalFLeftS:  " + kalFLeftS, 0, 2);
			LCD.drawString("kalFLeftH:  " + kalFLeftH, 0, 3);
			LCD.drawString("LeftT:  " + leftLightThreshold, 0, 4);
			LCD.drawString("RightT: " + rightLightThreshold, 0, 5);

			Delay.msDelay(2000);

			LCD.clear();
			LCD.drawString("Velg retning" , 0, 0);

			//1 = mot venstre, 2 = mot høyre
			/*while (path == 0) {
				if (keys.ID_LEFT.isDown()) {
					path = 1;
				} else if (keys.RIGHT.isDown()) {
					path = 2;
				}
			} */

			boolean turnR = false;
			boolean turnL = false;
			boolean triggered = false;
			int drawTimer = 15;
			int turnTimer = 0;
			float angleOffset = 0;


			LCD.drawString("Trykk en knapp for å starte.", 0, 0);
			keys.waitForAnyPress();

			control.rightMotorSpeed = control.targetMotorSpeed;
			control.leftMotorSpeed = control.targetMotorSpeed;

			LCD.clear();

			LCD.drawString("TurnT: ", 0, 1);
			LCD.drawString("Gyro: ", 0, 2);
			LCD.drawString("RMS: ", 0, 3);
			LCD.drawString("LMS: ", 0, 4);
			LCD.drawString("Tacho: ", 0, 5);
			LCD.drawString("PathProg: ", 0, 6);
			LCD.drawString("MS/CS: ", 0, 7);

			control.path = 1;

			while (!control.exit) {
				fargeLeserR.fetchSample(fargeSampleR, 0);
				fargeLeserL.fetchSample(fargeSampleL, 0);	//konvertering fra farge til tallverdi

				if (fargeSampleR[0] < rightLightThreshold) {
					if (control.leftMotorSpeed < 500) {
						control.leftMotorSpeed += control.correctionMotorSpeed;
					}
					if (control.rightMotorSpeed > 12) {
						control.rightMotorSpeed -= control.correctionMotorSpeed;
					}
					turnL = true;
					if (control.waitForTurn == 2) {
					}
				} else if (control.leftMotorSpeed > control.targetMotorSpeed) {
					control.leftMotorSpeed = control.targetMotorSpeed;
					turnL = false;

					if (!turnR) {
						control.rightMotorSpeed = control.targetMotorSpeed;
					}
				}

				if (fargeSampleL[0] < leftLightThreshold) {
					if (control.rightMotorSpeed < 500) {
						control.rightMotorSpeed += control.correctionMotorSpeed;
					}
					if (control.leftMotorSpeed > 12) {
						control.leftMotorSpeed -= control.correctionMotorSpeed;

					}
					if (control.waitForTurn == 1) {
					}
					turnR = true;

				} else if (control.rightMotorSpeed > control.targetMotorSpeed) {
					control.rightMotorSpeed = control.targetMotorSpeed;
					turnR = false;
					if (!turnL) {
						control.leftMotorSpeed = control.targetMotorSpeed;
					}
				}

				if (control.waitForTurn ==  1) {
					if (!triggered) {
						gyroLeser.fetchSample(gyroSample, 0);
					}

					if (triggered) {
						turnTimer--;
					} else if (gyroSample[0] - angleOffset > 75) {
						triggered = true;
					} else if (turnTimer == -1) {
						turnTimer = 300;
						angleOffset = gyroSample[0];
					} else if (turnTimer == 0) {
						control.waitForTurn = 3;
						triggered = false;
						turnTimer = -1;
					}
				}

				if (control.waitForTurn ==  2) {
					if (!triggered) {
						gyroLeser.fetchSample(gyroSample, 0);
					}

					if (triggered) {
						turnTimer--;
					} else if (turnTimer == -1) {
						turnTimer = 300;
						angleOffset = gyroSample[0];
					} else if (gyroSample[0] - angleOffset < -80) {
						triggered = true;
					} else if (turnTimer == 0) {
						control.waitForTurn = 3;
						triggered = false;
						turnTimer = -1;
					}


				}

				if (drawTimer == 20) {
					LCD.drawString("" + turnTimer, 10, 1);
					LCD.drawString("" + (gyroSample[0]-angleOffset) + "   ", 10, 2);
					LCD.drawString("" + control.rightMotorSpeed, 10, 3);
					LCD.drawString("" + control.leftMotorSpeed, 10, 4);
					LCD.drawString(control.tachoMeter + "/" + control.pathLeftLength[control.pathProg] + "      ", 10, 5);
					LCD.drawString(control.pathProg + "/" + (control.pathLeftLength.length - 1), 10, 6);
					LCD.drawString(control.targetMotorSpeed + "/" + control.correctionMotorSpeed, 10, 7);
					drawTimer = 0;
				}
				drawTimer++;
				//Delay.msDelay(2);
			}

			//LCD.clear();

			//Delay.msDelay(2);



		}
	}

	class ThreadPath implements Runnable {
		public void run() {

			int path = control.path;
			int[] pathLength;
			int[] pathAction;

			while (control.path == -1) {
			}

			path = control.path;

			if (path == 1) {
				pathLength = control.pathLeftLength;
				pathAction = control.pathLeftAction;
			} else {
				pathLength = control.pathRightLength;
				pathAction = control.pathRightAction;
			}

			while (!control.exit) {
				if ( control.tachoMeter >= pathLength[control.pathProg] && !(control.waitForTurn == 1 || control.waitForTurn == 2) || control.waitForTurn == 3 || control.pathProg == 0) {   			//s = (deg/360)*5.5cm*pi*2     deg = s*360/(5.5cm*pi*2)            s = 1 -> deg = 10,42
					control.waitForTurn = 0;
					control.tachoMeterReset = true;
					if (control.pathProg < pathLength.length - 1 ) {
						control.pathProg++;
					} else {
						break;
					}
					switch (pathAction[control.pathProg]) {

						case 0:		control.targetMotorSpeed = 120;
									control.correctionMotorSpeed = 1;
									break;

						case 1:		control.targetMotorSpeed = 150;
									control.correctionMotorSpeed = 1;
									break;

						case 2:		control.targetMotorSpeed = 180;
									control.correctionMotorSpeed = 1.0;
									break;

						case 3:		control.targetMotorSpeed = 200;
									control.correctionMotorSpeed = 0.8;
									break;

						case 4:		control.targetMotorSpeed = 150;
									control.correctionMotorSpeed = 0;
									break;

						case 5:		control.targetMotorSpeed = 125;			//left
									control.correctionMotorSpeed = 1;
									control.waitForTurn = 1;
									break;

						case 6:		control.targetMotorSpeed = 125;			//right
									control.correctionMotorSpeed = 1;
									control.waitForTurn = 2;
									break;
					}


					Delay.msDelay(1000);
				}
			}
		}
	}

	private void tunnel() {
		ThreadDrive tDrive = new ThreadDrive();
		ThreadSensor tSensor = new ThreadSensor();
		ThreadPath tPath = new ThreadPath();

		new Thread(tSensor).start();
		new Thread(tDrive).start();
		new Thread(tPath).start();

	}

	//static RegulatedMotor leftM = Motor.B;
	//static RegulatedMotor rightM = Motor.A;

	public static void main(String[] args) {

		FuryRoad func = new FuryRoad();
		func.tunnel();

	}
}