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
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;
// import lejos.robotics.filter.SampleThread;

class LightSensors {
	float leftSample, rightSample;
	float rightLightThreshold, leftLightThreshold;

	EV3ColorSensor fargeSensorR, fargeSensorL;
	SampleProvider fargeLeserR, fargeLeserL;
	float[] fargeSampleR, fargeSampleL;
	//SampleThread fargeThreadR, fargeThreadL;

	public LightSensors(String rightSensorPort) {
		Brick brick = BrickFinder.getDefault();
		Screen screen = new Screen();
		//laster sensorer
		screen.printClear("Laster sensorer", 0, 1);

		//Port leftSensor = brick.getPort(leftSensorPort);
		Port rightSensor = brick.getPort(rightSensorPort);

		this.fargeSensorR = new EV3ColorSensor(rightSensor);
		this.fargeLeserR = fargeSensorR.getRGBMode();
		this.fargeSampleR = new float[fargeLeserR.sampleSize()];
		//this.fargeThreadR = new SampleThread(fargeLeserR, 100.0F);

		//this.fargeSensorL = new EV3ColorSensor(leftSensor);
		//this.fargeLeserL = fargeSensorL.getRGBMode();
		//this.fargeSampleL = new float[fargeLeserL.sampleSize()];
		//this.fargeThreadL = new SampleThread(fargeLeserL, 100.0F);

	}

	public int[] getPixel() {
		int[] rgb = new int[3];
		fargeLeserR.fetchSample(fargeSampleR, 0);
		rgb[0] = (int) (fargeSampleR[0] * 255 * 2);
		rgb[1] = (int) (fargeSampleR[1] * 255 * 2);
		rgb[2] = (int) (fargeSampleR[2] * 255 * 2.3);
		return rgb;
		
		// fargeThreadR.fetchSample(fargeSampleR, 0);
		// return (fargeSampleR[0] < rightLightThreshold);
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
	RegulatedMotor xMotor, yMotor;
	SampleProvider trykksensor;
	float[] trykkSample;
	int yDistance;

	public MotorControl(RegulatedMotor xMotor, RegulatedMotor yMotor, String resetSensor, int yDistance) {
		this.xMotor = xMotor;
		this.yMotor = yMotor;
		xMotor.setSpeed(800);
		yMotor.setSpeed(800);
		
		this.yDistance = yDistance;
		
		Brick brick = BrickFinder.getDefault();
		Port s1 = brick.getPort(resetSensor); 
		this.trykksensor = new NXTTouchSensor(s1);
		this.trykkSample = new float[trykksensor.sampleSize()];
	}
	
	public void xPlus() {
		xMotor.rotate(45); // 10 mm
	}
	
	public void yPlus() {
		yMotor.rotate(-250); // 10 mm
	}

	public int getX() {
		return xMotor.getTachoCount();
	}
	
	public int getY() {
		return yMotor.getTachoCount();
	}

	public void resetX() {
		xMotor.backward();
		trykksensor.fetchSample(trykkSample, 0);
		while (trykkSample[0] == 0) {
			trykksensor.fetchSample(trykkSample, 0);
		}
		stopX();
		Delay.msDelay(100);
		resetDistanceX();
	}
	
	public void setSpeedX(int speed) {
		if (speed >= 0) {
			xMotor.setSpeed(speed);
			xMotor.forward();
		} else {
			xMotor.setSpeed(-speed);
			xMotor.backward();
		}
	}
	
	public void stopX(){
		xMotor.stop();
	}
	
	public int getDistanceX() {
		return xMotor.getTachoCount();
	}

	public void resetDistanceX() {
		xMotor.resetTachoCount();
	}
	
	public void moveToPosX(int pos) {
		xMotor.setSpeed(200);
		while (getDistanceX() != pos) {
			if (getDistanceX() > pos) {
				xMotor.backward();
			} else {
				xMotor.forward();
			}
		}
		xMotor.stop();
	}
}

class ImageArray {
	int[][][] bilde;
	BufferedImage img = null;
	Color color;
	
	public ImageArray(int width, int height) {
		bilde = new int[width][height][3];
		img = new BufferedImage(width, height, 1);
	}
	
	public void scanPixel(int x, int y, int[] rgb) {
		for (int i = 0;i < 3;i++) bilde[x][y][i] = rgb[i];
			
		//System.out.println(rgb[0] + ", " + rgb[1] + ", " + rgb[2]);
	}
	
	public void toFile(String name) {
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				// for (int c = 0; c < 3; c++) {
					// int rgb 
				// }
				color = new Color(bilde[x][y][0], bilde[x][y][1], bilde[x][y][2]);
				img.setRGB(x, y, color.getRGB());
			}
		}
		
		try {
			File outputfile = new File(name + ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			
		}
	}
}

class Printer {

	public static void main(String[] args) {

		double widthMM = 150, heightMM = 150, dpi = 40;
		
		int width = (int) (widthMM * dpi /25.4), height =  (int) (heightMM * dpi /25.4), distanceX = (int) (200 / dpi); //dpi 
	
		LightSensors light = new LightSensors("S4");
		MotorControl motor = new MotorControl(Motor.D, Motor.A, "S1", (int)(3500 / dpi));
		Screen screen = new Screen();
		BrickKeys keys = new BrickKeys();
		int[] rgb = new int[3];
		String out = "";

		//screen.printClear("Trykk for aa starte");
		//keys.waitKey();	
		
		// while (true) {
			// rgb = light.getPixel();
			// out = rgb[0] + ", " + rgb[1] + ", " + rgb[2];
			// System.out.println(out);
			// Delay.msDelay(500);
		// }
		
		
		
		ImageArray image = new ImageArray(width, height);
		//ImageArray image2 = new ImageArray(width, height);
		
		motor.resetX();
		for (int y = 0;y < height;y++) {
			// for (int x = 0;x < width;x++) {
				// image.scanPixel(x, y, light.getPixel());
				// motor.xPlus();
			// }
			motor.setSpeedX(900);
			for (int x = 0;x < width;) {
				if (x * distanceX <= motor.getDistanceX()) {
					image.scanPixel(x, y, light.getPixel());
					x++;
				}
				
			}
			
			if (y < height - 1) {
				y++;
				motor.moveToPosX(width*distanceX);
				motor.yPlus();
				//Delay.msDelay(250);
				motor.setSpeedX(-900);
				for (int x = width -1;x >= 0;) {
					if (x * distanceX - 26 >= motor.getDistanceX()) {
						image.scanPixel(x, y, light.getPixel());
						x--;
					}
					
				}
				//motor.resetDistanceX();
			}
			
			motor.resetX();
			//motor.resetDistanceX();
			motor.yPlus();
			
		}

		image.toFile("saved");
		//image2.toFile("saved1");
	}
}