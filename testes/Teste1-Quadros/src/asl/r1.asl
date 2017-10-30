/* Initial beliefs */


/* Initial goal */

!check(slots). 

/* Plans */

+!check(slots) : not foundSomething(r1) 
	<- turnRight(r1); !check(slots).