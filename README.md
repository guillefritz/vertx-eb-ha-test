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


##### 