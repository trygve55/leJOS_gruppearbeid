import lejos.hardware.motor.*;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.port.Port;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.NXTSoundSensor;
import lejos.hardware.sensor.NXTTouchSensor;
import lejos.hardware.sensor.SensorMode;
import lejos.hardware.motor.*;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.port.Port;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.NXTSoundSensor;
import java.io.File;

import java.lang.Object;
import lejos.hardware.Device;
import lejos.hardware.sensor.BaseSensor;
import lejos.hardware.sensor.AnalogSensor;
import lejos.hardware.sensor.NXTSoundSensor;
import lejos.robotics.navigation.*;
import lejos.hardware.Button;


public class cleaner
{
	
	static RegulatedMotor leftM = Motor.B;
	static RegulatedMotor rightM = Motor.A;
	static RegulatedMotor vaskeM = Motor.C;

	 public static void main (String[] args)  throws Exception
	 {
		 
		final float wheelDiameter = 5.5F;
		final float wheelDistance = 17.0F;
		final boolean reverse = true;
		final int gridSize = 30;
			
		DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, wheelDistance, leftM, rightM, reverse);
			
		pilot.setLinearAcceleration(500);
		pilot.setLinearSpeed(20);
		pilot.setAngularSpeed(120);

		Brick brick = BrickFinder.getDefault();
		 
		Port s1 = brick.getPort("S1"); // fargesensor
		EV3ColorSensor fargeLeser = new EV3ColorSensor(s1);
		float[] fargeSample = new float[fargeLeser.sampleSize()];
		 
 		Port s2 = brick.getPort("S2"); // trykksensor
 		SampleProvider trykkSensorH = new EV3TouchSensor(s2);
		float[] trykkSampleH = new float[trykkSensorH.sampleSize()];

 		Port s3 = brick.getPort("S3"); // trykksensor
 		SampleProvider trykkSensorV = new NXTTouchSensor(s3);
	    float[] trykkSampleV = new float[trykkSensorV.sampleSize()];

		Port s4 = brick.getPort("S4"); // lydsensor
		NXTSoundSensor lydSensor = new NXTSoundSensor(s4);
		SampleProvider lydLeser = lydSensor.getDBAMode();
		float[] lydSample = new float[lydLeser.sampleSize()];
		 
		int svart = 0;
		//Beregner verdi for svart
		for (int i = 0; i < 100; i ++)
		{
			fargeLeser.fetchSample(fargeSample, 0);
			svart += fargeSample[0] * 100;
		}
			
		svart = svart / 100 + 5;
		System.out.println("Svart: " + svart);
		
		int sound = 0; 
		//beregne verdi for lyd
		for (int m = 0; m < 100; m ++)
		{
			lydLeser.fetchSample(lydSample, 0);
			sound += lydSample[0] * 100;
		}
		
		sound = sound / 100 + 5;
		System.out.println("Lyd" + sound);
 
		while (fargeSample[0] < svart)
		 {
			fargeLeser.fetchSample(fargeSample, 0);
			trykkSensorH.fetchSample(trykkSampleH, 0);
			trykkSensorV.fetchSample(trykkSampleV, 0);
			lydLeser.fetchSample(lydSample, 0);
			
			Motor.A.setSpeed(40);
			Motor.B.setSpeed(40);
			Motor.C.setSpeed(40);
			
			 if (sound > 5)
			 {
				 Motor.A.stop();
				 Motor.B.stop();
				 Motor.C.stop();
				 Thread.sleep(5000);
			 }

		 }
		 System.out.println("bye bye, motherfuckers");
	 }
 }
