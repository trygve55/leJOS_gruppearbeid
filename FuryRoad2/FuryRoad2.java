import lejos.hardware.motor.*;
import lejos.robotics.RegulatedMotor;
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
import lejos.robotics.filter.MeanFilter;
// import lejos.robotics.filter.SampleThread;

class LightSensors {
	float leftSample, rightSample;
	float rightLightThreshold, leftLightThreshold;

	EV3ColorSensor fargeSensorR, fargeSensorL;
	SampleProvider fargeLeserR, fargeLeserL;
	float[] fargeSampleR, fargeSampleL;
	//SampleThread fargeThreadR, fargeThreadL;

	public LightSensors(String leftSensorPort, String rightSensorPort) {
		Brick brick = BrickFinder.getDefault();

		BrickKeys key = new BrickKeys();
		Screen screen = new Screen();

		//laster sensorer
		screen.printClear("Laster sensorer", 0, 1);

		Port leftSensor = brick.getPort(leftSensorPort);
		Port rightSensor = brick.getPort(rightSensorPort);

		this.fargeSensorR = new EV3ColorSensor(rightSensor);
		this.fargeLeserR = fargeSensorR.getRedMode();
		this.fargeSampleR = new float[fargeLeserR.sampleSize()];
		//this.fargeThreadR = new SampleThread(fargeLeserR, 100.0F);

		this.fargeSensorL = new EV3ColorSensor(leftSensor);
		this.fargeLeserL = fargeSensorL.getRedMode();
		this.fargeSampleL = new float[fargeLeserL.sampleSize()];
		//this.fargeThreadL = new SampleThread(fargeLeserL, 100.0F);


		//kalibrere sensorer
		screen.printClear("Kalibrer svart", 0, 1);
		key.waitKey();
		updateSensors();
		float kalFRightS = rightSample;
		float kalFLeftS = leftSample;

		screen.printClear("Kalibrer hvit", 0, 1);
		key.waitKey();
		updateSensors();
		float kalFRightH = rightSample;
		float kalFLeftH = leftSample;
		
		// //kalibrere sensorer
		// screen.printClear("Kalibrer svart", 0, 1);
		// key.waitKey();
		// fargeLeserL.fetchSample(fargeSampleL, 0);
		// fargeLeserR.fetchSample(fargeSampleR, 0);
		// float kalFRightS = fargeSampleR[0];
		// float kalFLeftS = fargeSampleL[0];

		// screen.printClear("Kalibrer hvit", 0, 1);
		// key.waitKey();
		// fargeLeserL.fetchSample(fargeSampleL, 0);
		// fargeLeserR.fetchSample(fargeSampleR, 0);
		// float kalFRightH = fargeSampleR[0];
		// float kalFLeftH = fargeSampleL[0];

		this.rightLightThreshold = (kalFRightS + kalFRightH)/2 + 0.1F;
		this.leftLightThreshold = (kalFLeftS + kalFLeftH)/2 + 0.1F;

		screen.printClear("Right tres: " + rightLightThreshold, 0, 0);
		screen.print("left tres:  " + leftLightThreshold, 0, 1);
		Delay.msDelay(2000);
	}

	public void updateSensors() {
		updateLeft();
		updateRight();
	}

	public void updateLeft() {
		fargeLeserL.fetchSample(fargeSampleL, 0);
		leftSample = fargeSampleL[0];
	}

	public void updateRight() {
		fargeLeserR.fetchSample(fargeSampleR, 0);
		rightSample = fargeSampleR[0];
	}

	public boolean getLeft() {
		updateLeft();
		return (leftSample < leftLightThreshold);
		
		// fargeThreadL.fetchSample(fargeSampleL, 0);
		// return (fargeSampleL[0] < leftLightThreshold);
	}

	public boolean getRight() {
		updateRight();
		return (rightSample < rightLightThreshold);
		
		// fargeThreadR.fetchSample(fargeSampleR, 0);
		// return (fargeSampleR[0] < rightLightThreshold);
	}

	public float getLeftValue() {
		return leftSample;
	}

	public float getRightValue() {
		return rightSample;
	}

	public float getLeftLightThreshold() {
		return leftLightThreshold;
		}

	public float getRightLightThreshold() {
		return rightLightThreshold;
	}
}

class GyroSensor {
	float angleOffset = 0;

	SampleProvider gyroLeser;
	float[] gyroSample;
	MeanFilter filter;

	public GyroSensor(String GyroSensorPort) {
		Brick brick = BrickFinder.getDefault();
		Port gyroPort = brick.getPort("S4");

		EV3GyroSensor gyroSensor = new EV3GyroSensor(gyroPort);
		this.gyroLeser = gyroSensor.getAngleMode();
		gyroSensor.reset();
		this.gyroSample = new float[gyroLeser.sampleSize()];
		this.filter = new MeanFilter(gyroLeser, 310);

	}

	public void reset() {
		//gyroLeser.fetchSample(gyroSample, 0);
		filter.fetchSample(gyroSample, 0);
		angleOffset = gyroSample[0];
	}

	public float getAngle() {
		//gyroLeser.fetchSample(gyroSample, 0);
		filter.fetchSample(gyroSample, 0);
		return (gyroSample[0] - angleOffset);
	}
}

class Screen {
	public Screen() {
		Brick brick = BrickFinder.getDefault();
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		TextLCD lcd = ev3.getTextLCD();
	}

	public void print(String text, int x, int y) {
		LCD.drawString(text, x, y);
	}

	public void printClear(String text, int x, int y) {
		LCD.clear();
		LCD.drawString(text, x, y);
	}

	public void printClear(String text) {
		LCD.clear();
		LCD.drawString(text, 0, 0);
	}

	public void clear() {
		LCD.clear();
	}
}

class BrickKeys {
	Keys keys;

	public BrickKeys() {
		Brick brick = BrickFinder.getDefault();
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		this.keys = ev3.getKeys();
	}

	public void waitKey() {
		keys.waitForAnyPress();
	}
}

class MotorControl {
	RegulatedMotor leftMotor, rightMotor;
	float leftMotorSpeed, rightMotorSpeed, maxSpeed, minSpeed;

	public MotorControl(RegulatedMotor leftMotor, RegulatedMotor rightMotor, int maxSpeed, int minSpeed) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.leftMotorSpeed = 0.0F;
		this.rightMotorSpeed = 0.0F;
		this.maxSpeed = (float) maxSpeed;
		this.minSpeed = (float) minSpeed;

		leftMotor.backward();
		rightMotor.backward();
		leftMotor.setSpeed((int) leftMotorSpeed);
		rightMotor.setSpeed((int) rightMotorSpeed);
	}

	public void leftSpeed(int newSpeed) {
		if (newSpeed != leftMotorSpeed) {
			leftMotorSpeed = newSpeed;
			leftMotor.setSpeed((int) leftMotorSpeed);
			leftMotor.backward();
		}
	}

	public void rightSpeed(int newSpeed) {
		if (newSpeed != rightMotorSpeed && newSpeed <= maxSpeed && newSpeed >= minSpeed) {
			rightMotorSpeed = (float) newSpeed;
			rightMotor.setSpeed((int) rightMotorSpeed);
			rightMotor.backward();
		}
	}

	public void leftSpeedChange(int newSpeed) {
		if (leftMotorSpeed + newSpeed <= maxSpeed && leftMotorSpeed + newSpeed >= minSpeed) {
			leftMotorSpeed += newSpeed;
			leftMotor.setSpeed((int) leftMotorSpeed);
			leftMotor.backward();
		}
	}

	public void rightSpeedChange(int newSpeed) {
		if (rightMotorSpeed + newSpeed <= maxSpeed && rightMotorSpeed + newSpeed >= minSpeed) {
			rightMotorSpeed += newSpeed;
			rightMotor.setSpeed((int) rightMotorSpeed);
			rightMotor.backward();
		}
	}

	public int getDistance() {
		return (leftMotor.getTachoCount() + rightMotor.getTachoCount())/(-21);
	}

	public void resetDistance() {
		leftMotor.resetTachoCount();
		rightMotor.resetTachoCount();
	}
	
	public void maxSpeed() {
		int speed = 300;
		while (speed < 1000) {
			speed += 2;
			leftMotor.setSpeed(speed);
			rightMotor.setSpeed(speed);
			leftMotor.backward();
			rightMotor.backward();
			speed += 2;
			rightMotor.setSpeed(speed);
			leftMotor.setSpeed(speed);
			rightMotor.backward();
			leftMotor.backward();
		}
		Delay.msDelay(1300);
		leftMotor.setSpeed(0);
		rightMotor.setSpeed(0);
		leftMotor.stop();
		rightMotor.stop();
		Delay.msDelay(10000);
	}
}

class FuryRoad2 {

	public static void main(String[] args) {

		LightSensors light = new LightSensors("S2", "S1");
		MotorControl motor = new MotorControl(Motor.A, Motor.B, 850, 40);
		Screen screen = new Screen();
		BrickKeys keys = new BrickKeys();
		GyroSensor gyro = new GyroSensor("S4");

		int normalSpeed = 280;
		int saveTimer = 0, svingTime = 0, printTimer = 0, path = 0, startTimer = 2000, runde = 0, stopTimer = 0;
		boolean retningHoyre = true;
		float gyroV = 0;

		screen.printClear("Trykk for aa starte");
		keys.waitKey();
		gyro.reset();	

		while(true) {
			
			if (path == 5) {
				if (light.getLeft()) {
					motor.leftSpeed(normalSpeed);
					motor.rightSpeedChange(-20);
				} else if (light.getRight()) {
					motor.rightSpeed(normalSpeed);
					motor.leftSpeedChange(-12);
				} else {
					motor.leftSpeed(normalSpeed);
					motor.rightSpeed(normalSpeed);
				}
			} else if (path == 10 || path == 0) {
				if (light.getRight()) {
					motor.rightSpeed(normalSpeed);
					motor.leftSpeedChange(-20);
				} else if (light.getLeft()) {
					motor.leftSpeed(normalSpeed);
					motor.rightSpeedChange(-12);
				} else {
					motor.rightSpeed(normalSpeed);
					motor.leftSpeed(normalSpeed);
				}
			} else {
				if (light.getLeft()) {
					motor.rightSpeedChange(-19);
					motor.leftSpeedChange(12);
				} else {
					motor.rightSpeed(normalSpeed);
				}
				if (light.getRight()) {
					motor.leftSpeedChange(-19);
					motor.rightSpeedChange(12);
				} else {
					motor.leftSpeed(normalSpeed);
				}
			}

			if (saveTimer == 199) {
				gyroV = gyro.getAngle();
				//screen.print("" + gyroV, 0, 6);
				saveTimer = 0;
				if (gyroV > 90.0F) {
					gyro.reset();
					svingTime = 1;
					path++;
					//motor.resetDistance();
					//screen.print("Left ", 0, 5);
				} else if (gyroV < -90.0F) {
					//screen.print("Right ", 0, 5);
					svingTime = 1;
					gyro.reset();
					path++;
					//motor.resetDistance();
				}
				
				if (path == 11) {
					path = 1;
					runde++;
				}
				if (path ==  7 || path == 8) {
					 normalSpeed = 250;
				} else {
					normalSpeed = 280;
				}
				
			if (runde == 1 && path == 10 && retningHoyre || runde == 2 && path == 5 && !retningHoyre) {
				if (stopTimer == 0) {
					stopTimer = 1;
					motor.resetDistance();
				} else if (motor.getDistance() > 140) {
					motor.rightSpeed(0);
					motor.leftSpeed(0);
					break;
				}
			} 
			}
				
			// if (printTimer == 997) {
				// // screen.print("left: " + leftTurns, 0, 1);
				// // screen.print("right: " + rightTurns, 0, 2);
				// screen.print("path: " + path + "  ", 0, 3);
				// // screen.print("TM: " + motor.getDistance() + "     ", 0, 4);
				// printTimer = 0;
				
			// } 
			// printTimer++;
			saveTimer++;
			if (svingTime > 0) {
				svingTime++;
				if (svingTime == 800) {
					gyro.reset();
				}
			}
					
					
			if (startTimer > 0) {
				startTimer--;
				if (startTimer == 0) {
					path = 0;
					if (!retningHoyre) path = 5;
				}
			}
			//screen.print("LLS: " + light.getLeftValue() + ":" + light.getLeftLightThreshold() + "  ", 0, 2);
			//screen.print("RLS: " + light.getRightValue() + ":" + light.getRightLightThreshold() + "  ", 0, 3);
		}


	}
}