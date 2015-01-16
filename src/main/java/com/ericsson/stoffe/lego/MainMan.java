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
	private static DLights lights = null;
	private static int lightN = 1;
	private static int[] lightRGB = { 255, 255, 255 };
	private static EV3MediumRegulatedMotor ma,mc,md;
    private static EV3LargeRegulatedMotor mb;

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

		for (String s: args) {
			System.out.println("Args = " + s);
		}
		//System.out.println("Args = " + args[1]);

        Port p1 = LocalEV3.get().getPort("S1");
		System.out.println("Port =" + p1.getPortType() + " sensor ="
				+ p1.getSensorType());
		MainMan.connectToMQTT();
		// myPort.setTypeAndMode(arg0, arg1)

        try {
			I2CPort myPort = p1.open(I2CPort.class);
			System.out.println("Mode =" + myPort.getMode() + " type="
					+ myPort.getType());
			myPort.setMode(I2CPort.HIGH_SPEED);
			lights = new DLights(myPort);
		} catch (Throwable t) {
			System.err.println("Failed with Lights ... "
					+ t.getLocalizedMessage());
		}
		// lights = new DLights(port1);

		System.out.println("Laser = " + lights);

		// Delay.msDelay(5000);
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

		mqtt = new MqttClient("tcp://mafalda.hack.att.io:11883", "LegoClient",
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
					sample = "lock.wav";
				}
                else if ("door_unlock".equals(cmd))
                    sample = "unlock.wav";

                else if ("engine_off".equals(cmd)) {
					engineRunning = false;
					sample = "engine_off.wav";

				} else if ("honk_blink".equals(cmd)) {
                    blink = true;
					sample = "honk.wav";
				}
				else if ("honk".equals(cmd)) {
					sample = "honk.wav";
				}
				else if ("blink".equals(cmd)) {
					blink = true;
				}
                else if ("alarm_on".equals(cmd)) {
                     sample = "alarm.wav";
                }
                else if ("alarm_off".equals(cmd)) {
                    // sample = "honk.wav";
                }

                else if ("openTrunk".equals(cmd)) {
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
                }
                else if ("closeTrunk".equals(cmd)) {
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
                }
                else if ("engine_on".equals(cmd)) {
                    engineRunning = true;

                    try {
                        // Propulsion
                        ma = new EV3MediumRegulatedMotor(MotorPort.A);
                        // Steering
                        mb = new EV3LargeRegulatedMotor(MotorPort.B);

                        // Pre-programmed route
                        ma.forward();
                        Delay.msDelay(5000);
                        mb.rotate(45);

                        Delay.msDelay(2000);
                        mb.rotate(-90);

                        Delay.msDelay(3000);
                        mb.rotate(45);

                        Delay.msDelay(4000);
                        ma.stop();
                        mb.stop();
                        ma.close();
                        mb.close();
                    } catch (Throwable t) {
                        System.err.println("Failed with turning on engine - motor A and B... "
                                + t.getLocalizedMessage());
                        t.printStackTrace();
                    }

                    sample = "engine_on.wav";
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

}
