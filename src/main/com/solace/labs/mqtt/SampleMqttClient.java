/*
 * Copyright 2016 Ken.Barr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/


package com.solace.labs.mqtt;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class SampleMqttClient implements MqttCallbackExtended {

    MqttAsyncClient myClient;
	MqttConnectOptions connOpt;

	static SyncronizeObj sync;
	static String BROKER_URL1;
	static String BROKER_URL2;
	static final String M2MIO_DOMAIN = "testDomain";
	static final String M2MIO_STUFF = "things";
	static final String M2MIO_THING = "myDeviceID";
	static final String M2MIO_USERNAME = "myClientUsername";
	static final String M2MIO_PASSWORD_MD5 = "password";
	static final String M2MIO_CLIENTNAME_TOPIC = "$SYS/client/client-name";
	static final String M2MIO_REPLYTO_TOPIC = "$SYS/client/reply-to";

	// the following two flags control whether this example is a publisher, a subscriber or both
	static final Boolean subscriber = true;
	static final Boolean publisher = true;
	String clientName = "";
	String replyToTopic = "";
	

	public SampleMqttClient() {
		BROKER_URL1 = System.getenv("BROKER_URL1");
		BROKER_URL2 = System.getenv("BROKER_URL2");
		System.out.println("Broker list will be: " + BROKER_URL1 + " + " + BROKER_URL2);
		sync = new SyncronizeObj();
	}
	
	/**
	 * 
	 */
	public void addSubscriptions() {
		try {
			// topics on m2m.io are in the form <domain>/<stuff>/<thing>
			String myTopic = M2MIO_DOMAIN + "/" + M2MIO_STUFF + "/" + M2MIO_THING;
			myClient.subscribe(M2MIO_REPLYTO_TOPIC, 0);
			myClient.subscribe(M2MIO_CLIENTNAME_TOPIC, 0);
			myClient.subscribe(myTopic, 0);

		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * MAIN
	 * 
	 */
	public static void main(String[] args) {
		SampleMqttClient smc = new SampleMqttClient();
		smc.runClient();
	}
	
	/**
	 * 
	 * runClient
	 * The main functionality of this simple example.
	 * Create a MQTT client, connect to broker, pub/sub, disconnect.
	 * 
	 */
	public void runClient() {
		// setup MQTT Client
		String clientID = M2MIO_THING;
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		connOpt.setConnectionTimeout(60);
		connOpt.setAutomaticReconnect(true);
		connOpt.setUserName(M2MIO_USERNAME);
		connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());
		connOpt.setWill("mqtt/disconnect/ungracefull", ("MQTT Client: " + M2MIO_USERNAME + " ungracefull disconnect").getBytes(), 0, false);
		MemoryPersistence persistence = new MemoryPersistence();
		String[] brokerList = new String[2];
		brokerList[0] = BROKER_URL1;
		brokerList[1] = BROKER_URL2;
		connOpt.setServerURIs(brokerList);

		DisconnectedBufferOptions bufferOpts = new DisconnectedBufferOptions();
		bufferOpts.setBufferEnabled(true);
		bufferOpts.setBufferSize(100);            // 100 message buffer
		bufferOpts.setDeleteOldestMessages(true); // Purge oldest messages when buffer is full
		bufferOpts.setPersistBuffer(false);       // Do not buffer to disk
		
		
		// Connect to Broker
		try {
			myClient = new MqttAsyncClient(brokerList[0], clientID, persistence);
			myClient.setBufferOpts(bufferOpts);
			myClient.setCallback(this);
			System.out.println("Connection attempt! To: " + brokerList[0]);
            myClient.connect(connOpt);
            System.out.println("Blocking for known issue: #233");
            sync.doWait((long)connOpt.getConnectionTimeout() * 1000);
            System.out.println("Received initial connection signal, continuing");
        } catch (MqttException ex){
			// TODO Auto-generated catch block
            ex.printStackTrace();
		}


		String myTopic = M2MIO_DOMAIN + "/" + M2MIO_STUFF + "/" + M2MIO_THING;
        MqttMessage message = new MqttMessage("TEST MESSAGE".getBytes());
        message.setQos(0);
        try {
        	System.out.println("Publish1");
        	myClient.publish(myTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }


		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// disconnect
		try {
			myClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		try {
			System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (topic.equals(M2MIO_CLIENTNAME_TOPIC)) {
			clientName = new String(message.getPayload());
			System.out.println("ClientName set");
		}
		if (topic.equals(M2MIO_REPLYTO_TOPIC)) {
			replyToTopic = new String(message.getPayload());
			System.out.println("ReplyTo set");
		}
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| QoS: " + message.getQos());
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------");

	}


	public void connectionLost(Throwable t) {
		System.out.println("Connection lost!" + t.toString());
	}
	

	public void connectComplete(boolean reconnect, String serverURI) {
		if (reconnect) {
			System.out.println("Connection Reconnected! To: " + serverURI);
		} else {
			System.out.println("Initial Connection! To: " + serverURI);
			sync.doNotify();
		}
		addSubscriptions();
	}

	public class SyncronizeObj {
		public void doWait(long l){
		    synchronized(this){
		        try {
		            this.wait(l);
		        } catch(InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		        }
		    }
		}

		public void doNotify() {
		    synchronized(this) {
		        this.notify();
		    }
		}

		public void doWait() {
		    synchronized(this){
		        try {
		            this.wait();
		        } catch(InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		        }
		    }
		}
		}

}
