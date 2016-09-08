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

public class GolfBaneBil3 {

	//ordne motorer for DifferentialPilot
	static RegulatedMotor leftM = Motor.B;
	static RegulatedMotor rightM = Motor.A;


	public static void main(String[] args) throws Exception {

		//Pilot start
		final float wheelDiameter = 5.5F;
		final float wheelDistance = 17.0F;
		final boolean reverse = true;
		final int gridSize = 30;

		DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, wheelDistance, leftM, rightM, reverse);

		pilot.setLinearAcceleration(500);
		pilot.setLinearSpeed(20);
		pilot.setAngularSpeed(120);

		//US Sensor start
		Brick brick = BrickFinder.getDefault();
		Port s1 = brick.getPort("S1"); // ultrasonisksensor
		EV3UltrasonicSensor ultraSensor = new EV3UltrasonicSensor(s1);
		SampleProvider ultraLeser = ultraSensor.getDistanceMode();
		float[] ultraSample = new float[ultraLeser.sampleSize()]; // tabell som inneholder avlest verdi

		//touch sensor for stopp av program
		Port s2 = brick.getPort("S2");
		SampleProvider trykksensor = new EV3TouchSensor(s2);
		float[] trykkSample = new float[trykksensor.sampleSize()];

		//touch sensor venstre
		Port s3 = brick.getPort("S3");
		SampleProvider trykksensorV = new NXTTouchSensor(s3);
		float[] trykkSampleV = new float[trykksensorV.sampleSize()];

		//touch sensor høyre
		Port s4 = brick.getPort("S4");
		SampleProvider trykksensorH = new NXTTouchSensor(s4);
		float[] trykkSampleH = new float[trykksensorH.sampleSize()];

		//wait start
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		TextLCD lcd = ev3.getTextLCD();
		Keys keys = ev3.getKeys();

		lcd.drawString("Klar", 0, 1);
		keys.waitForAnyPress();



		//main

		int lastSving = 0; //0 = none  , 1 = venstre, 2 = høyre           for å hindre at den svinger fram og tilbake i en loop, må vente før den kan svinge andre veien igjen
		int svingTime = 0; //timer for å resette lastSving
		boolean noExit = true;

		while (noExit) {

			//lese sensorer
			ultraLeser.fetchSample(ultraSample, 0);
			trykksensorH.fetchSample(trykkSampleH, 0);
			trykksensorV.fetchSample(trykkSampleV, 0);
			trykksensor.fetchSample(trykkSample, 0);

			//draw debug
			lcd.drawString("Sving time: " + svingTime, 0, 6);
			lcd.drawString("Avstand: " + ultraSample[0], 0, 5);

			//Ultrasonic sensor
			if (ultraSample[0] < 0.25F) {       //vis avstand lavere en 25CM
				lcd.drawString("Sving", 0, 25);
				Motor.A.stop();     			//stop motorer
				Motor.B.stop();
				pilot.rotate(90);				//roter 90 grader
			} else {
				Motor.A.setSpeed(900);			//kjører fort
				Motor.B.setSpeed(900);
				Motor.A.backward();				//kjører framover, motorer monter andre veien
				Motor.B.backward();
			}

			//kufanger
			if (trykkSampleH[0] > 0 || trykkSampleV[0] > 0) {          //noen av front trykksensorer truffet

				if (trykkSampleH[0] == 1 && trykkSampleV[0] == 1) {		//begge helt trykt inn
					pilot.travel(-10);									//kjør 10cm bakover
					pilot.rotate(180);									//roter 180 grader
				} else if (trykkSampleH[0] > trykkSampleV[0] && lastSving != 1 || lastSving == 2) {		//sving høyre
					pilot.travel(-10);
					pilot.rotate(-45);
					svingTime = 1200;
					lastSving = 2;
				} else {																				//sving venstre
					pilot.travel(-10);
					pilot.rotate(45);
					svingTime = 1200;
					lastSving = 1;
				}
			}

			//sving timer reset and counter
			if (svingTime > 0) {
				svingTime--;
				if (svingTime == 0) {
					lastSving = 0;
				}
			}

			//avslutt - knapp bakpå
			if (trykkSample[0] > 0) {
				noExit = false;
			}
		}
	}
}