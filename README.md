# vertx-eb-ha-test
vertx verticles with session + HA poc


### Description

This project tries to handle distributed Verticles (V) using High Availability (HA) but keeping the V state.


##### Files


*   Application.java: java main class to be runned as java (see running section)
*   Server.java: vertx webserver (see webserver section)
*	VertConfig.java: spring boot's vertx beans configuration
*	ClickVerticle.java: Verticle that holds a click counter, for demo purposes
*	SpringVerticleFactory.java: Custom Verticle Factory for Verticle deployments as beans. prefix: "spring:"
*	webroot: static content, html and js files 
*	clusterVIEW.xml: hazelcast xml config



##### Running

For running this demo, just compile the project with `mvn package` then `java -jar target/*.jar` or vertx-eb-ha-test-0.0.1-SNAPSHOT.jar :D. Then go to `http://localhost:8080/`. Once you enter a ClickVerticle will be deployed tied to a session (random generated on client side).

	2016-04-15 16:02:18.161 [t]  INFO 13450 -> [ntloop-thread-3] (Application.java:67) Application: initClick 8654
	2016-04-15 16:02:18.319 [t]  INFO 13450 -> [ntloop-thread-3] (Application.java:128) Application: deploy ClickVerticle dd8d48a3-d72b-4553-9d7a-22ae754e4f4f
	2016-04-15 16:02:18.324 [t]  INFO 13450 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 1 dd8d48a3-d72b-4553-9d7a-22ae754e4f4f 
	2016-04-15 16:02:19.111 [t]  INFO 13450 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 2 dd8d48a3-d72b-4553-9d7a-22ae754e4f4f 
	2016-04-15 16:02:19.530 [t]  INFO 13450 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 3 dd8d48a3-d72b-4553-9d7a-22ae754e4f4f 


Click on "click" buttton and the counter will increase...

Recommended to run 2/3 more instances `java -jar target/*.jar --server.port=18080` and `java -jar target/*.jar --server.port=28080`

 
##### WebServer

The webserver has 3 important parts:

1.  EventBus: for bridge eventBus from browsers to backend and backwards
2.  Static content: for serve static content
3.	emigrate: if you get the `http://localhost:8080/emigrate` all the living Verticles will emigrate to others instances in the cluster


##### HA / Failover


### 18080
	2016-04-15 16:31:34.365 [t]  INFO 13941 -> [ntloop-thread-5] (Application.java:74) Application: unregister initConsumer
	2016-04-15 16:31:34.382 [t]  INFO 13941 -> [ntloop-thread-5] (Application.java:86) Application: apagando -> initclick 9826b789-89d5-478a-b3ed-4b7eb19692a9
	2016-04-15 16:31:34.498 [t]  INFO 13941 -> [ntloop-thread-2] (ClickVerticle.java:106) ClickVerticle: clicked 11 9826b789-89d5-478a-b3ed-4b7eb19692a9 
	2016-04-15 16:31:34.503 [t]  INFO 13941 -> [ntloop-thread-5] (Application.java:88) Application: apagando -> initclick -> emigrate 9826b789-89d5-478a-b3ed-4b7eb19692a9
	2016-04-15 16:31:34.504 [t]  INFO 13941 -> [ntloop-thread-2] (ClickVerticle.java:69) ClickVerticle: undeploy emigrate-v-9826b789-89d5-478a-b3ed-4b7eb19692a9
	2016-04-15 16:31:34.517 [t]  INFO 13941 -> [ntloop-thread-2] (ClickVerticle.java:196) ClickVerticle: finalizando ClickVerticle 9826b789-89d5-478a-b3ed-4b7eb19692a9
	2016-04-15 16:31:34.523 [t]  INFO 13941 -> [ntloop-thread-5] (Application.java:90) Application: apagando -> initclick -> emigrate -> ok 9826b789-89d5-478a-b3ed-4b7eb19692a9



### 8080
	2016-04-15 16:31:34.385 [t]  INFO 13989 -> [ntloop-thread-3] (Application.java:67) Application: initClick 23377
	2016-04-15 16:31:34.493 [t]  INFO 13989 -> [ntloop-thread-3] (Application.java:128) Application: deploy ClickVerticle d456d5ec-17ce-49ec-8b31-58916f0bdf25
	2016-04-15 16:31:34.504 [t]  INFO 13989 -> [ntloop-thread-7] (ClickVerticle.java:79) ClickVerticle: reinicializando ClickVerticle desde sesion d456d5ec-17ce-49ec-8b31-58916f0bdf25
	2016-04-15 16:31:39.396 [t]  INFO 13989 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 12 d456d5ec-17ce-49ec-8b31-58916f0bdf25 
	2016-04-15 16:31:39.677 [t]  INFO 13989 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 13 d456d5ec-17ce-49ec-8b31-58916f0bdf25 
	2016-04-15 16:31:39.908 [t]  INFO 13989 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 14 d456d5ec-17ce-49ec-8b31-58916f0bdf25 
	2016-04-15 16:31:40.117 [t]  INFO 13989 -> [ntloop-thread-7] (ClickVerticle.java:106) ClickVerticle: clicked 15 d456d5ec-17ce-49ec-8b31-58916f0bdf25 
