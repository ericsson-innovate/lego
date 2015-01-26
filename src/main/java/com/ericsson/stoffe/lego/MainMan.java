package com.ericsson.stoffe.lego;

import java.io.File;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.BrickInfo;
import lejos.hardware.Sound;
import lejos.hardware.device.DLights;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.I2CPort;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.robotics.EncoderMotor;
import lejos.utility.Delay;

public class MainMan {
	private static MqttClient mqtt;
	private static boolean engineRunning = false;
	private static boolean alarmOn = false;
	private static boolean trunkOpen = false;
	private static boolean doorsLocked = false;

	private static DLights lights = null;
	private static int lightN = 1;
	private static int[] lightRGB = { 255, 255, 255 };
	private static EV3MediumRegulatedMotor ma,mc,md;
	private static EV3LargeRegulatedMotor mb;
	private static MusicMaker alarmMaker;

	private static String mqtthost =  "mafalda.hack.att.io";
	private static String mqttport = "11883";

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws MqttException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException,
			MqttException, IOException {

		// BrickInfo[] info = BrickFinder.discover();
		// Brick brick = BrickFinder.getDefault();
		// Port p1 = brick.getPort("S1");



		if (args.length == 0) {
			;
		}
		else if (args.length == 1) {
			mqtthost = args[0];
		} else if (args.length == 2) {
			mqtthost = args[0];
			mqttport = args[1];
		} else {
			System.out.println("Invalid number of arguments. Usage lego-1.0-standalone.jar [host] [mqttport] [default: mafalda.hack.att.io, ]");
			System.exit(0);
		}


		System.out.println("Using host " + mqtthost + " and port " + mqttport + "...");


		Port p1 = LocalEV3.get().getPort("S1");

		//System.out.println("Port =" + p1.getPortType() + " sensor ="  + p1.getSensorType());

		MainMan.connectToMQTT();
		// myPort.setTypeAndMode(arg0, arg1)

		try {
			I2CPort myPort = p1.open(I2CPort.class);
			System.out.println("Mode =" + myPort.getMode() + " type="
					+ myPort.getType());
			//myPort.setMode(I2CPort.HIGH_SPEED);
			lights = new DLights(myPort);
		} catch (Throwable t) {
			System.err.println("Failed with Lights ... "
					+ t.getLocalizedMessage());
		}


		//lights = new DLights(port1);

		System.out.println("Laser = " + lights);

		lights.setColor(1,255,255,255);

		lights.enable(1);

		Delay.msDelay(2000);

		lights.disable(1);

		while (true) {
			// connectToMQTT();
			Delay.msDelay(5000);
			// Thread.sleep(5000);
			System.out.println("Tick ---");
		}
	}

	static void connectToMQTT() throws MqttException {
		MemoryPersistence persistence = new MemoryPersistence();
		// MqttDefaultFilePersistence persistence = new
		// MqttDefaultFilePersistence(dir);

		mqtt = new MqttClient("tcp://" + mqtthost + ":" + mqttport, "LegoClient",
				// mqtt = new MqttClient("tcp://12.144.186.180:1883", "LegoClient",
				persistence);
		MqttConnectOptions connOpt = new MqttConnectOptions();
		connOpt.setConnectionTimeout(60 * 10);
		connOpt.setKeepAliveInterval(60 * 5);
		// mqtt.setTimeToWait(60000);
		mqtt.connect(connOpt);

		mqtt.setCallback(new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				System.out.println("connectionLost : "
						+ cause.getLocalizedMessage());
				cause.printStackTrace();
				try {
					MainMan.connectToMQTT();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Log.d("WingMan", );
			}

			@Override
			public void messageArrived(String topic, final MqttMessage message)
					throws Exception {
				System.out.println("Got message = " + message + " in topic - "
						+ topic);
				LCD.clearDisplay();
				LCD.drawString(message.toString(), 0, 4);
				// lejos.hardware.Sound.playSample(arg0, arg1, arg2, arg3, arg4)
				String cmd = new String(message.getPayload()).trim();
				System.out.println("CMD -----" + cmd + "-----");

				String sample = null;
				Boolean blink = false;

				if ("door_lock".equals(cmd)) {
					if (doorsLocked){
						blink = true;
						System.out.println("Lock! Doors already locked. Blinking lights.");
					} else {
						doorsLocked = true;
						sample = "lock.wav";
						blink = true;
						System.out.println("Lock! Locking doors. Blink lights make sound.");
					}
				}
				else if ("door_unlock".equals(cmd))
					if (doorsLocked){
						doorsLocked = false;
						blink = true;
						sample = "unlock.wav";
						System.out.println("Door unlock! Unlocking doors. Blink lights make sound.");
					} else {
						blink = true;
						System.out.println("Door unlock! Doors already unlocked. Blinking lights.");
					}
				else if ("engine_on".equals(cmd)) {
					if (engineRunning)
						System.out.println("Engine On! Engine already running. Doing nothing.");
					else{
						engineRunning = true;
						sample = "engine_on.wav";
						System.out.println("Engine On! Starting engine. Making sound.");
					}
				}
				else if ("engine_off".equals(cmd)) {
					if (engineRunning){
						engineRunning = false;
						sample = "engine_on.wav";
						System.out.println("Engine Off! Stopping engine. Stop making sound.");
					}
					else {
						System.out.println("Engine Off! Engine already off. Doing nothing.");
					}
				}
				else if ("honk_blink".equals(cmd)) {
					blink = true;
					sample = "honk.wav";
					System.out.println("Honk/Blink! Blink lights make sound.");
				}
				else if ("honk".equals(cmd)) {
					sample = "honk.wav";
					System.out.println("Honk! Make sound.");
				}
				else if ("blink".equals(cmd)) {
					System.out.println("Blink! Blinking lights.");
					blink = true;
				}

				else if ("alarm_on".equals(cmd)) {
					if (alarmOn){
						System.out.println("Alarm On! Alarm already engaged. Doing nothing.");
					} else {
						alarmMaker = new MusicMaker();
						alarmMaker.start();
						System.out.println("Alarm On! Starting alarm.");
						alarmOn = true;
					}
				}
				else if ("alarm_off".equals(cmd)) {
					if (alarmOn){
						alarmMaker.stop();
						System.out.println("Alarm Off! Stopping alarm.");
						alarmOn = false;
					} else {
						System.out.println("Alarm Off! Alarm already off. Doing nothing.");
					}
				}

				else if ("open_trunk".equals(cmd)) {
					if (trunkOpen){
						System.out.println("Open Trunk! Trunk already open. Blinking lights.");
						blink = true;
					} else {
						System.out.println("Open Trunk! Opening trunk.");
						// Trunk engine
						try {
							mc = new EV3MediumRegulatedMotor(MotorPort.C);
							mc.forward();
							Delay.msDelay(5000);
							mc.stop();
							mc.close();
						} catch (Throwable t) {
							System.err.println("Failed with opeing trunk - motor C... "
									+ t.getLocalizedMessage());
							t.printStackTrace();
						}
						trunkOpen = true;
					}

				}
				else if ("close_trunk".equals(cmd)) {
					if (trunkOpen){
						System.out.println("Close Trunk! Closing trunk.");
						// Trunk engine
						try {
							mc = new EV3MediumRegulatedMotor(MotorPort.C);
							mc.backward();
							Delay.msDelay(5000);
							mc.stop();
							mc.close();
						} catch (Throwable t) {
							System.err.println("Failed with closing trunk - motor C... "
									+ t.getLocalizedMessage());
							t.printStackTrace();
						}
						trunkOpen = false;
					} else {
						System.out.println("Close Trunk! Trunk already closed. Blinking lights.");
						blink = true;
					}
				}
				else if ("pickup".equals(cmd)) {

					if (!engineRunning){
						engineRunning = true;
					}

					System.out.println("Pickup! Start pre-programmed route.");

					try {
						// Propulsion
						ma = new EV3MediumRegulatedMotor(MotorPort.A);
						// Steering
						mb = new EV3LargeRegulatedMotor(MotorPort.B);

						// Set zero angle
						mb.setSpeed(100);

						// Pre-programmed route
						ma.backward();
						Delay.msDelay(5000);
						mb.rotate(65);

						Delay.msDelay(2000);
						mb.rotate(-130);

						Delay.msDelay(3000);
						mb.rotate(65);

						Delay.msDelay(4000);

						ma.forward();
						Delay.msDelay(1000);

						ma.stop();
						mb.stop();
						ma.close();
						mb.close();
					} catch (Throwable t) {
						System.err.println("Failed with turning on engine - motor A and B... "
								+ t.getLocalizedMessage());
						t.printStackTrace();
					}
					engineRunning = false;
				}

				else if ("park".equals(cmd)) {

					System.out.println("Park! Start pre-programmed route.");

					if (!engineRunning){
						engineRunning = true;
					}

					try {
						// Propulsion
						ma = new EV3MediumRegulatedMotor(MotorPort.A);
						// Steering
						mb = new EV3LargeRegulatedMotor(MotorPort.B);

						// Pre-programmed route
						ma.forward();
						Delay.msDelay(5000);
						mb.rotate(-65);

						Delay.msDelay(2000);
						mb.rotate(130);

						Delay.msDelay(3000);
						mb.rotate(-65);

						Delay.msDelay(4000);

						ma.backward();
						Delay.msDelay(3000);

						ma.stop();
						mb.stop();
						ma.close();
						mb.close();
					} catch (Throwable t) {
						System.err.println("Failed with turning on engine - motor A and B... "
								+ t.getLocalizedMessage());
						t.printStackTrace();
					}
					engineRunning = false;
				}

				if (sample != null) {
					File f = new File("/home/lejos/programs/sounds/" + sample);
					System.out.println("File = " + f.toString());
					int returnCode = Sound.playSample(f, 100);
					System.err.println("Play sample return code is: " + returnCode);
					}

					if (blink) {
						try {
							// Set motor to emulate lights
							md = new EV3MediumRegulatedMotor(MotorPort.D);
							md.forward();
							Delay.msDelay(300);
							md.stop();
							md.close();

						} catch (Throwable t) {
							System.err.println("Failed with Lights - motor port D... "
									+ t.getLocalizedMessage());
							t.printStackTrace();
						}

					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("deliveryComplete() : " + token);
				}
			});
			mqtt.subscribe("toy/luigi");
		}


				// Inner music player class to keep playing sounds
		public static class MusicMaker extends Thread {
			String sample = "honk.wav";
			int delay = 1000;

			public void run() {

				File f = new File("/home/lejos/programs/sounds/" + sample);
				System.out.println("File = " + f.toString());

				while (true) {
					try {
						int returnCode = Sound.playSample(f, 100);
						System.err.println("Play sample return code is: " + returnCode);
						Thread.sleep(delay);
					}
					catch (InterruptedException ie) {
						System.out.println("Exception in main thread: "+ie.getMessage());
					}

				}
			}
		}

	}
