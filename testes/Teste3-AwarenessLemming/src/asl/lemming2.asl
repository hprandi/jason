/* Initial beliefs */


/* Initial goal */

!waitForRedLight. 

/* Plans */

+!waitForRedLight: redLight
	<- 	stopWalking(lemming); 
	 	!waitForGreenLight.	 	
	
+!waitForGreenLight: greenLight
	<- startWalking(lemming); 
		!waitForRedLight.